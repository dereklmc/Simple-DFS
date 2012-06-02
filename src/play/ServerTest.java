package play;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import server.CachedFile.CacheState;

import lib.FileContents;
import lib.ServerInterface;

public class ServerTest extends UnicastRemoteObject implements ServerInterface {
	
	public enum CacheState {
		READ_SHARED, WRITE_SHARED, OWNERSHIP_CHANGED, NOT_SHARED;
	}
	
	private CacheState state;
	
	public ServerTest() throws RemoteException {
		super();
		state = CacheState.NOT_SHARED;
		printState();
	}

	private void printState() {
		System.out.println(String.format("Current State: <%s>", state.name()));
	}

	@Override
	public FileContents download(String client, String filename, String mode)
			throws RemoteException {
		System.out.println(String.format(
				"Recieved Download request from %s for file \"%s\" in mode <%s>.", client,
				filename, mode));
		printState();
		System.out.println("Changing state...");
		switch (state) {
		case NOT_SHARED:
			if (mode.equals("r")) {
				state = CacheState.READ_SHARED;
			} else {
				state = CacheState.WRITE_SHARED;
			}
			break;
		case READ_SHARED:
			if (mode.equals("r")) {
			} else {
				state = CacheState.WRITE_SHARED;
			}
			break;
		case WRITE_SHARED:
			if (mode.equals("r")) {
			} else {
				state = CacheState.OWNERSHIP_CHANGED;
			}
			break;
		case OWNERSHIP_CHANGED:
			if (mode.equals("r")) {
			} else {
				// TODO
			}
			break;

		default:
			break;
		}
		printState();
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
