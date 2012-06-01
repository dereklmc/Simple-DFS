package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;

import server.CachedFile.CacheState;

import lib.AccessMode;
import lib.ClientInterface;
import lib.FileContents;

public class CachedFile {

	public enum CacheState {
		READ_SHARED, WRITE_SHARED, OWNERSHIP_CHANGED, NOT_SHARED;
	}

	private String fileName;
	private CacheState state;
	private List<String> readers;
	private String owner;
	private File storedFile;
	private byte[] data;

	public CachedFile(String fileName) {
		this.fileName = fileName;
		state = CacheState.NOT_SHARED;
		readers = new ArrayList<String>();
		owner = null;
		
		storedFile = new File(fileName);
		data = new byte[(int) storedFile.length()];
		try {
			FileInputStream fileReader = new FileInputStream(storedFile);
			fileReader.read(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public String getName() {
		return fileName;
	}

	public void registerClient(String client, AccessMode mode) {
		switch(state) {
			case NOT_SHARED:
				if (mode == AccessMode.READ) {
					readers.add(client);
					state = CacheState.READ_SHARED;
				} else {
					owner = client;
					state = CacheState.WRITE_SHARED;
				}
				break;
			case READ_SHARED:
				if (mode == AccessMode.READ) {
					readers.add(client);
				} else {
					owner = client;
					state = CacheState.WRITE_SHARED;
				}
				break;
			case WRITE_SHARED:
				if (mode == AccessMode.READ) {
					readers.add(client);
				} else {
//					writebackClient(client);
					// Suspend
					state = CacheState.OWNERSHIP_CHANGED;
				}
				break;
			case OWNERSHIP_CHANGED:
				if (mode == AccessMode.READ) {
					readers.add(client);
				} else {
					// TODO
				}
				break;
			default:
				
		}
	}
	
//	private void writebackClient(String clientAddress) {
//		String rmiClientAddress = String.format("rmi://%s:%s/fileclient");
//		ClientInterface client = (ClientInterface) Naming.lookup(clientAddress);
//	}

	public boolean updateContents(FileContents contents) {
		switch(state) {
			case WRITE_SHARED:
				// TODO
				data = contents.get();
				for (String client : readers) {
					// TODO invalidate
				}
				state = CacheState.NOT_SHARED;
				break;
			case OWNERSHIP_CHANGED:
				// TODO
				state = CacheState.WRITE_SHARED;
				break;
			default:
				return false;
		}
		return false;
	}

	public FileContents getContents() {
		return new FileContents(data);
	}

	public CacheState getState() {
		return state;
	}

	public void addReader(String client) {
		readers.add(client);
	}

	public void setState(CacheState newState) {
		state = newState;
	}

	public void setOwner(String client) {
		owner= client;
	}

}
