import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


public class Client2 {
	
	private Socket socket;
	private BufferedReader userInput;
	private DataOutputStream out;
	
	
	public Client2(String address, int port ) {
		
		try 
		{
			socket = new Socket(address, port);
			System.out.println("connected...");
			
			userInput = new BufferedReader(new InputStreamReader(System.in));
			String userCommand = userInput.readLine();
			
			out= new DataOutputStream(socket.getOutputStream());
			out.writeUTF(userCommand);
			
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
	
	public void close() {
		//close the connection
		try 
		{
			userInput.close();
			out.close();
			socket.close();
		}catch(IOException i) {
			System.out.println(i);
		}
	}
	
	
	public static void main(String args[]) {
		Client2 client2 = new Client2("127.0.0.1", 5555);
		client2.printOutput();
		client2.close();
		
	}
	
}
