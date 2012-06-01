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

	private enum FileCacheState {
		INVALID, READ_SHARED, WRITE_OWNED, MODIFIED_OWNED, RELEASE_OWNERSHIP
	}
	
	private String name;
	private String accessMode;
	private boolean ownership;
	private FileCacheState state = FileCacheState.INVALID;

	private ServerInterface fileServer;
	private File tempFile;

	public FileCache(ServerInterface fileServer, String cacheLocation)
			throws IOException {
		this.fileServer = fileServer;
		tempFile = new File("useraccount.txt");
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
			state = FileCacheState.INVALID;
		}
		switch (state) {
		case INVALID:
			downloadFile(fileName, mode);
			if (mode == AccessMode.READ)
				state = FileCacheState.READ_SHARED;
			else
				state = FileCacheState.WRITE_OWNED;
			break;
		case READ_SHARED:
			if (mode == AccessMode.WRITE) {
				downloadFile(fileName, mode);
				state = FileCacheState.WRITE_OWNED;
			}
			break;
		case WRITE_OWNED:
			state = FileCacheState.WRITE_OWNED;
			break;
		case MODIFIED_OWNED:
			state = FileCacheState.WRITE_OWNED;
			break;
		default:
			throw new IllegalStateException();
		}
	}
	
	public void launchEditor() {
//		String[] command = { "sh", "-c",
//		"vim useraccount.txt </dev/tty >/dev/tty" };
		String[] command = { "gvim", "-f", tempFile.getAbsolutePath() };
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

	public synchronized void invalidateFile() throws RemoteException {
		switch (state) {
		case INVALID:
			state = FileCacheState.INVALID;
			break;
		case READ_SHARED:
			state = FileCacheState.INVALID;
			break;
		case WRITE_OWNED:
			fileServer.upload("", name, getContents());
			state = FileCacheState.INVALID;
			break;
		case MODIFIED_OWNED:
			fileServer.upload("", name, getContents());
			state = FileCacheState.INVALID;
			break;
		default:
			throw new IllegalStateException();
		}
	}

	public synchronized void writeBack() throws RemoteException {
		switch (state) {
		case WRITE_OWNED:
			state = FileCacheState.RELEASE_OWNERSHIP;
			break;
		case MODIFIED_OWNED:
			fileServer.upload("", name, getContents());
			state = FileCacheState.READ_SHARED;
			break;
		default:
			throw new IllegalStateException();
		}
	}

	public synchronized void completeSession() throws RemoteException {
		if (state == FileCacheState.RELEASE_OWNERSHIP) {
			fileServer.upload("", name, getContents());
			state = FileCacheState.READ_SHARED;
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