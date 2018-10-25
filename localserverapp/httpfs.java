import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server class.
 * @author 
 *
 */
public class httpfs {

	static boolean isPort = false;
	static boolean isVerbose = false;
	static boolean isPathToDir = false;
	
	static int port;
	private static String pathToDir;
	String crlf = "\r\n";
	String clientRequest; // client input
	
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private DataInputStream in = null; // input stream to get request from Client
	private PrintWriter out = null; // output stream send response to client

	/**
	 * Constructor of Server class used to:<br>
	 * - start a Server.<br>
	 * - make a connection with Client.<br>
	 * - get the request of client and call the GET or POST method of server class according to request.
	 * 
	 * @param port port number of Server.
	 */
	public httpfs(int port) {
		try
		{
			serverSocket = new ServerSocket(port);
			System.out.println("Server started....");
			System.out.println("Waiting for connection");

			socket = serverSocket.accept();
			System.out.println("Client connection established....");

			//get the data from client
			in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());
			clientRequest = in.readUTF();
			System.out.println(clientRequest);

			if(clientRequest.startsWith("GET")) {
				this.getServerRequest(clientRequest.substring(4));
			}else if(clientRequest.startsWith("POST")) {
				this.postServerRequest(clientRequest.substring(5));
			}

			out.println("");
			out.flush();
			socket.close();
			in.close();

		}catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * "GET /" returns a list of the current files in the data directory.<br>
	 * "GET /foo" returns the content of the file named foo in the data directory.<br>
	 * 
	 * @param fileName name of file.
	 * 
	 */
	public void getServerRequest(String fileName) {

		File filePath = new File(pathToDir+fileName);
		if(filePath.exists()) {
			if(filePath.isDirectory()) {	
				File[] listOfFiles = filePath.listFiles();
				for(File file : listOfFiles) {
					if(file.isFile()) {
						System.out.println("File      >> "+file.getName());
						out.println(file.getName());
					}else if(file.isDirectory()) {
						System.out.println("Directory >> "+file.getName());
						out.println(file.getName());
					}
				}
			}else if(filePath.isFile()) {
				System.out.println("path: "+pathToDir+fileName);
				FileReader fileReader;
				try {
					fileReader = new FileReader(pathToDir+fileName);
					BufferedReader bufferedReader = new BufferedReader(fileReader);
					String currentLine;
					String fileData = null;
					while ((currentLine = bufferedReader.readLine()) != null) {
						fileData = fileData + currentLine;
						out.println(currentLine);
					}
				} catch (FileNotFoundException e) {
					System.out.println("ERROR HTTP 404");
					out.println("ERROR HTTP 404");
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		} else {
			System.out.println("ERROR HTTP 404");
			out.println("ERROR HTTP 404");
		}	
	}


	/**
	 * "POST /bar" should create or overwrite the file named bar in the data directory<br>
	 * with the content of the body of the request.<br>
	 * options for the POST such as overwrite=true|false.
	 * 
	 * @param fileName name of file. 
	 */
	public void postServerRequest(String fileName) {
		File filePath = new File(pathToDir+fileName);		
		PrintWriter fileWriter;
		try {
			fileWriter = new PrintWriter(pathToDir+fileName);
			fileWriter.println("data changed");
			fileWriter.close();
		} catch (FileNotFoundException e) {
			out.print("manan");
		}
	}

	/**
	 * main method used to create a Server with port Number 5555.
	 * @param args args.
	 * @throws IOException Input-Output Exception.
	 */
	public static void main(String args[]) throws IOException {
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String serverInput = br.readLine();
		String[] commands = serverInput.split(" ");
		for(int i = 0; i<commands.length; i++) {
			if(commands[i].equals("-p")){
				isPort = true;
				port = Integer.parseInt(commands[++i]);
				System.out.println(port);
			}else {
				port = 8080;
			}
			if(commands[i].equals("-d")) {
				isPathToDir = true;
				pathToDir = commands[++i];
				System.out.println(pathToDir);
			}else {
				pathToDir = "C:/Users/Manan/Desktop/demo";
			}
			if(commands[i].equals("-v")) {
				isVerbose = true;
			}
		}
		httpfs httpf = new httpfs(port);
	}
}
