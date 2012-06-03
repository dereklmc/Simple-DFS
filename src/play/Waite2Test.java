package play;

import play.Waite2Test.Foo;

public class Waite2Test {

	public static class Foo {

		int state = 0;

		public void triggerA() {
			System.out.println("WAITING");
			synchronized (this) {
				try {
					while (state == 0) {
						wait();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			System.out.println("STATE CHANGED");
		}
		
		public void triggerB() {
			synchronized (this) {
				state = 1;
				notifyAll();
			}
		}

	}

	public static class Bar extends Thread {

		private Foo foo;

		public Bar(Foo f) {
			foo = f;
		}

		@Override
		public void run() {
			foo.triggerA();
		}

	}

	public static class Baz extends Thread {

		private Foo foo;

		public Baz(Foo f) {
			foo = f;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			foo.triggerB();
		}

	}

	public static void main(String[] args) throws InterruptedException {
		Foo f = new Foo();
		Bar b1 = new Bar(f);
		Baz b2 = new Baz(f);

		b1.start();
		b2.start();

		b1.join();
		b2.join();
	}

}
