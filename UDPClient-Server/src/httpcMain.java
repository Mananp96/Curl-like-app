import java.net.URISyntaxException;
import java.util.Scanner;

/**
 * java httpc class takes the User Input.
 * @author Manan 
 * @author Khyati
 */
public class httpcMain {

	private static Scanner scanner;
	
	/**
	 * main method takes the user input from console and pass it to Client.java class.
	 * 
	 * @param args
	 * @throws URISyntaxException
	 */
	public static void main(String args[]) throws URISyntaxException {
		while(true) {
			System.out.print(">>");
			scanner = new Scanner(System.in);
			String userInput = scanner.nextLine();
			httpcClient client = new httpcClient(userInput);
			client.parseUserInput();
		}
	}
}
