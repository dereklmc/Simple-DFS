import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Writes data to a file asynchronously.
 *
 * Is somewhat optimistic that the thread will complete without error.
 */
public class AsyncFileWriter extends Thread {
	
    // The file to write to
	private File file;
    // the data to write
	private byte[] data;
    
    /**
     * Constructs the thread to write the specified data to the specified file.
     *
     * @param file - the file to write to
     * @param data - the data to write to the given file.
     */
	public AsyncFileWriter(File file, byte[] data) {
		this.file = file;
		this.data = data;
	}
	
    /*
     * Performs the actual write operation.
     *
     * @see Thread#run()
     */
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
