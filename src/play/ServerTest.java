package play;

import ClientInterface;
import FileContents;
import ServerInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class ServerTest extends UnicastRemoteObject implements ServerInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum CacheState {
		READ_SHARED, WRITE_SHARED, OWNERSHIP_CHANGED, NOT_SHARED;
	}

	private CacheState state;
	private Set<String> readers;
	private String owner = "";
	private String port;

	public ServerTest(String port) throws RemoteException, MalformedURLException {
		super();
		readers = new HashSet<String>();
		state = CacheState.NOT_SHARED;
		printState();
		this.port = port;
		Naming.rebind("rmi://localhost:" + port + "/dfsserver", this);
	}

	private void printState() {
		System.out.println(String.format("Current State: <%s>", state.name()));
	}

	@Override
	public FileContents download(String client, String filename, String mode)
			throws RemoteException {
		System.out.println(String.format(
				"Recieved Download request from %s for file \"%s\" in mode <%s>.", client,
				filename, mode));
		printState();
		System.out.println("Changing state...");
		if (mode.equals("r")) {
			readers.add(client);
			if (state == CacheState.NOT_SHARED)
				state = CacheState.READ_SHARED;
		} else {
			if (state == CacheState.NOT_SHARED || state == CacheState.READ_SHARED) {
				owner = client;
				state = CacheState.WRITE_SHARED;
			} else if (!client.equals(owner)) {
				synchronized (this) {
					while (state == CacheState.WRITE_SHARED) {
						writebackClient(owner);
						try {
							wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
					}
					owner = client;
					state = CacheState.WRITE_SHARED;
				}
			}
		}
		
//		do {
//			switch (state) {
//			case NOT_SHARED:
//				if (mode.equals("r")) {
//					readers.add(client);
//					state = CacheState.READ_SHARED;
//				} else {
//					owner = client;
//					state = CacheState.WRITE_SHARED;
//				}
//				break;
//			case READ_SHARED:
//				if (mode.equals("r")) {
//					readers.add(client);
//				} else {
//					owner = client;
//					state = CacheState.WRITE_SHARED;
//				}
//				break;
//			case OWNERSHIP_CHANGED:
//				if (mode.equals("r")) {
//					readers.add(client);
//					break;
//				} else {
//					// TODO
//					synchronized (this) {
//						try {
//							while (state == CacheState.OWNERSHIP_CHANGED) {
//								wait();
//							}
//						} catch (InterruptedException e) {
//							RemoteException re = new RemoteException();
//							re.initCause(e);
//							throw re;
//						}
//					}
//				}
//			case WRITE_SHARED:
//				if (mode.equals("r")) {
//					readers.add(client);
//				} else {
//					state = CacheState.OWNERSHIP_CHANGED;
//					writebackClient(owner);
//					synchronized (this) {
//						try {
//							while (state == CacheState.OWNERSHIP_CHANGED) {
//								wait();
//							}
//						} catch (InterruptedException e) {
//							RemoteException re = new RemoteException();
//							re.initCause(e);
//							throw re;
//						}
//					}
//				}
//				break;
//
//			default:
//				break;
//			}
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				RemoteException re = new RemoteException();
//				re.initCause(e);
//				throw re;
//			}
//		} while (state == CacheState.OWNERSHIP_CHANGED);
		System.out.println("Current Readers:");
		for (String reader : readers) {
			System.out.println("\t" + reader);
		}
		printState();
		return null;
	}

	private void writebackClient(String clientAddress) {
		try {
			ClientInterface client = getRemoteClient(clientAddress);
			client.writeback();
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

	private void invalidateClient(String clientAddress) {
		try {
			ClientInterface client = getRemoteClient(clientAddress);
			client.invalidate();
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

	public ClientInterface getRemoteClient(String clientAddress) throws NotBoundException,
			MalformedURLException, RemoteException {
		String rmiClientAddress = String.format("rmi://%s:%s/fileclient", clientAddress, port);
		ClientInterface client = (ClientInterface) Naming.lookup(rmiClientAddress);
		return client;
	}

	@Override
	public boolean upload(String client, String filename, FileContents contents)
			throws RemoteException {
		System.out.println(String.format("Recieved Upload request from %s for file \"%s\".",
				client, filename));
		printState();
		for (Iterator<String> iterator = readers.iterator(); iterator.hasNext();) {
			String reader = iterator.next();
			System.out.println("Invalidating reader <" + reader + ">");
			invalidateClient(reader);
			iterator.remove();
		}
		state = CacheState.NOT_SHARED;
		synchronized (this) {
			notifyAll();
		}
//		switch (state) {
//		case WRITE_SHARED:
//			state = CacheState.NOT_SHARED;
//			break;
//		case OWNERSHIP_CHANGED:
//			state = CacheState.WRITE_SHARED;
//			synchronized (this) {
//				notifyAll();
//			}
//			break;
//		default:
//			break;
//		}
		printState();
		return false;
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: java server.DFSServer port#");
			System.exit(-1);
		}
		try {
			ServerTest dfs = new ServerTest(args[0]);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
