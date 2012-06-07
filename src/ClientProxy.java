
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Establishes a connection to a remote client object.
 * 
 * Functions as a proxy to the remote client. Used instead of a bare ClientInterface
 * to store additional information about the client as well as encapsulate the process
 * of connecting to the client.
 */
public class ClientProxy implements ClientInterface {
	
    // Name of the client
	private String name;
    // Reference to the remote client
	private ClientInterface client;
    
    /**
     * Constructs object for client at a given address with a given port.
     */
	public ClientProxy(String address, String port) throws MalformedURLException, RemoteException, NotBoundException {
		name = address;
		
		String rmiClientAddress = String.format("rmi://%s:%s/fileclient", address, port);
		client = (ClientInterface) Naming.lookup(rmiClientAddress);
	}
	
    /**
     * Constructs an object for an existing reference to a remote client with a given client name.
     */
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

    /**
     * Returns the name of the client.
     */
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
