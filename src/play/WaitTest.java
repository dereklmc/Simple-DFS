package play;

public class WaitTest {

	public static class Client extends Thread {

		private Server server;
		private String name;
		private boolean doWriteBack = false;

		public Client(String name, Server server) {
			this.server = server;
			this.name = name;
		}

		@Override
		public void run() {
			for (int i = 0; i < 5; i++) {
				try {
					if (doWriteBack) {
						System.out.println(name
								+ " performing writeback request");
						server.upload(name);
						doWriteBack = false;
					} else {
						server.download(this, "r");
					}
					sleep(1000);
					if (doWriteBack) {
						System.out.println(name
								+ " performing writeback request");
						server.upload(name);
						doWriteBack = false;
					} else {
						server.download(this, "w");
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public String getClientName() {
			return name;
		}

		public void writeBack() {
			System.out.println(name + " recieved writeback request");
			doWriteBack = true;
		}

	}

	public static class Server {

		private Client owner = null;
		private int call = 0;
		private boolean fileNotUpdated;

		public void download(Client client, String mode)
				throws InterruptedException {
			int currentCall = call++;
			System.out.println(String.format(
					"[%s] :: Server#download called by %s for mode <%s>.",
					currentCall, client.getClientName(), mode));
			if (mode.equals("w") && owner != client) {
				System.out.println(String.format(
						"[%s] :: \t\tOwnership Change!", currentCall));
				if (owner != null) {
					System.out.println(String.format(
							"[%s] :: \t\tLast owner: %s", currentCall,
							owner.getClientName()));
					fileNotUpdated = true;
					owner.writeBack();
					while (fileNotUpdated) {
						System.out
								.println(String.format(
										"[%s] :: \t\tWaiting for update.",
										currentCall));
						Thread.sleep(500);
					}
				}
				owner = client;
			}
		}

		public void upload(String clientName) {
			System.out.println("Server#upload called by " + clientName + ".");
			fileNotUpdated = false;
//			notifyAll();
		}
	}

	public static void main(String[] args) {
		Server s = new Server();
		Client c1 = new Client("C1", s);
		Client c2 = new Client("C2", s);
		Client c3 = new Client("C3", s);
		c1.start();
		c2.start();
		c3.start();
		try {
			c1.join();
			c2.join();
			c3.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
