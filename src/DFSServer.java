

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;



public class DFSServer extends UnicastRemoteObject implements ServerInterface {
	
	
	private static final long serialVersionUID = 1539908244124054096L;
	private List<CachedFile> fileCache;
	private String port;
	
	protected DFSServer(String port) throws RemoteException {
		fileCache = new ArrayList<CachedFile>();
		this.port = port;
	}

	@Override
	public FileContents download(String clientName, String filename, String mode)
			throws RemoteException {
		CachedFile file = getCachedFile(filename);
		System.out.println(String.format("Downloading file \"%s\" in mode [%s]. ", filename, mode));
		if (file == null) {
			System.out.println("\tFile not cached. Caching file. ");
			try {
				file = new CachedFile(filename);
			} catch (IOException e) {
				throw new RemoteException("Could not open requested file!", e);
			}
			fileCache.add(file);
		}
		
		try {
			ClientProxy client = new ClientProxy(clientName, port);
			if (mode.equals("r"))
				file.registerReader(client);
			else
				file.registerWriter(client);
			return file.getContents();
		} catch (IllegalArgumentException e) {
			throw new RemoteException("Bad request!", e);
		} catch (MalformedURLException e) {
			throw new RemoteException("Error creating connection to client requesting file download.", e);
		} catch (NotBoundException e) {
			throw new RemoteException("Error creating connection to client requesting file download.", e);
		}
	}

	@Override
	public boolean upload(String clientName, String filename, FileContents contents)
			throws RemoteException {
		CachedFile file = getCachedFile(filename);
		System.out.println(String.format("Uploading file \"%s\".", filename));
		return file.updateContents(clientName, contents);
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
			DFSServer dfs = new DFSServer(args[0]);
			Naming.rebind("rmi://localhost:" + args[0] + "/dfsserver", dfs);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}