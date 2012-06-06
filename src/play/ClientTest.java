package play;

import ClientInterface;
import Prompter;
import ServerInterface;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientTest extends UnicastRemoteObject implements ClientInterface {
	
	private static final long serialVersionUID = 1L;
	private ServerInterface server;
	private boolean recievedWriteback = false;
	private String localAddress;

	public ClientTest(ServerInterface server, String localAddress) throws RemoteException {
		super();
		this.server = server;
		this.localAddress = localAddress;
	}
	
	@Override
	public boolean invalidate() throws RemoteException {
		System.out.println("\nRecieved Invalidate request!");
		return false;
	}

	@Override
	public boolean writeback() throws RemoteException {
		System.out.println("\nRecieved Writeback request!");
		recievedWriteback = true;
		return false;
	}
	
	public boolean uploadChanges() throws RemoteException {
		if (recievedWriteback) {
			boolean result = uploadCurrent();
			recievedWriteback = false;
			return result;
		}
		return false;
	}

	public boolean uploadCurrent() throws RemoteException {
		System.out.println("Attempting to upload file...");
		return server.upload(localAddress, "testfile.txt", null);
	}

	public static void main(String[] args) throws IOException, NotBoundException {
		Prompter input = new Prompter();
		
		InetAddress addr = InetAddress.getLocalHost();
		String localAddress = addr.getHostName();
		
		String address = String.format("rmi://%s:%s/%s", args[0], args[1], "dfsserver");
		ServerInterface server = (ServerInterface) Naming.lookup(address);
		
		ClientTest test = new ClientTest(server, localAddress);
		Naming.rebind("rmi://localhost:" + args[1] + "/fileclient", test);
		
		while (true) {
			test.uploadChanges();
			if (input.ask("Do you want to exit?")) {
				test.uploadCurrent();
				System.exit(0);
			}
			test.uploadChanges();
			int accessChoice = input.promptChoice("Open file in mode", new String[] {"read", "write"});
			server.download(localAddress, "testfile.txt", accessChoice == 1 ? "r" : "w");
			System.out.println("Downloaded file!");
			test.uploadChanges();
		}
	}

}
