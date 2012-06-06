

import java.util.Scanner;

/**
 * Encapsulates utility functions for getting input from a shell.
 * 
 */
public class Prompter {

	// The shell to read from
	Scanner console;

	/**
	 * Default constructor.
	 * 
	 * Constructs prompter to get input from stdin.
	 */
	public Prompter() {
		console = new Scanner(System.in);
	}

	/**
	 * Ask a yes or a no question.
	 * 
	 * These questions have only two possibly answers: yes or no. This function
	 * renders these answers as boolean values.
	 * 
	 * Keeps asking the question until the user answers either yes or no,
	 * assuming the user can and will.
	 * 
	 * @param question
	 *            - A yes or no question to ask the user.
	 * @return true if the user selected yes, otherwise false.
	 */
	public boolean ask(String question) {
		// Loop until the user enters valid input.
		// This can be bad if the user never will enter valid input.
		while (true) {
			System.out.print(question + "(y/n): ");
			String in = console.nextLine();
			if (in.equals("y"))
				return true;
			else if (in.equals("n"))
				return false;
			else
				System.out.println("Invalid response received: " + in);
		}

	}

	/**
	 * Asks a question that requires some arbitrary string as an answer.
	 * 
	 * @param prompt
	 *            - A description of what the user should enter.
	 * @return - The input the user entered in response to the prompt.
	 */
	public String prompt(String prompt) {
		System.out.print(prompt + ": ");
		return console.nextLine();
	}

	/**
	 * Asks the user to select from one of several specified inputs.
	 * 
	 * @param desc
	 *            - A description of what the options mean or why the user is
	 *            selecting between them.
	 * @param options
	 *            - the options to choose from.
	 * @return The index of the chosen option.
	 */
	public int promptChoice(String desc, String[] options) {
		System.out.println(desc);
		// Print options.
		for (int i = 0; i < options.length; i++) {
			String choiceDisplay = String.format(" [%s] %s", i, options[i]);
			System.out.println(choiceDisplay);
		}
		String choice = prompt("Choice? (select number) : ");
		return Integer.parseInt(choice);
	}
	
	/**
	 * Asks the user to select from one of several specified inputs.
	 * 
	 * @param desc
	 *            - A description of what the options mean or why the user is
	 *            selecting between them.
	 * @param options
	 *            - the options to choose from.
	 * @return The value of the chosen option.
	 */
	public String promptChoices(String desc, String[] options) {
		int choice = promptChoice(desc, options);
		return options[choice];
	}
}
