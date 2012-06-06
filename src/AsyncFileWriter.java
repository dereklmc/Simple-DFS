

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AsyncFileWriter extends Thread {
	
	private File file;
	private byte[] data;

	public AsyncFileWriter(File file, byte[] data) {
		this.file = file;
		this.data = data;
	}
	
	@Override
	public void run() {
		try {
			FileOutputStream writer = new FileOutputStream(file);
			writer.write(data);
		} catch (IOException e) {
			System.err.println("Could not write cached file to persistant storage!");
			e.printStackTrace();
		}
	}
}