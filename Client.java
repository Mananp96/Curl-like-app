import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * @author Manan 
 * @author Khyati
 * @version beta
 */
public class Client {
	public static final int HTTP_OK = 200;
	public static final int HTTP_MULT_CHOICE = 300;
	public static final int HTTP_MOVED_PERM = 301;
	public static final int HTTP_MOVED_TEMP = 302;
	public static final int HTTP_SEE_OTHER = 303;
	public static final int HTTP_NOT_MODIFIED = 304;
	public static final int HTTP_USE_PROXY = 305;

	String userInput;
	String requestCommand;
	String inlineData = null;
	String fileToRead;
	String fileData = new String();
	String fileToWrite;
	String url;
	String host;
	String path;
	String query;
	String protocol;
	String Location;

	int port;
	int statusCode;

	URI uri;
	Socket socket;
	PrintWriter request;
	BufferedWriter wr;
	
	boolean verbose = false;			
	boolean headerOption = false;
	boolean inlineDataOption = false;
	boolean sendfileOption = false;
	boolean writeToFileOption = false;
	boolean isRedirect = false;

	ArrayList<String> headerslist = new ArrayList<>();
	String[] inputArray;

	public Client(String[] userInput) {
		this.inputArray = userInput;
	}

	//Take the UserInput from console and parsing it according to request/option/URL.
	public void parseUserInput() throws URISyntaxException {

		if(inputArray != null) {
			
				requestCommand = inputArray[0];
				if(inputArray[0].equals("get") || inputArray[0].equals("post")) {

					for(int i = 0; i< inputArray.length ; i++) {
						if(inputArray[i].equals("-v")) {
							verbose = true;
						}
						if(inputArray[i].equals("-h")) {
							headerOption = true;
							headerslist.add(inputArray[++i]);
						}
						if(inputArray[i].equals("-d") || inputArray[i].equals("--d")) {
							inlineDataOption = true;
							inlineData = inputArray[++i];
						}
						if(inputArray[i].equals("-f")) {
							sendfileOption = true;
							fileToRead = inputArray[++i];
						}
						if(inputArray[i].equals("-o")) {
							writeToFileOption = true;
							fileToWrite = inputArray[++i];
						}
						if(inputArray[i].startsWith("http://") || inputArray[i].startsWith("https://")) {
							url = inputArray[i];
						}
					}

					if(url != null) {
						this.getUrlData(url);
						if(!(sendfileOption && inlineDataOption) ) {
							if(requestCommand.equals("get")) {
								
								if(!(sendfileOption || inlineDataOption)) {
									try {
										this.getRequest();
									} catch (UnknownHostException e) {
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}else {
									System.out.println("-f or -d are not allowed in GET Request");
								}
								
							}
							else if(requestCommand.equals("post")) {
								try {
									this.postRequest();
								} catch (UnknownHostException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}else {
							System.out.println("Command is not Valid: -f and -d both are not allowed.");
						}
					}else {
						System.out.println("please enter correct URL");
					}

				}
				else if(requestCommand.equals("help")) {
					this.help();
				}else {
					System.out.println("this command is not valid.");
				}
			
		}else {
			System.out.println("please enter proper command.");
		}

	}
	
	//this method is used to extract details from URL.
	public void getUrlData(String url2) throws URISyntaxException {

		uri = new URI(url2);
		host = uri.getHost();
		path = uri.getRawPath();
		query = uri.getRawQuery();
		protocol = uri.getScheme();
		port = uri.getPort();

		if (path == null || path.length() == 0) {
			path = "";
		}
		if (query == null || query.length() == 0) {
			query = "";
		}
		if (query.length() > 0 || path.length() > 0) {
			path = path + "?" + query;
		}

		if (port == -1) {
			if (protocol.equals("http")) {
				port = 80;
			}
			if (protocol.equals("https")) {
				port = 443;
			}
		}
	}
	
	//this method is used to send GET Request to Server.
	public void getRequest() throws UnknownHostException, IOException {

		socket = new Socket(host, port);
		request = new PrintWriter(socket.getOutputStream());

		if (path.length() == 0) {
			request.println("GET / HTTP/1.1");
		} else {
			request.println("GET " + path + " HTTP/1.1");
		}
		request.println("Host:" + host);

		//to send headers
		if (!headerslist.isEmpty()) {
			for (int i = 0; i < headerslist.size(); i++) {
				if (headerOption) {
					String[] headerKeyValue = headerslist.get(i).split(":");
					request.println(headerKeyValue[0] + ":" + headerKeyValue[1]);
				}
			}
		}
		request.println("");
		request.flush();
		this.printToConsole();
		request.close();
		this.checkForRedirection("getRedirect");
	}
	
	//this method is used to send POST Request to Server.
	public void postRequest() throws UnknownHostException, IOException {
		 
		 socket = new Socket(host, port);
		wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
		
		if (path.length() == 0) {
			wr.write("POST / HTTP/1.1\r\n");
		} else {
			wr.write("POST " + path + " HTTP/1.1\r\n");
		}
		wr.write("Host:" + host + "\r\n");

		if(headerOption) {
			if (!headerslist.isEmpty()) {
				for (int i = 0; i < headerslist.size(); i++) {
					String[] headerKeyValue = headerslist.get(i).split(":");
					wr.write(headerKeyValue[0] + ":" + headerKeyValue[1]+"\r\n");
				}
			}
		}
		if(inlineDataOption) {
			//wr.write("Content-Type: application/json\r\n");
			wr.write("Content-Length:" + inlineData.length() + "\r\n");
		}
		else if(sendfileOption) {
			FileReader fr = new FileReader(fileToRead);
			BufferedReader brreader = new BufferedReader(fr);
			String sCurrentLine;
			while ((sCurrentLine = brreader.readLine()) != null) {
				fileData = fileData + sCurrentLine;
			}
			wr.write("Content-Length:" + fileData.length() + "\r\n");
		}

		wr.write("\r\n");
		if (inlineData != null) {
			inlineData = inlineData.replace("\'", "");
			wr.write(inlineData);
		}
		if (fileData != null) {
			wr.write(fileData);
		}
		wr.flush();
		this.printToConsole();

		wr.close();
		this.checkForRedirection("postRedirect");

	}
	
	//This method is used to get the RESPONSE from Server and PRINT it to Console.
	public void printToConsole() throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String outputmsg;
		int count = 0;
		StringBuilder content = new StringBuilder();
		
		do {
			outputmsg = br.readLine();
			if (outputmsg.isEmpty()) {
				count++;
				if (count == 1) {
					content.append("|");
				}
			}
			content.append(outputmsg);
			content.append("^");

		} while ((outputmsg != null) && !(outputmsg.endsWith("}") || outputmsg.endsWith("</html>")
				|| outputmsg.endsWith("/get") || outputmsg.endsWith("post")));
		
		br.close();

		// System.out.println(content);
		String[] contentdevide = content.toString().split("\\|");
		String[] headers = contentdevide[0].split("\\^");
		String[] messagebody = contentdevide[1].split("\\^");

		// code for redirect
		statusCode = Integer.parseInt(headers[0].substring(9, 12));
		for (int k = 0; k < headers.length; k++) {

			if (headers[k].startsWith("Location:")) {
				Location = headers[k].substring(10);
			}
		}
		// end of code for redirect

		// For Verbose
		if (verbose) {
			// headers
			for (int i = 0; i < headers.length; i++) {
				System.out.println(headers[i]);

			}
			System.out.println("");
			for (int m = 0; m < messagebody.length; m++) {
				System.out.println(messagebody[m]);
			}
		}else {
			for (int m = 0; m < messagebody.length; m++) {
				System.out.println(messagebody[m]);
			}
		}

		if(writeToFileOption) {
			this.writeToFile(headers,messagebody);
		}
	}
	
	//this method is used to write the RESPONSE from Server to the FILE.
	public void writeToFile(String[] headers, String[] messagebody) {
		PrintWriter writer;
		if (fileToWrite != null) {
			try {
				writer = new PrintWriter(fileToWrite, "UTF-8");
				writer.println("Command: " + userInput + "\r\n");

				if (verbose) {
					for (int i = 0; i < headers.length; i++) {
						writer.println(headers[i]);
					}
				}
				writer.println("");
				for (int k = 0; k < messagebody.length; k++) {
					writer.println(messagebody[k]);
				}
				writer.close();
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}
	
	//this method is used to check if the Server RESPONDS with 301 code? if yes, then it will process REDIRECTION.
   //	301 Moved Permanently
	private void checkForRedirection(String requestRedirect) {
		if (statusCode != HTTP_OK) {
			if (statusCode == HTTP_MOVED_TEMP || statusCode == HTTP_MOVED_PERM
					|| statusCode == HTTP_SEE_OTHER)
				isRedirect = true;
		}

		if (isRedirect) {
			try {
				isRedirect = false;
				System.out.println("");
				Thread.sleep(1000);
				System.out.println("Status Code :"+statusCode);
				Thread.sleep(1000);
				System.out.print("Connecting to:"+Location);
				for(int k = 0; k<4 ; k++) {
					Thread.sleep(500);
					System.out.print(".");
				}
				System.out.println("");
				System.out.println("");

				this.getUrlData(Location);
				if(requestRedirect.equals("getRedirect")) {
					this.getRequest();
				}
				else if(requestRedirect.equals("postRedirect")) {
					this.postRequest();
				}
				System.out.println("Redirect Done...");

			} catch (URISyntaxException e) {

				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	//this method shows the details of HTTPC command.
	public void help() {

		System.out.println("httpc is a curl like application but supports HTTP protocol only.\r\n" + "Usage:\r\n"
				+ "    httpc command [arguments] \r\n" + "The commands are:\n"
				+ "    get 	executes a HTTP GET request and prints the response.\r\n"
				+ "    post	executes a HTTP POST request and prints the response.\r\n"
				+ "    help	prints this screen.\r\n"
				+ "Use \"httpc help [command]\" for more information about a command.");
	}
}