package play;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import server.DFSServer;

import lib.FileContents;
import lib.ServerInterface;

public class ServerTest extends UnicastRemoteObject implements ServerInterface {

	public ServerTest() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public FileContents download(String client, String filename, String mode)
			throws RemoteException {
		System.out.println(String.format(
				"Recieved Download request from %s for file \"%s\".", client,
				filename));
		return null;
	}

	@Override
	public boolean upload(String client, String filename, FileContents contents)
			throws RemoteException {
		System.out.println(String.format(
				"Recieved Upload request from %s for file \"%s\".", client,
				filename));
		return false;
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: java server.DFSServer port#");
			System.exit(-1);
		}
		try {
			ServerTest dfs = new ServerTest();
			Naming.rebind("rmi://localhost:" + args[0] + "/dfsserver", dfs);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
