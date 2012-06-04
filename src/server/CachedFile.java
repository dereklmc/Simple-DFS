package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lib.AccessMode;
import lib.FileContents;

public class CachedFile {

	public enum CacheState {
		READ_SHARED, WRITE_SHARED, OWNERSHIP_CHANGED, NOT_SHARED;
	}

	private String fileName;
	private CacheState state;
	private List<ClientProxy> readers;
	private ClientProxy owner;
	private File storedFile;
	private byte[] data;

	public CachedFile(String fileName) throws IOException {
		this.fileName = fileName;
		state = CacheState.NOT_SHARED;
		readers = new ArrayList<ClientProxy>();
		owner = null;

		storedFile = new File(fileName);
		data = new byte[(int) storedFile.length()];
		FileInputStream fileReader = new FileInputStream(storedFile);
		fileReader.read(data);

	}

	public String getName() {
		return fileName;
	}

	public synchronized void registerClient(ClientProxy client, AccessMode mode) throws RemoteException {
		if (mode == AccessMode.READ) {
			readers.add(client);
			if (state == CacheState.NOT_SHARED)
				state = CacheState.READ_SHARED;
		} else {
			if (state == CacheState.NOT_SHARED || state == CacheState.READ_SHARED) {
				owner = client;
				state = CacheState.WRITE_SHARED;
			} else if (!client.equals(owner)) {
				// Remove client from reader list, if present in reader list.
				readers.remove(client);
				while (state == CacheState.WRITE_SHARED) {
					try {
						owner.writeback();
						wait();
					} catch (InterruptedException e) {
						// TODO log exception
						continue;
					} catch (RemoteException e) {
						throw new RemoteException("Writeback request to current owner failed!", e);
					}
				}
				owner = client;
				state = CacheState.WRITE_SHARED;
			}
		}
	}

	public synchronized boolean updateContents(FileContents contents) throws RemoteException {
		if (state == CacheState.NOT_SHARED || state == CacheState.READ_SHARED) {
			return false;
		}
		for (Iterator<ClientProxy> iterator = readers.iterator(); iterator.hasNext();) {
			ClientProxy reader = iterator.next();
			System.out.println("Invalidating reader <" + reader + ">");
			try {
				reader.invalidate();
			} catch (RemoteException e) {
				throw new RemoteException("Invalidate request to current reader " + reader.getClientName() + " failed!", e);
			}
			iterator.remove();
		}
		data = contents.get();
		state = CacheState.NOT_SHARED;
		notifyAll();
		return true;
	}

	public FileContents getContents() {
		return new FileContents(data);
	}

	public boolean isOwnedBy(String clientName) {
		if (owner == null)
			return false;
		return owner.getClientName().equals(clientName);
	}

}
