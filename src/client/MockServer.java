package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import lib.FileContents;
import lib.ServerInterface;

public class MockServer implements ServerInterface {
	
	private Map<String, FileContents> testFileData;
	
	public MockServer() throws NotBoundException {
		testFileData = new HashMap<String, FileContents>();
		FileContents contents = new FileContents("hello!\nhow are you?\n".getBytes());
		testFileData.put("hello_world.txt", contents);
	}

	@Override
	public FileContents download(String client, String filename, String mode)
			throws RemoteException {
		return testFileData.get(filename);
	}

	@Override
	public boolean upload(String client, String filename, FileContents contents)
			throws RemoteException {
		testFileData.put(filename, contents);
		return true;
	}

}
