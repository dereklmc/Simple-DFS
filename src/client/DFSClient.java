package client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;

import lib.AccessMode;
import lib.ClientInterface;
import lib.Prompter;
import lib.ServerInterface;

public class DFSClient extends UnicastRemoteObject implements ClientInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3680069569724752340L;
	private FileCache cache;

	public DFSClient(ServerInterface fileServer) throws IOException {
		cache = new FileCache(fileServer);
	}

	public boolean invalidate() throws RemoteException {
		return cache.invalidateFile();
	}

	public boolean writeback() throws RemoteException {
		return cache.writeBack();
	}

	public void open(String fileName, AccessMode mode) throws RemoteException {
		cache.openFile(fileName, mode);
	}
	
	public void completeSession() throws RemoteException {
		cache.completeSession();
	}
	
	public void launchEditor(String desiredEditor) {
		String cachePath = cache.getCachePath();
		String[] command = null;
		if (desiredEditor.equals("vim")) {
			command = new String[] { "sh", "-c", "vim " + cachePath + " </dev/tty >/dev/tty" };
		} else if (desiredEditor.equals("gvim")) {
			command = new String[] { "gvim", "-f", cachePath };
		} else if (desiredEditor.equals("emacs")) {
			
		}
		try {
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
		} catch (IOException e) {
			System.err.println("Could not open local cache of file with emacs");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ServerInterface fileServer;
		try {
			//fileServer = new MockServer(); // (ServerInterface)
										// Naming.lookup("");
			String dfsAddress = String.format("rmi://%s:%s/dfsserver", args[0], args[1]);
			fileServer = (ServerInterface) Naming.lookup(dfsAddress);
			DFSClient client = new DFSClient(fileServer);
			Naming.rebind("rmi://localhost:" + args[1] + "/fileclient", client);
			Prompter input = new Prompter();
			while (true) {
				if (input.ask("Do you want to exit?")) {
					client.writeback();
					client.completeSession();
					System.exit(0);
				}
				System.out.println("FileClient: Next file to open");
				String fileName = input.prompt("Filename:");
				String modeInput = input.prompt("How(r/w):");
				AccessMode mode = AccessMode.getMode(modeInput);
				String editor = input.promptChoices("Editor", new String[] {"vim", "gvim", "emacs" });
				client.open(fileName, mode);
				client.launchEditor(editor);
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
