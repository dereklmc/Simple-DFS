

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;


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
	
	public synchronized void removeReader(String clientName) {
		Iterator<ClientProxy> it = readers.iterator();
		while (it.hasNext()) {
			ClientProxy reader = it.next();
			if (reader.getName().equals(clientName))
				it.remove();
		}
	}

	public synchronized void registerWriter(ClientProxy client) throws RemoteException {
		// Remove client from reader list, if present in reader list.
		readers.remove(client);
		if (!client.equals(owner)) {
			while (owner != null) {
				try {
					System.out.println("RegisterWrite for <" + client.getName() + "> Waiting for writeback from \"" + owner.getName() + "\"");
					owner.writeback();
					wait();
					System.out.println("Writeback complete. Continue down for <" + client.getName() + ">");
				} catch (InterruptedException e) {
					System.err.println("Interrupt while waiting for writeback from <" + owner.getName() + ">");
					continue;
				} catch (RemoteException e) {
					throw new RemoteException("Writeback request to current owner failed!", e);
				}
			}
		} else {
			System.out.println("Client <" + client.getName() + "> already owns file <" + storedFile.getName() + "> for write.");
		}
		owner = client;
	}

	public FileContents getContents() {
		return new FileContents(data);
	}

	public boolean updateContents(String clientName, FileContents contents)
			throws RemoteException {
		System.out.println("Update from <" + clientName + "> for file <" + storedFile.getName() + ">. Current owner is <" + (owner == null ? "-" : owner.getName()) + ">");
		if (owner == null && owner.getName().equals(clientName)) {
			return false;
		}
		System.out.println("Invalidating readers for file previously owned by <" + clientName + ">");
		while (!readers.isEmpty()) {
			ClientProxy reader = readers.remove();
			System.out.println("Invalidating reader <" + reader + "> for file previously owned by <" + clientName + ">");
			reader.invalidate();
		}
		owner = null;
		data = contents.get();
		(new AsyncFileWriter(storedFile, data)).start();
		synchronized (this) {
			notifyAll();
		}
		System.out.println("Finished update contents from <" + clientName + ">");
		return true;
	}

	public List<ClientProxy> getReaders() {
		return Collections.unmodifiableList(readers);
	}

	public String getOwnerName() {
		if (owner == null) {
			return "-";
		}
		return owner.getName();
	}
}
