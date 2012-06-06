

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.LinkedList;



public class CachedFile {

	private static final String ROOT_DIR = "/tmp/";

	private LinkedList<ClientProxy> readers;
	private ClientProxy owner;
	private File storedFile;
	private byte[] data;

	public CachedFile(String fileName) throws IOException {
		owner = null;
		readers = new LinkedList<ClientProxy>();

		storedFile = new File(ROOT_DIR + fileName);
		data = new byte[(int) storedFile.length()];
		FileInputStream fileReader = new FileInputStream(storedFile);
		fileReader.read(data);
	}

	public String getName() {
		return storedFile.getName();
	}

	public synchronized void registerReader(ClientProxy client) {
		readers.add(client);
	}

	public synchronized void registerWriter(ClientProxy client) throws RemoteException {
		// Remove client from reader list, if present in reader list.
		readers.remove(client);
		if (!client.equals(owner)) {
			while (owner != null) {
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
		}
		owner = client;
	}

	public FileContents getContents() {
		return new FileContents(data);
	}

	public synchronized boolean updateContents(String clientName, FileContents contents)
			throws RemoteException {
		if (owner == null && owner.getClientName().equals(clientName)) {
			return false;
		}
		while (!readers.isEmpty()) {
			ClientProxy reader = readers.remove();
			System.out.println("Invalidating reader <" + reader + ">");
			reader.invalidate();
		}
		owner = null;
		data = contents.get();
		(new AsyncFileWriter(storedFile, data)).start();
		notifyAll();
		return true;
	}
}
