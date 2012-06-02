package lib;

import java.io.IOException;
import java.util.Scanner;

public class Prompter {
	Scanner console;
	private boolean waitingForInput = false;
	
	public Prompter() {
		console = new Scanner(System.in);
	}
	
	public boolean ask(String question) throws IOException {
		System.out.print(question + "(y/n): ");
		String in = getInput();
		if (in.equals("y"))
			return true;
		else if (in.equals("n"))
			return false;
		else
			throw new IOException("Invalid response received: " + in);
		
	}

	private String getInput() {
		waitingForInput = true;
		String in = console.nextLine();
		waitingForInput = false;
		notifyAll();
		return in;
	}
	
	public String prompt(String prompt) {
		System.out.print(prompt);
		return getInput();
	}

	public int promptChoice(String desc, String[] serverMethods) {
		System.out.println(desc);
		for (int i = 0; i < serverMethods.length; i++) {
			String choiceDisplay = String.format(" [%s] %s", i+1, serverMethods[i]);
			System.out.println(choiceDisplay);
		}
		String choice = prompt("Choice? (select number) : ");
		return Integer.parseInt(choice);
	}

	public boolean isWaitingForInput() {
		return waitingForInput ;
	}
}