import static java.nio.channels.SelectionKey.OP_READ;

import java.awt.SecondaryLoop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
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
 * Client class
 * @author Manan
 *
 */
public class HttpfsClient {

	static boolean isHeader = false;
	static boolean isContent = false;
	static String content;
	static DatagramChannel channel;
	private PrintWriter out;
	static String URL;
	int port;
	static String query;
	static ArrayList<String> headers = new ArrayList<>();
	static SocketAddress routerAddr = new InetSocketAddress("localhost", 3000);
	static int seqNum = 1001;
	private static final Logger logger = LoggerFactory.getLogger(HttpfsClient.class);
	static InetSocketAddress serverAddr;
	static String payload;
	private StringBuilder clientRequest;
	public static final String ACK = "ACK";
	static Packet p;
	static Map<Integer, String> packetInfo;

	public String getPacketInfo(int packetNumber) {
		return packetInfo.get(packetNumber);
	}

	public void setPacketInfo(Map<Integer, String> packetInfo) {
		this.packetInfo = packetInfo;
	}

	public static void setPacketACK(long sequenceNumber) {
		packetInfo.put((int) sequenceNumber, ACK);
	}
	
	/**
	 * Constructor 
	 * @param serverAddr2 Server address
	 * @param query2 client request
	 * @param content2 request Content
	 * @param headers2 Client headers
	 * @throws IOException IOException
	 */
	public HttpfsClient(InetSocketAddress serverAddr2, String query2,String content2, ArrayList<String> headers2) throws IOException {

		this.serverAddr = serverAddr2;
		this.content = content2;
		this.query = query2;
		this.headers = headers2;
		channel = DatagramChannel.open();
		packetInfo = new HashMap<Integer, String>();
		this.initialHandshake();
	}	

	/**
	 * used to do initial three-way handshake to server.
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
			this.sendRequest();
		}else {

			logger.info("Sending \"{}\" again to router at {}", "handshake message", routerAddr);
			this.initialHandshake();
		}
	}
	
	/**
	 * receive packet from server.
	 * @param channel DatagramChannel
	 * @param routerAddr Router Address
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
	 * Send data to server
	 * @throws IOException IOException
	 */
	public void sendRequest() throws IOException {
		clientRequest = new StringBuilder();
		clientRequest.append(query+"\n");
		if(isHeader) {
			for(int i = 0 ; i<headers.size();i++) {
				clientRequest.append(headers.get(i)+"\n");
			}
		}
		if(isContent) {
			clientRequest.append("-d"+content+"\n");
		}
		clientRequest.append("\r\n");
		p = new Packet.Builder()
				.setType(Packet.DATA_TYPE)
				.setSequenceNumber(seqNum)
				.setPortNumber(serverAddr.getPort())
				.setPeerAddress(serverAddr.getAddress())
				.setPayload(clientRequest.toString().trim().getBytes())
				.create();
		channel.send(p.toBuffer(), routerAddr);
		packetInfo.put(seqNum, "");
		logger.info("Sending \"{}\" to router at {}", clientRequest, routerAddr);

		receive(channel, routerAddr);
		//last packet from client to close the connection
		if(getPacketInfo(seqNum).equals(ACK)) {
			seqNum++;
			p = new Packet.Builder()
					.setType(Packet.DATA_TYPE)
					.setSequenceNumber(seqNum)
					.setPortNumber(serverAddr.getPort())
					.setPeerAddress(serverAddr.getAddress())
					.setPayload("ACK".getBytes())
					.create();
			channel.send(p.toBuffer(), routerAddr);
			packetInfo.put(seqNum, "");
		}else {
			this.sendRequest();
		}

	}

	/**
	 * main method
	 * @param args arguments
	 * @throws IOException IOException
	 * @throws URISyntaxException URISyntaxException
	 */
	public static void main(String args[]) throws IOException, URISyntaxException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String httpfsClient = br.readLine();

		String[] commandClient = httpfsClient.split(" ");
		if(commandClient[0].equals("httpfs")) {
			for(int i =0; i<commandClient.length; i++) {
				if(commandClient[i].equals("-h")) {
					isHeader = true;
					headers.add(commandClient[++i]);
				}
				if(commandClient[i].startsWith("http://")){
					URL = commandClient[i];
				}
				if(commandClient[i].startsWith("-d")) {
					isContent = true;
					content = commandClient[++i];
				}
			}
		}
		URI uri = new URI(URL);
		String host = uri.getHost();
		int port = uri.getPort();
		query = uri.getPath();
		InetSocketAddress serverAddress = new InetSocketAddress(host, port);
		HttpfsClient client2 = new HttpfsClient(serverAddress,query.substring(1),content, headers);
		System.out.println(packetInfo);
	}
}
