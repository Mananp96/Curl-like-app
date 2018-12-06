import static java.nio.channels.SelectionKey.OP_READ;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client class handles GET and POST request, Prints the RESPONSE to console,
 * Write the REPONSE to file, REDIRECT the request if REPONSE STATUS CODE is 3xx;
 * 
 * @author Manan 
 * @author Khyati
 * @version beta
 */
public class httpcClient {
	
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
	
	int seqNum = 1;
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
	
	InetSocketAddress serverAddr;
	public static final String ACK = "ACK";
	static Packet p;
	static Map<Integer, String> packetInfo;
	static SocketAddress routerAddr = new InetSocketAddress("localhost", 3000);
	static DatagramChannel channel;
	private static final Logger logger = LoggerFactory.getLogger(httpcClient.class);
	static String payload;
	StringBuilder writerBuilder;
	
    public String getPacketInfo(int packetNumber) {
		return packetInfo.get(packetNumber);
	}

	public void setPacketInfo(Map<Integer, String> packetInfo) {
		this.packetInfo = packetInfo;
	}
	
	public static void setPacketACK(long sequenceNumber) {
		packetInfo.put((int) sequenceNumber, ACK);
	}

	ArrayList<String> headerslist = new ArrayList<>();

	public httpcClient(String userInput) {
		this.userInput = userInput;
		
	}
	
	/**
	 * Open the UDP Datagram Channel
	 */
	public void openChannel() {
		try {
			channel = DatagramChannel.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
		packetInfo = new HashMap<Integer, String>();
		try {
			this.initialHandshake();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * used to do three-way handshake to server.
	 * @param serverAddr 
	 * @throws IOException
	 */
	public void initialHandshake() throws IOException {
		p = new Packet.Builder()
				.setType(Packet.CONNECTION_TYPE)
				.setSequenceNumber(seqNum)
				.setPortNumber(serverAddr.getPort())
				.setPeerAddress(serverAddr.getAddress())
				.setPayload("handshake".getBytes())
				.create();
		channel.send(p.toBuffer(), routerAddr);
	    packetInfo.put(seqNum, "");
		
		logger.info("Sending \"{}\" to router at {}", "handshake message", routerAddr);
		receive(channel,routerAddr);
		
		if(getPacketInfo(seqNum).equals(ACK)) {
			seqNum++;
			p = new Packet.Builder()
					.setType(Packet.CONNECTION_TYPE)
					.setSequenceNumber(seqNum)
					.setPortNumber(serverAddr.getPort())
					.setPeerAddress(serverAddr.getAddress())
					.setPayload("ACK".getBytes())
					.create();
			channel.send(p.toBuffer(), routerAddr);
			packetInfo.put(seqNum, "");
			
			logger.info("Sending \"{}\" to router at {}", "Connection done", routerAddr);
			seqNum++;
		}else {
			
			logger.info("Sending \"{}\" again to router at {}", "handshake message", routerAddr);
			this.initialHandshake();
		}
	}
	
	/**
	 * Receive the packet from Server.
	 * @param channel DatagramChannel
	 * @param routerAddr RouterAddress
	 * @throws IOException IOException
	 */
	public static void receive(DatagramChannel channel,SocketAddress routerAddr) throws IOException{
		// Try to receive a packet within timeout.
		channel.configureBlocking(false);
		Selector selector = Selector.open();
		channel.register(selector, OP_READ);
		logger.info("Waiting for the response");
		selector.select(10000);

		Set<SelectionKey> keys = selector.selectedKeys();
		if(keys.isEmpty()){
			logger.error("No response after timeout");
			return;
		}
		
		while(true) {
		ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
		SocketAddress router = channel.receive(buf);
		buf.flip();
		if(buf.limit() == 0)
			break;
		Packet resp = Packet.fromBuffer(buf);
		logger.info("Packet: {}", resp);
		logger.info("Router: {}", router);
		payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
		logger.info("Payload: {}",  payload);
		setPacketACK(resp.getSequenceNumber());
		keys.clear();
		}
	}  

	
	/**
	 * Takes the UserInput and parse it according to request/option/URL.
	 * 
	 * @throws URISyntaxException
	 */
	public void parseUserInput() throws URISyntaxException {

		String[] inputArray = userInput.split(" ");
		if(inputArray != null) {
			if(inputArray[0].equals("httpc")) {
				requestCommand = inputArray[1];
				if(inputArray[1].equals("get") || inputArray[1].equals("post")) {

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
							if(requestCommand.equals("get")  ) {
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
				System.out.println("this command is not valid.");
			}
		}else {
			System.out.println("please enter proper command.");
		}

	}
	
	
	/**
	 * this method is used to extract details from URL.
	 * 
	 * @param url2 a HTTP URL.
	 * @throws URISyntaxException
	 */
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
		serverAddr = new InetSocketAddress(host, port);
		this.openChannel();
	}
	
	
	/**
	 * this method is used to send GET Request to Server.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void getRequest() throws UnknownHostException, IOException {
		writerBuilder = new  StringBuilder();
		if (path.length() == 0) {
			writerBuilder.append("GET / HTTP/1.1\n");
		} else {
			writerBuilder.append("GET " + path + " HTTP/1.1\n");
		}
		writerBuilder.append("Host:" + host+"\n");
		//to send headers
		if (!headerslist.isEmpty()) {
			for (int i = 0; i < headerslist.size(); i++) {
				if (headerOption) {
					String[] headerKeyValue = headerslist.get(i).split(":");
					writerBuilder.append(headerKeyValue[0] + ":" + headerKeyValue[1]+"\n");
				}
			}
		}
		writerBuilder.append("\r\n\n");
		this.flush();
		this.checkForRedirection("getRedirect");
	}
	
	/**
	 * send the data to server
	 */
	public void flush() {
		p = new Packet.Builder()
				.setType(Packet.DATA_TYPE)
				.setSequenceNumber(seqNum)
				.setPortNumber(serverAddr.getPort())
				.setPeerAddress(serverAddr.getAddress())
				.setPayload(writerBuilder.toString().trim().getBytes())
				.create();
		try {
			channel.send(p.toBuffer(), routerAddr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		packetInfo.put(seqNum, "");
		logger.info("Sending \"{}\" to router at {}", writerBuilder.toString(), routerAddr);
		
				
		System.out.println("request__"+writerBuilder.toString().trim());
		try {
			receive(channel,routerAddr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(getPacketInfo(seqNum).equals(ACK)) {
			seqNum++;
			p = new Packet.Builder()
					.setType(Packet.DATA_TYPE)
					.setSequenceNumber(seqNum)
					.setPortNumber(serverAddr.getPort())
					.setPeerAddress(serverAddr.getAddress())
					.setPayload("ACK".getBytes())
					.create();
			try {
				channel.send(p.toBuffer(), routerAddr);
			} catch (IOException e) {
				e.printStackTrace();
			}
			packetInfo.put(seqNum, "");
		}else {
			try {
				this.getRequest();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * this method is used to send POST Request to Server.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void postRequest() throws UnknownHostException, IOException {
		 
		 socket = new Socket(host, port);
		wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		
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
			wr.write("\r\n");
		}
		if (fileData != null) {
			wr.write(fileData);
			wr.write("\r\n");
		}
		wr.flush();
		this.printToConsole();
		wr.close();
		this.checkForRedirection("postRedirect");

	}
	
	/**
	 * This method is used to get the RESPONSE from Server and PRINT it to Console.
	 * 
	 * @throws IOException
	 */
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
	
	
	/**
	 * this method is used to write the RESPONSE from Server to the FILE.
	 * 
	 * @param headers headers part of REPONSE.
	 * @param messagebody body part of RESPONSE.
	 */
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
	
	
	/**
	 * this method is used to check if the Server RESPONDS with 301 code? if yes, then it will process REDIRECTION.
	 * REDIRECT SEPCIFICATION:301 Moved Permanently
	 * 
	 * @param requestRedirect a string contains redirect request from "getRequest" or "postRequest".
	 */
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
				e.printStackTrace();
			}
		}

	}

	
	/**
	 * this method shows the details of HTTPC command.
	 */
	public void help() {

		System.out.println("httpc is a curl like application but supports HTTP protocol only.\r\n" + "Usage:\r\n"
				+ "    httpc command [arguments] \r\n" + "The commands are:\n"
				+ "    get 	executes a HTTP GET request and prints the response.\r\n"
				+ "    post	executes a HTTP POST request and prints the response.\r\n"
				+ "    help	prints this screen.\r\n"
				+ "Use \"httpc help [command]\" for more information about a command.");
	}
}
