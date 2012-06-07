import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;

/**
 * Handles client interaction, cache, and state for a simple distributed file system.
 *
 * Maintains a cache of the latest file opened in the DFS.
 * Responds to user input and server calls that modify the state of the cache.
 */
public class DFSClient extends UnicastRemoteObject implements ClientInterface {

	private static final long serialVersionUID = 2960838778861780947L;
	// Path where the latest file is stored on disc to be opened with an editor.
    private static final String LOCAL_CACHE_PATH = "/tmp/dlm18.txt";
	private static final char READ = 'r';
	private static final char WRITE = 'w';

    /**
     * Represents possible states of the cache.
     */
	private enum CacheState {
		INVALID, READ_SHARED, WRITE_OWNED, MODIFIED_OWNED, RELEASE_OWNERSHIP;
	}
    
    // Name of file cached locally.
	private String name;
    // Current state of teh cache.
	private CacheState state = CacheState.INVALID;
    
    // The remote server for the DFS
	private ServerInterface fileServer;
    // A reference to the cached file.
	private File tempFile;
    
    // Name of the current, local client. This should be the hostname the
    // sever can use to contact the client.
	private String clientName;
    
    /**
     * Constructs a client to interact with a given server.
     *
     * The server to get files from and send files to.
     */
	public DFSClient(ServerInterface fileServer) throws IOException {
		this.fileServer = fileServer;
		tempFile = new File(LOCAL_CACHE_PATH);
		if (!tempFile.exists()) {
			tempFile.createNewFile();
			tempFile.setReadable(true);
		}

		InetAddress addr = InetAddress.getLocalHost();
		clientName = addr.getHostName();
	}
    
    /**
     * Open a given file in the specified mode.
     *
     * May get the file from the sever depending on the current state of the cache.
     * May block until the dfs file server can send the file.
     *
     * @param fileName - the name of the file to open.
     * @param mode - the access mode of the file.
     */
	public synchronized void openFile(String fileName, char mode) throws IOException {
		if (!fileName.equals(name)) {
			if (state == CacheState.WRITE_OWNED || state == CacheState.MODIFIED_OWNED)
				uploadFile();
			state = CacheState.INVALID;
		}
        
        // Transistion function for state machine.
		switch (state) {
		case INVALID:
			downloadFile(fileName, mode);
			state = (mode == READ) ? CacheState.READ_SHARED : CacheState.WRITE_OWNED;
			break;
		case READ_SHARED:
			if (mode == WRITE) {
				downloadFile(fileName, mode);
				state = CacheState.WRITE_OWNED;
			}
			break;
		case WRITE_OWNED:
			// DO NOTHING
			break;
		case MODIFIED_OWNED:
			state = CacheState.WRITE_OWNED;
			break;
		default:
			throw new IllegalStateException();
		}
	}
    
    /**
     * Handle an invalidate signal from the server.
     *
     * Set cache, which has file for read, to invalid state.
     *
     * @return whether the cache can be invalidated.
     */
	public synchronized boolean invalidate() throws RemoteException {
		if (state == CacheState.READ_SHARED) {
			state = CacheState.INVALID;
			return true;
		}
		return false;
	}
    
    /**
     * Handle a writeback signal from the server.
     *
     * Sets file to be written back either immediately or when the current editing session closes.
     */
	public synchronized boolean writeback() throws RemoteException {
		if (state == CacheState.WRITE_OWNED) {
			state = CacheState.RELEASE_OWNERSHIP;
			return true;
		} else if (state == CacheState.MODIFIED_OWNED) {
			final FileContents contents;
			try {
			contents = getCurrentContents();
			} catch (IOException e) {
				throw new RemoteException("Could not read contents of local file cache.", e);
			}
            // Immediately write back file.
            // Handle on separate thread to avoid deadlock
			(new Thread() {

				public void run() {
					try {
						System.out.println("Trying to upload current changes.");
						if (fileServer.upload(clientName, name, contents)) {
							System.out.println("Successful upload!");
							state = CacheState.READ_SHARED;
							// return true;
						}
					} catch (IOException e) {
						e.printStackTrace();
//						throw new RemoteException("Could not read contents of local file cache.", e);
					}
				}
			}).start();
			return true;
		}
		return false;
	}
    
    /**
     * Ends current editing session.
     *
     * May end current file session if write ownership is being transferred.
     */
	public synchronized void completeSession() throws IOException {
		if (state == CacheState.RELEASE_OWNERSHIP) {
			uploadFile();
			state = CacheState.READ_SHARED;
		} else if (state == CacheState.WRITE_OWNED) {
			state = CacheState.MODIFIED_OWNED;
		}
	}

    /**
     * Helper method for downloading file from remote dfs fileserver.
     */
	private void downloadFile(String fileName, char mode) throws IOException {
		FileContents contents = fileServer.download(clientName, fileName, Character.toString(mode));
		name = fileName;
		tempFile.setWritable(true);

		FileOutputStream tempFileWriter = new FileOutputStream(tempFile);
		tempFileWriter.write(contents.get());
		tempFileWriter.close();

		tempFile.setWritable(mode == WRITE);
	}
    
    /**
     * Helper method for uploading file to remote dfs file server.
     */
	private boolean uploadFile() throws IOException {
		FileContents contents = getCurrentContents();
		return fileServer.upload(clientName, name, contents);

	}

	/**
     * Helper method for getting current contents of cached file.
     * Used to get contents to upload.
	 */
	private FileContents getCurrentContents() throws FileNotFoundException, IOException {
		byte[] data = new byte[(int) tempFile.length()];
		FileInputStream fileReader = new FileInputStream(tempFile);
		fileReader.read(data);
		FileContents contents = new FileContents(data);
		return contents;
	}
    
    /**
     * Run desired editing program.
     *
     * @param desiredEditor - the editor to use. Should be either vim, gvim, or emacs.
     */
	public void launchEditor(String desiredEditor) {
		String[] command = null;
		if (desiredEditor.equals("vim")) {
			command = new String[] { "sh", "-c", "vim " + LOCAL_CACHE_PATH + " </dev/tty >/dev/tty" };
		} else if (desiredEditor.equals("gvim")) {
			command = new String[] { "gvim", "-f", LOCAL_CACHE_PATH };
		} else if (desiredEditor.equals("emacs")) {
            command = new String[] { "emacs", LOCAL_CACHE_PATH };
		}
		try {
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
		} catch (IOException e) {
			System.err.println("Could not open local cache of file with " + desiredEditor);
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
	public static void main(String[] args) {
		ServerInterface fileServer;
		try {
			System.out.println("Connecting to server ...");
			// fileServer = new MockServer();
			String dfsAddress = String.format("rmi://%s:%s/dfsserver", args[0], args[1]);
			fileServer = (ServerInterface) Naming.lookup(dfsAddress);

			System.out.println("Starting client ...");
			DFSClient client = new DFSClient(fileServer);
			Naming.rebind("rmi://localhost:" + args[1] + "/fileclient", client);

			Prompter input = new Prompter();
			// Loop until user is done.
            while (true) {
				if (input.ask("Do you want to exit?")) {
					System.out.println("Writing any changes ...");
					client.writeback();
					client.completeSession();
					System.out.println("DONE");
					System.exit(0);
				}
                // Get input.
				System.out.println("FileClient: Next file to open");
				String fileName = input.prompt("Filename");
				char mode = input.prompt("How(r/w)").charAt(0);
				String editor = input.promptChoices("Editor",
						new String[] { "vim", "gvim", "emacs" });
                
                // Open file for reading or writing.
				client.openFile(fileName, mode);
				client.launchEditor(editor);
				client.completeSession();
			}
        // Should log exceptions or handle more gracefully
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
