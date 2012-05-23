package client;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import lib.FileContents;
import lib.ServerInterface;

public enum CacheState {
	INVALID {
		@Override
		public CacheState readFile(String fileName, FileCache cache) {
			FileContents contents = openRemoteFile(fileName, "r");
			cache.putFile(fileName, contents);
			return READ_SHARED;
		}

		@Override
		public CacheState writeFile(String fileName, FileCache cache) {
			FileContents contents = openRemoteFile(fileName, "w");
			cache.putFile(fileName, contents);
			return WRITE_OWNED;
		}

		@Override
		public CacheState replaceFile() {
			throw new IllegalStateException();
		}

		@Override
		public CacheState invalidateFile() {
			throw new IllegalStateException();
		}

		@Override
		public CacheState completeSession() {
			throw new IllegalStateException();
		}

		@Override
		public CacheState writeBack() {
			throw new IllegalStateException();
		}
	},
	READ_SHARED {
		@Override
		public CacheState readFile(String fileName, FileCache cache) {
			// TODO Auto-generated method stub
			return READ_SHARED;
		}

		@Override
		public CacheState writeFile(String fileName, FileCache cache) {
			FileContents file = openRemoteFile(fileName, "w");
			return WRITE_OWNED;
		}

		@Override
		public CacheState replaceFile() {
			// TODO Auto-generated method stub
			return INVALID;
		}

		@Override
		public CacheState invalidateFile() {
			// TODO Auto-generated method stub
			return INVALID;
		}

		@Override
		public CacheState completeSession() {
			// TODO Auto-generated method stub
			throw new IllegalStateException();
		}

		@Override
		public CacheState writeBack() {
			// TODO Auto-generated method stub
			throw new IllegalStateException();
		}
	},
	WRITE_OWNED {
		@Override
		public CacheState readFile(String fileName, FileCache cache) {
			// TODO Auto-generated method stub
			return WRITE_OWNED;
		}

		@Override
		public CacheState writeFile(String fileName, FileCache cache) {
			// TODO Auto-generated method stub
			return WRITE_OWNED;
		}

		@Override
		public CacheState replaceFile() {
			// TODO Auto-generated method stub
			return INVALID;
		}

		@Override
		public CacheState invalidateFile() {
			// TODO Auto-generated method stub
			throw new IllegalStateException();
		}

		@Override
		public CacheState completeSession() {
			// TODO Auto-generated method stub
			throw new IllegalStateException();
		}

		@Override
		public CacheState writeBack() {
			// TODO Auto-generated method stub
			return RELEASE_OWNERSHIP;
		}
	},
	RELEASE_OWNERSHIP {
		@Override
		public CacheState readFile(String fileName, FileCache cache) {
			// TODO Auto-generated method stub
			throw new IllegalStateException();
		}

		@Override
		public CacheState writeFile(String fileName, FileCache cache) {
			// TODO Auto-generated method stub
			throw new IllegalStateException();
		}

		@Override
		public CacheState replaceFile() {
			// TODO Auto-generated method stub
			throw new IllegalStateException();
		}

		@Override
		public CacheState invalidateFile() {
			// TODO Auto-generated method stub
			throw new IllegalStateException();
		}

		@Override
		public CacheState completeSession() {
			// TODO Auto-generated method stub
			return READ_SHARED;
		}

		@Override
		public CacheState writeBack() {
			// TODO Auto-generated method stub
			throw new IllegalStateException();
		}
	};

	public abstract CacheState readFile(String fileName, FileCache cache);

	public abstract CacheState writeFile(String fileName, FileCache cache);

	public abstract CacheState replaceFile();

	public abstract CacheState invalidateFile();

	public abstract CacheState writeBack();

	public abstract CacheState completeSession();
	
	/**
	 * @param fileName
	 * @param mode
	 * @return 
	 * @throws NotBoundException
	 * @throws MalformedURLException
	 * @throws RemoteException
	 */
	public FileContents openRemoteFile(String fileName, String mode) throws NotBoundException,
			MalformedURLException, RemoteException {
		ServerInterface dfsServer = (ServerInterface) Naming.lookup("");
		return dfsServer.download("", fileName, mode);
	}
}
