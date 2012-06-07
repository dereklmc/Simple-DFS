
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;

public class DFSClient extends UnicastRemoteObject implements ClientInterface {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2960838778861780947L;
	private static final String LOCAL_CACHE_PATH = "/tmp/dlm18.txt";
	private static final char READ = 'r';
	private static final char WRITE = 'w';

	private enum CacheState {
		INVALID, READ_SHARED, WRITE_OWNED, MODIFIED_OWNED, RELEASE_OWNERSHIP;
	}

	private String name;
	private CacheState state = CacheState.INVALID;

	private ServerInterface fileServer;
	private File tempFile;

	private String clientName;
	
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

	public synchronized void openFile(String fileName, char mode) throws IOException {
		if (!fileName.equals(name)) {
			if (state == CacheState.WRITE_OWNED || state == CacheState.MODIFIED_OWNED)
				uploadFile();
			state = CacheState.INVALID;
		}
		System.out.println("Opening File");
		System.out.println("\tCurrent State: " + state);
		System.out.println("\tMode: " + state);
		
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
		System.out.println("\tNext State: " + state.name());
	}

	public synchronized boolean invalidate() throws RemoteException {
		if (state == CacheState.READ_SHARED) {
			state = CacheState.INVALID;
			return true;
		}
		return false;
	}

	public synchronized boolean writeback() throws RemoteException {
		System.out.println("Recieved Writeback! Current state is [" + state.name() + "]");
		if (state == CacheState.WRITE_OWNED) {
			state = CacheState.RELEASE_OWNERSHIP;
			return true;
		} else if (state == CacheState.MODIFIED_OWNED) {
			try {
				System.out.println("Trying to upload current changes.");
				if (uploadFile()) {
					System.out.println("Successful upload!");
					state = CacheState.READ_SHARED;
					return true;
				}
			} catch (IOException e) {
				throw new RemoteException("Could not read contents of local file cache.", e);
			}
		}
		return false;
	}

	public synchronized void completeSession() throws IOException {
		if (state == CacheState.RELEASE_OWNERSHIP) {
			uploadFile();
			state = CacheState.READ_SHARED;
		} else if (state == CacheState.WRITE_OWNED) {
			state = CacheState.MODIFIED_OWNED;
		}
	}
	
	private void downloadFile(String fileName, char mode) throws IOException {
		FileContents contents = fileServer.download(clientName, fileName, Character.toString(mode));
		name = fileName;
		tempFile.setWritable(true);
		
		FileOutputStream tempFileWriter = new FileOutputStream(tempFile);
		tempFileWriter.write(contents.get());
		tempFileWriter.close();
		
		tempFile.setWritable(mode == WRITE);
	}
	
	private boolean uploadFile() throws IOException {
		byte[] data = new byte[(int) tempFile.length()];
		FileInputStream fileReader = new FileInputStream(tempFile);
		fileReader.read(data);
		FileContents contents = new FileContents(data);
		return fileServer.upload(clientName, name, contents);
		
	}
	
	public void launchEditor(String desiredEditor) {
		String[] command = null;
		if (desiredEditor.equals("vim")) {
			command = new String[] { "sh", "-c", "vim " + LOCAL_CACHE_PATH + " </dev/tty >/dev/tty" };
		} else if (desiredEditor.equals("gvim")) {
			command = new String[] { "gvim", "-f", LOCAL_CACHE_PATH };
		} else if (desiredEditor.equals("emacs")) {
			
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
			//fileServer = new MockServer();
			String dfsAddress = String.format("rmi://%s:%s/dfsserver", args[0], args[1]);
			fileServer = (ServerInterface) Naming.lookup(dfsAddress);
			
			System.out.println("Starting client ...");
			DFSClient client = new DFSClient(fileServer);
			Naming.rebind("rmi://localhost:" + args[1] + "/fileclient", client);
			
			Prompter input = new Prompter();
			while (true) {
				if (input.ask("Do you want to exit?")) {
					System.out.println("Writing any changes ...");
					client.writeback();
					client.completeSession();
					System.out.println("DONE");
					System.exit(0);
				}
				System.out.println("FileClient: Next file to open");
				String fileName = input.prompt("Filename");
				char mode = input.prompt("How(r/w)").charAt(0);
				String editor = input.promptChoices("Editor", new String[] {"vim", "gvim", "emacs" });
				
				client.openFile(fileName, mode);
				client.launchEditor(editor);
				client.completeSession();
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
}
