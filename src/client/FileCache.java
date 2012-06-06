package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;

import lib.AccessMode;
import lib.FileContents;
import lib.ServerInterface;

public class FileCache {

	private static final String LOCAL_CACHE_PATH = "/tmp/dlm18.txt";

	private enum CacheState {
		INVALID, READ_SHARED, WRITE_OWNED, MODIFIED_OWNED, RELEASE_OWNERSHIP
	}
	
	private String name;
	private CacheState state = CacheState.INVALID;

	private ServerInterface fileServer;
	private File tempFile;
	
	private String clientName;
	
	public FileCache(ServerInterface fileServer) throws IOException {
		this.fileServer = fileServer;
		tempFile = new File(LOCAL_CACHE_PATH);
		if (!tempFile.exists())
			tempFile.createNewFile();
		
		InetAddress addr = InetAddress.getLocalHost();
		clientName = addr.getHostName();
	}

	public FileContents getContents() throws IOException {
		byte[] data = new byte[(int) tempFile.length()];
		FileInputStream fileReader = new FileInputStream(tempFile);
		fileReader.read(data);
		return new FileContents(data);
	}

	public synchronized void openFile(String fileName, AccessMode mode)
			throws RemoteException {
		if (!fileName.equals(name)) {
			state = CacheState.INVALID;
		}
		switch (state) {
		case INVALID:
			downloadFile(fileName, mode);
			if (mode == AccessMode.READ)
				state = CacheState.READ_SHARED;
			else
				state = CacheState.WRITE_OWNED;
			break;
		case READ_SHARED:
			if (mode == AccessMode.WRITE) {
				downloadFile(fileName, mode);
				state = CacheState.WRITE_OWNED;
			}
			break;
		case WRITE_OWNED:
			if (!fileName.equals(name)) {
				try {
                         	       fileServer.upload(clientName, name, getContents());
                        	} catch (IOException e) {
                        	        throw new RemoteException("Could not read contents of local file cache.", e);
                        	}
				downloadFile(fileName, mode);
			}
			state = CacheState.WRITE_OWNED;
			break;
		case MODIFIED_OWNED:
			if (!fileName.equals(name)) {
				try {
                         	       fileServer.upload(clientName, name, getContents());
                        	} catch (IOException e) {
                        	        throw new RemoteException("Could not read contents of local file cache.", e);
                        	}
				downloadFile(fileName, mode);
			}
			state = CacheState.WRITE_OWNED;
			break;
		default:
			throw new IllegalStateException();
		}
	}

	public synchronized boolean invalidateFile() throws RemoteException {
		if (state == CacheState.READ_SHARED) {
			state = CacheState.INVALID;
			return true;
		}
		return false;
	}

	public synchronized boolean writeBack() throws RemoteException {
		boolean result = true;
		switch (state) {
		case WRITE_OWNED:
			state = CacheState.RELEASE_OWNERSHIP;
			break;
		case MODIFIED_OWNED:
			try {
				fileServer.upload(clientName, name, getContents());
			} catch (IOException e) {
				throw new RemoteException("Could not read contents of local file cache.", e);
			}
			if (result)
				state = CacheState.READ_SHARED;
			break;
		default:
			result = false;
		}
		return result;
	}

	public synchronized void completeSession() throws RemoteException {
		if (state == CacheState.RELEASE_OWNERSHIP) {
			try {
				fileServer.upload(clientName, name, getContents());
			} catch (IOException e) {
				throw new RemoteException("Could not read contents of local file cache.", e);
			}
			state = CacheState.READ_SHARED;
		}
	}

	private void downloadFile(String fileName, AccessMode mode)
			throws RemoteException {
		FileContents contents = fileServer.download(clientName, fileName, mode.toString());
		putFile(fileName, contents);
		tempFile.setWritable(mode == AccessMode.WRITE);
	}

	private void putFile(String fileName, FileContents contents) {
		name = fileName;
		try {
			tempFile.setWritable(true);
			FileOutputStream tempFileWriter = new FileOutputStream(tempFile);
			tempFileWriter.write(contents.get());
			tempFileWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getCachePath() {
		return LOCAL_CACHE_PATH;
	}
}
