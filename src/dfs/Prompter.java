package dfs;

import java.io.IOException;
import java.util.Scanner;

public class Prompter {
	Scanner console;
	
	public Prompter() {
		console = new Scanner(System.in);
	}
	
	public boolean ask(String question) throws IOException {
		System.out.print(question + "(y/n): ");
		String in = console.nextLine();
		if (in.equals("y"))
			return true;
		else if (in.equals("n"))
			return false;
		else
			throw new IOException("Invalid response received: " + in);
		
	}
	
	public String prompt(String prompt) {
		System.out.print(prompt);
		return console.nextLine();
	}
}