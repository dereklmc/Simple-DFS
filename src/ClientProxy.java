
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Establishes a connection to a remote client object.
 * @author derek
 *
 */
public class ClientProxy implements ClientInterface {
	
	private String name;
	private ClientInterface client;

	public ClientProxy(String address, String port) throws MalformedURLException, RemoteException, NotBoundException {
		name = address;
		
		String rmiClientAddress = String.format("rmi://%s:%s/fileclient", address, port);
		client = (ClientInterface) Naming.lookup(rmiClientAddress);
	}
	
	public ClientProxy(String name, ClientInterface client) {
		this.name = name;
		this.client = client;
	}

	@Override
	public boolean invalidate() throws RemoteException {
		return client.invalidate();
	}

	@Override
	public boolean writeback() throws RemoteException {
		return client.writeback();
	}

	public String getName() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		
		try {
			ClientProxy otherClient = (ClientProxy) obj;
			return otherClient.getName().equals(name);
		} catch (ClassCastException e) {
			return false;
		}
	}

}
