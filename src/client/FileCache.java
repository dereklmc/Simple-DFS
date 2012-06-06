package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import lib.AccessMode;
import lib.FileContents;
import lib.ServerInterface;

public class FileCache {

	private enum CacheState {
		INVALID, READ_SHARED, WRITE_OWNED, MODIFIED_OWNED, RELEASE_OWNERSHIP
	}
	
	private String name;
	private CacheState state = CacheState.INVALID;

	private ServerInterface fileServer;
	private File tempFile;

	public FileCache(ServerInterface fileServer, String cacheLocation)
			throws IOException {
		this.fileServer = fileServer;
		tempFile = new File("/tmp/dlm18.txt");
		if (!tempFile.exists())
			tempFile.createNewFile();
	}

	public FileContents getContents() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setMode(String mode) {
		
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
			state = CacheState.WRITE_OWNED;
			break;
		case MODIFIED_OWNED:
			state = CacheState.WRITE_OWNED;
			break;
		default:
			throw new IllegalStateException();
		}
	}
	
	public void launchEditor() {
		String[] command = { "sh", "-c", "vim " + tempFile.getAbsolutePath() + " </dev/tty >/dev/tty" };
//		String[] command = { "gvim", "-f", tempFile.getAbsolutePath() };
		try {
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
		} catch (IOException e) {
			System.err.println("Could not open local cache of file with emacs");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized boolean invalidateFile() throws RemoteException {
		switch (state) {
		case INVALID:
			state = CacheState.INVALID;
			break;
		case READ_SHARED:
			state = CacheState.INVALID;
			break;
		case WRITE_OWNED:
			fileServer.upload("", name, getContents());
			state = CacheState.INVALID;
			break;
		case MODIFIED_OWNED:
			fileServer.upload("", name, getContents());
			state = CacheState.INVALID;
			break;
		default:
			return false;
		}
		return true;
	}

	public synchronized boolean writeBack() throws RemoteException {
		switch (state) {
		case WRITE_OWNED:
			state = CacheState.RELEASE_OWNERSHIP;
			break;
		case MODIFIED_OWNED:
			fileServer.upload("", name, getContents());
			state = CacheState.READ_SHARED;
			break;
		default:
			return false;
		}
		return true;
	}

	public synchronized void completeSession() throws RemoteException {
		if (state == CacheState.RELEASE_OWNERSHIP) {
			fileServer.upload("", name, getContents());
			state = CacheState.READ_SHARED;
		}
	}

	private void downloadFile(String fileName, AccessMode mode)
			throws RemoteException {
		FileContents contents = fileServer.download("", fileName, mode.toString());
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
}
