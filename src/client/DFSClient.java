package client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import lib.AccessMode;
import lib.ClientInterface;
import lib.Prompter;
import lib.ServerInterface;

public class DFSClient implements ClientInterface {

	private FileCache cache;

	public DFSClient(ServerInterface fileServer) throws IOException {
		cache = new FileCache(fileServer, "/temp/useraccount.txt");
	}

	public boolean invalidate() throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean writeback() throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public void open(String fileName, AccessMode mode) throws RemoteException {
		cache.openFile(fileName, mode);
	}
	
	/**
	 * @param fileName
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void editCurrentFile() {
		cache.launchEditor();
		try {
			cache.completeSession();
		} catch (RemoteException e) {
			System.err.println("Could not upload saved results to DFS Server");
		}
	}

	public static void main(String[] args) {
		int port = Integer.parseInt(args[0]);
		System.out.println("Port: " + port);

		ServerInterface fileServer;
		try {
			fileServer = new MockServer(); // (ServerInterface)
											// Naming.lookup("");
			DFSClient client = new DFSClient(fileServer);
			Prompter input = new Prompter();
			while (true) {
				if (input.ask("Do you want to exit?"))
					System.exit(0);
				System.out.println("FileClient: Next file to open");
				String fileName = input.prompt("Filename:");
				String modeInput = input.prompt("How(r/w):");
				AccessMode mode = AccessMode.getMode(modeInput);
				client.open(fileName, mode);

				System.out.println("Chosen filename: " + "[" + mode + "] "
						+ fileName);
				client.open(fileName, mode);
				client.editCurrentFile();
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