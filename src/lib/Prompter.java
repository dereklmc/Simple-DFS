package lib;

import java.io.IOException;
import java.util.Scanner;

public class Prompter {
	Scanner console;
	
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
		return console.nextLine();
	}
	
	public String prompt(String prompt) {
		System.out.print(prompt);
		return getInput();
	}

	public int promptChoice(String desc, String[] options) {
		System.out.println(desc);
		for (int i = 0; i < options.length; i++) {
			String choiceDisplay = String.format(" [%s] %s", i+1, options[i]);
			System.out.println(choiceDisplay);
		}
		String choice = prompt("Choice? (select number) : ");
		return Integer.parseInt(choice);
	}
	
	public String promptChoices(String desc, String[] options) {
		int choice = promptChoice(desc, options);
		return options[choice];
	}
}
