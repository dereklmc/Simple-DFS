package server;

import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import server.CachedFile.CacheState;

import lib.AccessMode;
import lib.ClientInterface;
import lib.FileContents;
import lib.ServerInterface;

public class DFSServer extends UnicastRemoteObject implements ServerInterface {
	
	private List<CachedFile> fileCache;
	
	protected DFSServer() throws RemoteException {
		super();
		fileCache = new ArrayList<CachedFile>();
	}

	@Override
	public FileContents download(String client, String filename, String mode)
			throws RemoteException {
		CachedFile file = getCachedFile(filename);
		if (file == null) {
			file = new CachedFile(filename);
			fileCache.add(file);
		}
//		AccessMode accessMode = AccessMode.getMode(mode);
//		file.registerClient(client, accessMode);
		
		switch(file.getState()) {
		case NOT_SHARED:
			if (mode.equals("r")) {
				file.addReader(client);
				file.setState(CacheState.READ_SHARED);
			} else {
				file.setOwner(client);
				file.setState(CacheState.WRITE_SHARED);
			}
			break;
		case READ_SHARED:
			if (mode.equals("r")) {
				file.addReader(client);
			} else {
				file.setOwner(client);
				file.setState(CacheState.WRITE_SHARED);
			}
			break;
		case WRITE_SHARED:
			if (mode.equals("r")) {
				file.addReader(client);
			} else {
				writebackClient(client);
				// Suspend
				try {
					wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				file.setState(CacheState.OWNERSHIP_CHANGED);
			}
			break;
		case OWNERSHIP_CHANGED:
			if (mode.equals("r")) {
				file.addReader(client);
			} else {
				// TODO
			}
			break;
		default:
			
	}
		
		
		return file.getContents();
	}

	@Override
	public boolean upload(String client, String filename, FileContents contents)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void writebackClient(String clientAddress) {
		String rmiClientAddress = String.format("rmi://%s:%s/fileclient");
		try {
			ClientInterface client = (ClientInterface) Naming.lookup(clientAddress);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private CachedFile getCachedFile(String fileName) {
		for (CachedFile file : fileCache) {
			if (file.getName().equals(fileName))
				return file;
		}
		return null;
	}
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: java server.DFSServer port#");
			System.exit(-1);
		}
		try {
			DFSServer dfs = new DFSServer();
			Naming.rebind("rmi://localhost:" + args[0] + "/dfsserver", dfs);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}