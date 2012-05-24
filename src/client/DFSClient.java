package client;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

import lib.ClientInterface;

public class DFSClient implements ClientInterface {

	private FileCache cache;

	public DFSClient() {

	}

	public boolean invalidate() throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean writeback() throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	private File open(String fileName, String mode) {
		// TODO Auto-generated method stub
		File file = cache.getFile(fileName);
		file.setReadable(true);
		file.setWritable(mode.equals("w"));
		return file;
	}

	public static void main(String[] args) {
		int port = Integer.parseInt(args[0]);
		System.out.println("Port: " + port);
		/*
		 * DFSClient client = new DFSClient();
		 * 
		 * Prompter input = new Prompter(); try { if
		 * (input.ask("Do you want to exit?")) System.exit(0); } catch
		 * (IOException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 * System.out.println("FileClient: Next file to open"); String fileName
		 * = input.prompt("Filename:"); String mode = input.prompt("How(r/w):");
		 * //client.open(fileName, mode);
		 * 
		 * System.out.println("Chose filename: " + "[" + mode + "] " +
		 * fileName);
		 */
		try {
			String fileName = "hello-world.txt";
			editFile(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param fileName
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void editFile(String fileName) throws IOException, InterruptedException {
		String[] command = { "sh", "-c", "emacs -nw " + fileName + " </dev/tty >/dev/tty" };
		Process p = Runtime.getRuntime().exec(command);
		Thread.sleep(5000);
		p.destroy();
		//p.waitFor();
	}
}