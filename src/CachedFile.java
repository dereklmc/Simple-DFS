
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a file currently cached by the server.
 *
 * Cached files correspond to files in the directory /tmp
 *
 * Cached file acts as a state machine with three states:
 * not_shared, read_shared, write_shared
 * 
 * The cached file knows each client current reading the file
 * and the client that currently owns the file for write.
 */
public class CachedFile {
    
    // Root directory for the dfs' file system.
	private static final String ROOT_DIR = "/tmp/";
    
    // All clients currently reading the file.
	private LinkedList<ClientProxy> readers;
    // The client that owns the current file.
	private ClientProxy owner;

    // A reference to the file stored on disk
	private File storedFile;
    // An in-memory cache the file's current contents.
	private byte[] data;
    
    /**
     * Contructs CachedFile for a file stored on the server.
     *
     * Reads data from stored file into this cache. Initializes state to "not shared"
     *
     * @param fileName - the file to cache
     */
	public CachedFile(String fileName) throws IOException {
		owner = null;
		readers = new LinkedList<ClientProxy>();

		storedFile = new File(ROOT_DIR + fileName);
		data = new byte[(int) storedFile.length()];
		FileInputStream fileReader = new FileInputStream(storedFile);
		fileReader.read(data);
	}

    /**
     * Return the name of the current file being cached
     * 
     * @return name of the cached file
     */
	public String getName() {
		return storedFile.getName();
	}
    
    /**
     * Registers a client as a reader for this file, granting them read permissions.
     */
	public synchronized void registerReader(ClientProxy client) {
		readers.add(client);
	}
    
    /**
     * Removes client from cached file's list of readers.
     */
	public synchronized void removeReader(String clientName) {
		Iterator<ClientProxy> it = readers.iterator();
		// Remove readers matching client name.
        while (it.hasNext()) {
			ClientProxy reader = it.next();
			if (reader.getName().equals(clientName))
				it.remove();
		}
	}
    
    /**
     * Registers a client for write access to the current cached file.
     *
     * If cached file is currently owned for write by a different clien than specified,
     * this will initiate ownership transger to the given client. In this case, this
     * method will block until ownership is transfered.
     *
     * @param client - the client to register for write ownership
     *
     * @throws RemoteException if an error occurs with ownership transfer
     */
	public void registerWriter(ClientProxy client) throws RemoteException {
		// Remove client from reader list, if present in reader list.
		readers.remove(client);
		if (!client.equals(owner)) {
			synchronized (this) {
                // Wait until the previous owner has released ownership.
				while (owner != null) {
					try {
						System.out.println("RegisterWrite for <" + client.getName()
								+ "> Waiting for writeback from \"" + owner.getName() + "\"");
						owner.writeback();
						wait();
						System.out.println("Writeback complete. Continue down for <"
								+ client.getName() + ">");
					} catch (InterruptedException e) {
						System.err.println("Interrupt while waiting for writeback from <"
								+ owner.getName() + ">");
						continue;
					} catch (RemoteException e) {
						throw new RemoteException("Writeback request to current owner failed!", e);
					}
				}
			}
		} else {
			System.out.println("Client <" + client.getName() + "> already owns file <"
					+ storedFile.getName() + "> for write.");
		}
		owner = client;
	}
    
    /**
     * Get the contents of the cached file.
     *
     * For returning contents to a client.
     */
	public FileContents getContents() {
		return new FileContents(data);
	}
    
    /**
     * Update cached file with contents from client.
     *
     * Client releases write ownership of file. All readers are invalidated.
     * Ownership is now ready to be transfered to the next client.
     *
     * @param clientName - the name of the client uploading the contents.
     * @param contents - the new contents of the cached file
     * 
     * @return false if the client is not allowed to update the contents, true otherwise.
     *
     * @throws RemoteException if and error occurs during invalidation of readers.
     */
	public boolean updateContents(String clientName, FileContents contents) throws RemoteException {
		System.out.println("Update from <" + clientName + "> for file <" + storedFile.getName()
				+ ">. Current owner is <" + (owner == null ? "-" : owner.getName()) + ">");
		if (owner == null || owner.getName().equals(clientName)) {
			return false;
		}
        // Invalidate readers.
		System.out
				.println("Invalidating readers for file previously owned by <" + clientName + ">");
		while (!readers.isEmpty()) {
			ClientProxy reader = readers.remove();
			System.out.println("Invalidating reader <" + reader
					+ "> for file previously owned by <" + clientName + ">");
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
    
    /**
     * Get a list of readers of this file. Used for debugging.
     */
	public List<ClientProxy> getReaders() {
		return Collections.unmodifiableList(readers);
	}
    
    /**
     * Get the name of the client that owns this file for write.
     * 
     * @return owner name or, if no owner, the string "-"
     */
	public String getOwnerName() {
		if (owner == null) {
			return "-";
		}
		return owner.getName();
	}
}
