import java.io.BufferedReader;
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
	boolean isHttpcClient = false;
	boolean isHttpfsClient = false;
	int count = 0;
	static int port;
	private static String pathToDir;
	String crlf = "\r\n";
	String clientRequest; // client input
	String httpcRequest;
	
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private BufferedReader in = null; // input stream to get request from Client
	private PrintWriter out = null; // output stream send response to client
	private String request;
	httpfsModel httpfsModel;

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
			httpfsModel = new httpfsModel();
			serverSocket = new ServerSocket(port);
			System.out.println("Server started at port:"+port+"...");
			System.out.println("Waiting for connection...");

			socket = serverSocket.accept();
			System.out.println("Client "+socket.getInetAddress()+" connection established...");

			//get the data from client
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//output stream
			out = new PrintWriter(socket.getOutputStream());
			while((request = in.readLine())!=null) {
				
				if(request.endsWith("HTTP/1.1")) {
					httpcRequest = request;
					isHttpcClient = true;
				}
				else if(request.matches("(GET|POST)/(.*)")) {
						isHttpfsClient = true;
						clientRequest = request;
						System.out.println("----");
						break;
				}		
			
				if(isHttpcClient) {
					System.out.println(request);
					if(request.matches("(.*):(.*)")&&count==0){
						String[] headers = request.split(":");
						httpfsModel.addHeaders(headers[0], headers[1]);
					}
				}
				if(request.isEmpty())
					count++;
				if(count==1) {
					String data = request;
					httpfsModel.setContent(data);
				}
				if(count==2)
					break;
			}
			
			if(isHttpcClient) {
				if(httpcRequest.matches("(GET|POST) /(.*)")) {
					System.out.println("====");
					this.httpcRequest();
				}
			}
			
			System.out.println("-=----------");
			if(isHttpfsClient) {
				System.out.println("Client requested command..."+clientRequest);
	
				if(clientRequest.startsWith("GET")) {
					this.getServerRequest(clientRequest.substring(4));
				}else if(clientRequest.startsWith("POST")) {
					String[] command = clientRequest.substring(5).split(" ");
					String fileName = command[0];
					String content = new String();
					for(int i=1;i<command.length;i++) {
						content = content+" "+ command[i];
					}
					this.postServerRequest(fileName, content);
				}
			}
			out.println("");
			out.flush();
			in.close();
			socket.close();
			

		}catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * handles httpc Client.
	 */
	public void httpcRequest() {
		httpcRequest = httpcRequest.replace("GET /", "").replace("POST /", "").replace("HTTP/1.1", "");
		httpfsModel.setStatus("200");
		httpfsModel.setUrl("http://localhost:"+port+"/"+httpcRequest);
		out.println(httpfsModel.getHeaderPart());
		
		if(httpcRequest.startsWith("get?")) {
			System.out.println("httpc GET request...");
			//args
			httpcRequest = httpcRequest.replace("get?", "");
			if(httpcRequest.matches("(.*)&(.*)")) {
				String[] temp = httpcRequest.split("&");
				for(int i = 0;i<temp.length;i++) {
					String[] args = temp[i].split("=");
					httpfsModel.setArgs(args[0], args[1]);
				}
			}else {
				String[] args = httpcRequest.split("=");
				httpfsModel.setArgs(args[0], args[1]);
			}
			System.out.println(httpfsModel.getGETBodyPart());
			out.println(httpfsModel.getGETBodyPart());
			
		}else if(httpcRequest.startsWith("post?")) {
			System.out.println("httpc POST request...");
			httpcRequest = httpcRequest.replace("post?", "");
			if(!httpcRequest.isEmpty() && httpcRequest.matches("(.*)=(.*)")) {
				if(httpcRequest.matches("(.*)&(.*)")) {
					String[] temp = httpcRequest.split("&");
					for(int i = 0;i<temp.length;i++) {
						String[] args = temp[i].split("=");
						httpfsModel.setArgs(args[0], args[1]);
					}
				}else {
					String[] args = httpcRequest.split("=");
					httpfsModel.setArgs(args[0], args[1]);
				}
			}
			out.println(httpfsModel.getPOSTBodyPart());
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

		File filePath = new File(pathToDir+"/"+fileName);
		if(filePath.exists()) {
			if(filePath.isDirectory()) {	
				File[] listOfFiles = filePath.listFiles();
				for(File file : listOfFiles) {
					if(file.isFile()) {
						System.out.println("File      >> "+file.getName());
						out.println("File      >> "+file.getName());
					}else if(file.isDirectory()) {
						System.out.println("Directory >> "+file.getName());
						out.println("Directory >> "+file.getName());
					}
				}
			}else if(filePath.isFile()) {
				System.out.println("path: "+pathToDir+"/"+fileName);
				FileReader fileReader;
				try {
					fileReader = new FileReader(filePath);
					BufferedReader bufferedReader = new BufferedReader(fileReader);
					String currentLine;
					String fileData = null;
					while ((currentLine = bufferedReader.readLine()) != null) {
						fileData = fileData + currentLine;
						out.println(currentLine);
					}
				} catch (FileNotFoundException e) {
					System.out.println("ERROR HTTP 404");
					out.println("ERROR HTTP 404 : File Not Found");
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
	 * @param content 
	 */
	public void postServerRequest(String fileName, String content) {
		File filePath = new File(pathToDir+fileName);		
		PrintWriter fileWriter;
		try {
			fileWriter = new PrintWriter(pathToDir+fileName);
			fileWriter.println(content);
			fileWriter.close();
		} catch (FileNotFoundException e) {
			out.print("ERROR 404");
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