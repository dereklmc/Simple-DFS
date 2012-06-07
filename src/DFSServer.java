import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Handles server operations for a simple distributed files system
 *
 * Maintains a cache of every file opened by a client.
 */
public class DFSServer extends UnicastRemoteObject implements ServerInterface {

	private static final long serialVersionUID = 1539908244124054096L;
    // A files currently cached.
	private List<CachedFile> fileCache;
    // Port the DFS nodes should contact each other over.
	private String port;
    
    /**
     * Creates a server.
     *
     * The port the server runs on.
     */
	protected DFSServer(String port) throws RemoteException {
		fileCache = new ArrayList<CachedFile>();
		this.port = port;
	}
    
    /**
     * Handles a client request to download agiven file in a given accessmode.
     */
	@Override
	public FileContents download(String clientName, String filename, String mode)
			throws RemoteException {
        // Remove client from read perms of other cached files.
        // This is so the client doesn't recieve an invalidate request for a file
        // it is not currently reading, but was several sessions ago.
		for (CachedFile file : fileCache) {
			if (!file.getName().equals(filename))
				file.removeReader(clientName);
		}

		CachedFile file = getCachedFile(filename);
		System.out.println(String.format("Downloading file \"%s\" in mode [%s] to client \"%s\"",
				filename, mode, clientName));
        // If file is not cached, cache it
		if (file == null) {
			System.out.println("\tFile not cached. Caching file. ");
			try {
				file = new CachedFile(filename);
			} catch (IOException e) {
				throw new RemoteException("Could not open requested file!", e);
			}
			fileCache.add(file);
		}
        // Regiser client for access to this file.
		try {
			ClientProxy client = new ClientProxy(clientName, port);
			if (mode.equals("r")) {
				System.out.println("Registering Reader");
				file.registerReader(client);
			} else {
				System.out.println("Registering Writer");
				file.registerWriter(client);
			}
			return file.getContents();
		} catch (IllegalArgumentException e) {
			throw new RemoteException("Bad request!", e);
		} catch (MalformedURLException e) {
			throw new RemoteException(
					"Error creating connection to client requesting file download.", e);
		} catch (NotBoundException e) {
			throw new RemoteException(
					"Error creating connection to client requesting file download.", e);
		}
	}
    
    /**
     * Handles client request to upload a given file.
     *
     * Kind of a proxy to the cached file's method for handling the upload.
     */
	@Override
	public boolean upload(String clientName, String filename, FileContents contents)
			throws RemoteException {
		CachedFile file = getCachedFile(filename);
		System.out.println(String.format("Uploading file \"%s\" from \"%s\".", filename, clientName));
		return file.updateContents(clientName, contents);
	}
    
    /**
     * Helper method to search for a cached file with a given filename.
     */
	private CachedFile getCachedFile(String fileName) {
		for (CachedFile file : fileCache) {
			if (file.getName().equals(fileName))
				return file;
		}
		return null;
	}
    
    /**
     * For list command: display info about cached files and associated registered clients.
     */
	public void displayInfo() {
		for (CachedFile file : fileCache) {
			System.out.println("=== " + file.getName() + " ===");
			System.out.println("$ Owned by \"" + file.getOwnerName() + "\"");
			// Display reader info.
            for (ClientProxy reader : file.getReaders()) {
				System.out.println("# " + reader.getName());
			}
			System.out.println("=== *** ===");
		}
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: java server.DFSServer port#");
			System.exit(-1);
		}
		try {
			System.out.println("Starting server...");
			DFSServer dfs = new DFSServer(args[0]);
			Naming.rebind("rmi://localhost:" + args[0] + "/dfsserver", dfs);
			Scanner input = new Scanner(System.in);
			System.out.println("Server started.");

            // Allow for client input of commands to exit or inspect state of server.
			while (true) {
				String nextAction = input.nextLine();
				if (nextAction.equals("exit")) {
					// TODO cleanup
					System.exit(0);
				} else if (nextAction.equals("list")) {
					dfs.printReaders();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
