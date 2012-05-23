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
			try {
				downloadFile(fileName, "r");
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
			return READ_SHARED;
		}

		@Override
		public CacheState writeFile(String fileName, FileCache cache) {
			try {
				downloadFile(fileName, "w");
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
			return WRITE_OWNED;
		}

		@Override
		public CacheState replaceFile() {
			throw new IllegalStateTransistionException();
		}

		@Override
		public CacheState invalidateFile() {
			throw new IllegalStateTransistionException();
		}

		@Override
		public CacheState completeSession() {
			throw new IllegalStateTransistionException();
		}

		@Override
		public CacheState writeBack() {
			throw new IllegalStateTransistionException();
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
			try {
				downloadFile(fileName, "w");
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
			throw new IllegalStateTransistionException();
		}

		@Override
		public CacheState writeBack() {
			// TODO Auto-generated method stub
			throw new IllegalStateTransistionException();
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
			throw new IllegalStateTransistionException();
		}

		@Override
		public CacheState completeSession() {
			// TODO Auto-generated method stub
			throw new IllegalStateTransistionException();
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
			throw new IllegalStateTransistionException();
		}

		@Override
		public CacheState writeFile(String fileName, FileCache cache) {
			// TODO Auto-generated method stub
			throw new IllegalStateTransistionException();
		}

		@Override
		public CacheState replaceFile() {
			// TODO Auto-generated method stub
			throw new IllegalStateTransistionException();
		}

		@Override
		public CacheState invalidateFile() {
			// TODO Auto-generated method stub
			throw new IllegalStateTransistionException();
		}

		@Override
		public CacheState completeSession() {
			// TODO Auto-generated method stub
			return READ_SHARED;
		}

		@Override
		public CacheState writeBack() {
			// TODO Auto-generated method stub
			throw new IllegalStateTransistionException();
		}
	};
	
	public static class IllegalStateTransistionException extends UnsupportedOperationException {
		
	};
	
	private FileCache cache;

	private CacheState() {
		cache = FileCache.getCache();
	}

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
	public void downloadFile(String fileName, String mode) throws NotBoundException,
			MalformedURLException, RemoteException {
		ServerInterface dfsServer = (ServerInterface) Naming.lookup("");
		FileContents contents = dfsServer.download("", fileName, mode);
		cache.putFile(fileName, contents);
	}
}
