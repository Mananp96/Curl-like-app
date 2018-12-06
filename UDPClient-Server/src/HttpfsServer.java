import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Server class.
 *
 */
class httpfsServerThread extends Thread{

	boolean isContentType = false;
	boolean isDisposition = false;
	boolean isServerRunning = true;
	static boolean isPort = false;
	static boolean isVerbose = false;
	static boolean isPathToDir = false;
	boolean isHttpcClient = false;
	boolean isHttpfsClient = false;

	int count = 0;
	int port;
	String pathToDir;
	String crlf = "\r\n";
	String clientRequest; // client input
	String httpcRequest;
	String content;
	StringBuilder httpfsResponse = new StringBuilder();

	DatagramChannel channel;
	Packet packet;
	private BufferedReader in = null; // input stream to get request from Client
	private PrintWriter out = null; // output stream send response to client
	private String request;
	httpfsModel httpfsModelObject;
	int clientNumber;
	private static final Logger logger = LoggerFactory.getLogger(HttpfsServer.class);
	SocketAddress router;
	ByteBuffer buf;
	Map<Long, Integer> packetInfo = new HashMap<>();

	public Map<Long, Integer> getPacketInfo() {
		return packetInfo;
	}


	public void setPacketInfo(Long packetNumber,int packetACK) {
		packetInfo.put(packetNumber, packetACK);
	}

	public httpfsServerThread(DatagramChannel channel, Packet packet, int counter, String pathToDir2, ByteBuffer buf,SocketAddress router) throws IOException {
		this.channel = channel;
		this.packet = packet;
		this.clientNumber = counter;
		this.pathToDir = pathToDir2;
		this.buf = buf;
		this.router = router;
	}

	public void run() {
		try
		{	
			httpfsModelObject = new httpfsModel();

			String payload = new String(packet.getPayload(), UTF_8);
			System.out.println("payload=="+payload);
			Reader inputString = new StringReader(payload);
			BufferedReader in = new BufferedReader(inputString);

			while((request = in.readLine())!=null) {

				if(request.endsWith("HTTP/1.1")) {
					httpcRequest = request;
					isHttpcClient = true;
				}
				else if(request.matches("(GET|POST)/(.*)")) {
					isHttpfsClient = true;
					clientRequest = request;
				}		

				if(isHttpfsClient) {
					httpfsModelObject.addhttpfsHeaders(request);
					if(request.startsWith("Content-type:"))
						isContentType = true;
					if(request.startsWith("Content-Disposition:")) {
						isDisposition = true;
					}
					if(request.startsWith("-d")) {
						content = request.substring(2);
					}
				}
				if(isHttpfsClient && request.isEmpty()) {
					break;
				}

				if(isHttpcClient) {
					if(request.matches("(.*):(.*)")&&count==0){
						String[] headers = request.split(":");
						httpfsModelObject.addHeaders(headers[0], headers[1]);
					}

					if(count==1) {
						String data = request;
						httpfsModelObject.setContent(data);
						break;
					}
					if(request.isEmpty())
						count++;
				}	
			}

			if(isHttpcClient) {
				if(httpcRequest.matches("(GET|POST) /(.*)")) {
					this.httpcRequest();
				}
			}

			if(isHttpfsClient) {
				System.out.println("Client requested command..."+clientRequest);

				if(clientRequest.startsWith("GET")) {
					this.getServerRequest(clientRequest.substring(4));
				}else if(clientRequest.startsWith("POST")) {
					System.out.println(clientRequest.substring(5));
					String fileName = clientRequest.substring(5);
					this.postServerRequest(fileName, content);
				}
			}

			httpfsResponse.append("\n");
			String[] clientResult;
			long packetNumber = packet.getSequenceNumber();

			//create packet of Response
			Packet p = packet.toBuilder()
					.setPayload(httpfsResponse.toString().getBytes())
					.create();
			channel.send(p.toBuffer(), router);
			logger.info("Sending \"{}\" to router at {}", httpfsResponse.toString(), 3000);

			//        	//send null data packet when result is completed.
			//			Packet p1 = packet.toBuilder()
			//					.setPayload("".getBytes())
			//					.create();
			//			channel.send(p1.toBuffer(), router);
			//			logger.info("Sending \"{}\" to router at {}", "empty message", 3000);

		}catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * split a Response by size
	 */
	public static String[] split(String src, int len) {
		String[] result = new String[(int)Math.ceil((double)src.length()/(double)len)];
		for (int i=0; i<result.length; i++)
			result[i] = src.substring(i*len, Math.min(src.length(), (i+1)*len));
		return result;
	}

	/**
	 * handles httpc Client.
	 */
	public synchronized void httpcRequest() {
		httpcRequest = httpcRequest.replace("GET /", "").replace("POST /", "").replace("HTTP/1.1", "");
		httpfsModelObject.setStatus("200");
		httpfsModelObject.setUrl("http://localhost:"+port+"/"+httpcRequest);
		//		out.println(httpfsModelObject.getHeaderPart());
		httpfsResponse.append(httpfsModelObject.getHeaderPart()+"\n");

		if(httpcRequest.startsWith("get?")) {
			System.out.println("httpc GET request...");
			//args
			httpcRequest = httpcRequest.replace("get?", "");
			if(httpcRequest.matches("(.*)&(.*)")) {
				String[] temp = httpcRequest.split("&");
				for(int i = 0;i<temp.length;i++) {
					String[] args = temp[i].split("=");
					httpfsModelObject.setArgs(args[0], args[1]);
				}
			}else {
				String[] args = httpcRequest.split("=");
				httpfsModelObject.setArgs(args[0], args[1]);
			}
			httpfsResponse.append(httpfsModelObject.getGETBodyPart()+"\n");

		}else if(httpcRequest.startsWith("post?")) {
			System.out.println("httpc POST request...");
			httpcRequest = httpcRequest.replace("post?", "");
			if(!httpcRequest.isEmpty() && httpcRequest.matches("(.*)=(.*)")) {
				if(httpcRequest.matches("(.*)&(.*)")) {
					String[] temp = httpcRequest.split("&");
					for(int i = 0;i<temp.length;i++) {
						String[] args = temp[i].split("=");
						httpfsModelObject.setArgs(args[0], args[1]);
					}
				}else {
					String[] args = httpcRequest.split("=");
					httpfsModelObject.setArgs(args[0], args[1]);
				}
			}
			httpfsResponse.append(httpfsModelObject.getPOSTBodyPart()+"\n");
		}

	}

	/**
	 * "GET /" returns a list of the current files in the data directory.<br>
	 * "GET /foo" returns the content of the file named foo in the data directory.<br>
	 * 
	 * @param fileNam name of file.
	 * 
	 */
	public synchronized void getServerRequest(String fileNam) {
		File filePath;
		String fileName = fileNam;
		if(isContentType) {
			fileName = fileName+httpfsModelObject.getHttpfContentHeader();
			filePath = new File(pathToDir+"/"+fileName);
		}else {
			filePath = new File(pathToDir+"/"+fileName);
		}

		if(!fileName.contains("/")) {


			if(filePath.exists()) {
				if(filePath.isDirectory()) {	
					File[] listOfFiles = filePath.listFiles();
					for(File file : listOfFiles) {
						if(file.isFile()) {
							System.out.println("File      >> "+file.getName());
							httpfsResponse.append("File      >> "+file.getName()+"\n");
						}else if(file.isDirectory()) {
							System.out.println("Directory >> "+file.getName());
							httpfsResponse.append("Directory >> "+file.getName()+"\n");
						}
					}
				}else if(filePath.isFile()) {
					System.out.println("path: "+pathToDir+"/"+fileName);
					FileReader fileReader;
					PrintWriter fileWriter = null;
					File downloadPath = new File(pathToDir+"/Download");
					String fileDownloadName = new String();
					boolean dispositionDirectory;
					if(isDisposition) {
						fileDownloadName = httpfsModelObject.getHttpfsDispositionHeader();
						if(httpfsModelObject.isDispositionAttachment) {
							if(!downloadPath.exists())
								dispositionDirectory = new File(pathToDir+"/Download").mkdir();
						}
					}

					try {

						if(httpfsModelObject.isDispositionAttachment) {
							if(httpfsModelObject.isDispositionWithFileName) 
								fileWriter = new PrintWriter(downloadPath+"/"+fileDownloadName);
							else
								fileWriter = new PrintWriter(downloadPath+"/"+fileName);
						}	
						fileReader = new FileReader(filePath);
						BufferedReader bufferedReader = new BufferedReader(fileReader);
						String currentLine;
						String fileData = null;
						while ((currentLine = bufferedReader.readLine()) != null) {
							fileData = fileData + currentLine;
							if(isDisposition) {

								if(httpfsModelObject.isDispositionInline) {
									httpfsResponse.append(currentLine+"\n");
								}else if(httpfsModelObject.isDispositionAttachment) {
									fileWriter.println(currentLine);
								}
							}else 
								httpfsResponse.append(currentLine+"\n");
						}
						if(httpfsModelObject.isDispositionAttachment)
							fileWriter.close();
						httpfsResponse.append("Operation Success \n");
					} catch (FileNotFoundException e) {
						System.out.println("ERROR HTTP 404");
						httpfsResponse.append("ERROR HTTP 404 : File Not Found\n");
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			} else {
				System.out.println("ERROR HTTP 404");
				httpfsResponse.append("ERROR HTTP 404\n");
			}
		}else {
			System.out.println("Access Denied");
			httpfsResponse.append("Error: access denied");
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
	public synchronized void postServerRequest(String fileName, String content) {
		//		try {
		//			Thread.sleep(10000);
		//		} catch (InterruptedException e1) {
		//			e1.printStackTrace();
		//		}
		File filePath;
		PrintWriter postWriter;
		if(isContentType) 
			filePath = new File(pathToDir+"/"+fileName+httpfsModelObject.getHttpfContentHeader());
		else
			filePath = new File(pathToDir+"/"+fileName);

		if(!fileName.contains("/")) {
			try {
				postWriter = new PrintWriter(filePath);
				postWriter.println(content);
				httpfsResponse.append("Operatiion Sucessful...");
				postWriter.close();
			} catch (FileNotFoundException e) {
				httpfsResponse.append("ERROR 404");
			}
		}else {
			System.out.println("Access Denied");
			httpfsResponse.append("Error: Access Denied");
		}
	}

}

/**
 * Server class.	
 * @author Manan
 *
 */
public class HttpfsServer{	

	static boolean isPort = false;
	static int port;
	static String pathToDir;
	private static boolean isVerbose = false;
	private static final Logger logger = LoggerFactory.getLogger(HttpfsServer.class);
	Packet packet;
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
			}
			if(commands[i].equals("-d")) {
				pathToDir = commands[++i];
			}else {
				pathToDir = "C:/Users/Manan/Desktop/demo";
			}
			if(commands[i].equals("-v")) {
				isVerbose  = true;
			}
		}
		if(!isPort) {
			port = 8080;
		}
		HttpfsServer server = new HttpfsServer();
		server.listenAndServe(port);
		int counter = 0;
	}
	private void listenAndServe(int port2) throws IOException {
		try (DatagramChannel channel = DatagramChannel.open()) {
			channel.bind(new InetSocketAddress(port));
			logger.info("EchoServer is listening at {}", channel.getLocalAddress());
			ByteBuffer buf = ByteBuffer
					.allocate(Packet.MAX_LEN)
					.order(ByteOrder.BIG_ENDIAN);
			int counter = 0;
			for (; ; ) {

				buf.clear();
				SocketAddress router = channel.receive(buf);

				// Parse a packet from the received raw data.
				buf.flip();
				packet = Packet.fromBuffer(buf);
				buf.flip();

				String payload = new String(packet.getPayload(), UTF_8);
				logger.info("Packet: {}", packet);
				logger.info("Payload: {}", payload);
				logger.info("Router: {}", router);

				// Send the response to the router not the client.
				// The peer address of the packet is the address of the client already.
				// We can use toBuilder to copy properties of the current packet.
				// This demonstrate how to create a new packet from an existing packet.
				long packetNumber = packet.getSequenceNumber();
				String ACK = "send packet from "+(++packetNumber);
				if(packet.getType() == Packet.CONNECTION_TYPE && !payload.equals("ACK")) {
					Packet resp = packet.toBuilder()
							.setPayload(ACK.getBytes())
							.create();
					channel.send(resp.toBuffer(), router);
					System.out.println(">> Client "+packet.getPeerPort()+" connection established");
					System.out.println(packet.getType());
					System.out.println(payload);

				}else if(packet.getType() == Packet.DATA_TYPE && !payload.equals("ACK")) {

					httpfsServerThread hst = new httpfsServerThread(channel,packet,counter,pathToDir,buf,router);
					hst.start();
				}
				else if(packet.getType() == Packet.DATA_TYPE && payload.equals("ACK")) {
					System.out.println("DONE");
				}
			}
		}
	}	
}

