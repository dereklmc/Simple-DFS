package dfs;

import java.rmi.*;

import lib.*;

public class DFSServer implements ServerInterface {

	@Override
	public FileContents download(String client, String filename, String mode)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean upload(String client, String filename, FileContents contents)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}
	
	public static void main(String[] args) {
		
	}
}