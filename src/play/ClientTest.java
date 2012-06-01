package play;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import lib.ClientInterface;
import lib.Prompter;
import lib.ServerInterface;

public class ClientTest implements ClientInterface {

	@Override
	public boolean invalidate() throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean writeback() throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public static void main(String[] args) throws IOException, NotBoundException {
		ClientTest test = new ClientTest();
		Naming.rebind("rmi://localhost:" + args[1] + "/flieclient", test);
		
		InetAddress addr = InetAddress.getLocalHost();
		String localAddress = addr.getHostName();
		
		String address = String.format("rmi://%s:%s/%s", args[0], args[1], "dfsserver");
		ServerInterface server = (ServerInterface) Naming.lookup(address);
		
		Prompter input = new Prompter();
		String[] serverMethods = new String[] { "download", "upload" };
		
		while (true) {
			if (input.ask("Do you want to exit?"))
				System.exit(0);
			int methodChoice = input.promptChoice("Choose server method to execute",
					serverMethods);
			switch (methodChoice) {
			case 1:
				int accessChoice = input.promptChoice("Open file in mode", new String[] {"read", "write"});
				server.download(localAddress, "testfile.txt", accessChoice == 0 ? "r" : "w");
				break;
			case 2:
				server.upload(localAddress, "testfile.txt", null);
				break;
			default:
				System.err.println("Not a valid  choice! Exiting");
				System.exit(1);
				break;
			}
		}
	}

}
