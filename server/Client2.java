import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Client class
 * @author Manan
 *
 */
public class Client2 {
	
	private Socket socket;
	private BufferedReader userInput;
	private PrintWriter out;
	
	public Client2(String address, int port ) {
		
		try 
		{
			socket = new Socket(address, port);
			System.out.println("connected...");
			
			userInput = new BufferedReader(new InputStreamReader(System.in));
			String userCommand = userInput.readLine();
			
			out= new PrintWriter(socket.getOutputStream());
			out.println(userCommand);
			out.println("\r\n");
			out.flush();
			this.printOutput();
			userInput.close();
			out.close();
			socket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
		

	public void printOutput() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String output;
			while((output = br.readLine()) != null) {
				System.out.println(output);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public static void main(String args[]) {
		Client2 client2 = new Client2("127.0.0.1",569);
	}
	
}
