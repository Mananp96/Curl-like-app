import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Client class
 * @author Manan
 *
 */
public class Client2 {
	
	private Socket socket;
	private BufferedReader userInput;
	private PrintWriter out;
	static String URL;
	int port;
	static String query;
	
	public Client2(String host, int port, String query ) {
		
		try 
		{
			socket = new Socket(host, port);
			System.out.println("connected...");
			out= new PrintWriter(socket.getOutputStream());
			out.println(query);
			out.println("\r\n");
			out.flush();
			this.printOutput();
			out.close();
			socket.close();
			
		} catch (IOException e) {
			System.out.println("");
			System.out.println("ERROR HTTP 404: Host Not Found");
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
	
	
	public static void main(String args[]) throws IOException, URISyntaxException {
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String httpfsClient = br.readLine();
		String[] commandClient = httpfsClient.split(" ");
		if(commandClient[0].equals("httpfs")) {
			for(int i =0; i<commandClient.length; i++) {
				if(commandClient[i].startsWith("-h")) {
					String httpfsHeader = commandClient[++i];
				}
				if(commandClient[i].startsWith("http://")){
					URL = commandClient[i];
				}
			}
		}
		URI uri = new URI(URL);
		String host = uri.getHost();
		int port = uri.getPort();
		query = uri.getPath();
		System.out.println(query.substring(1));
		Client2 client2 = new Client2(host,port,query.substring(1));
	}
	
}
