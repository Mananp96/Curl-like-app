import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * @author Manan
 * @version 1.0
 */
public class Client extends HttpStatusCode {

	private Socket socket;
	int count = 0; // for seperate Header and Body part of Response, if 1 then seperate

	String HTTPresponse;
	String httpccommand;
	String httpInlineData = null;
	String fileData = new String();
	String Filename = null;
	int statusCode;
	String Location;
	boolean redirect = false;
	String[] headers;
	String[] messagebody;

	//	String headers;
	//	String messagebody;

	PrintWriter request;
	BufferedWriter wr;

	ArrayList<String> listsym = new ArrayList<>();

	public Client(String line) {
		this.httpccommand = line;
	}

	// httpc GET Request
	public void get(String host, int port, String path, String url, ArrayList<String> listsym1) {
		System.out.println("path" + path);
		try {
			socket = new Socket(host, port);
		} catch (Exception e) {
			System.out.println("Host is Unknown");
			e.printStackTrace();
		}

		try {
			request = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (path.length() == 0) {
			request.println("GET / HTTP/1.1");
		} else {
			request.println("GET " + path + " HTTP/1.1");
		}

		request.println("Host:" + host);
		listsym = listsym1;

		// for -h key:value
		if (!listsym.isEmpty()) {
			for (int i = 0; i < listsym.size(); i++) {
				if (listsym.get(i).startsWith("-h")) {
					String httpHeader = listsym.get(i).substring(3);
					String[] headerKeyValue = httpHeader.split(":");
					request.println(headerKeyValue[0] + ":" + headerKeyValue[1]);
				}
			}
		}

		request.println("");
		request.flush();

		try {
			Read(listsym);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		request.close();
		this.checkForRedirection();

	}

	// httpc POST Request
	public void post(String host, int port, String path, String url, ArrayList<String> listsym1)
			throws UnknownHostException, IOException {

		InetAddress address = InetAddress.getByName(host);
		SocketAddress socketaddress = new InetSocketAddress(address, port);
		socket = new Socket();
		// this method will block no more than timeout ms.
		int timeoutInMs = 10 * 1000; // 10 seconds
		socket.connect(socketaddress, timeoutInMs);

		// to encode parms
		//	    String params1 = URLEncoder.encode("param1", "UTF-8")
		//					+ ":" + URLEncoder.encode("value1", "UTF-8");
		//					            params += "&" + URLEncoder.encode("param2", "UTF-8")
		//					+ "=" + URLEncoder.encode("value2", "UTF-8");
		//	   
		wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
		listsym = listsym1;

		wr.write("POST " + path + " HTTP/1.1\r\n");
		wr.write("Host:" + host + "\r\n");
		// wr.write("Content-Length:"+ fileData.length() + "\r\n");
		// wr.write("Content-Type: application/json");
		// wr.write("Accept: application/txt");

		if (!listsym.isEmpty()) {
			for (int i = 0; i < listsym.size(); i++) {
				if (listsym.get(i).startsWith("-h")) {
					String httpHeader = listsym.get(i).substring(3);
					String[] headerKeyValue = httpHeader.split(":");
					wr.write(headerKeyValue[0] + ":" + headerKeyValue[1] + "\r\n");
				}

				if (listsym.get(i).startsWith("-d")) {
					String httpData = listsym.get(i).substring(3);
					wr.write("Content-Length:" + httpData.length() + "\r\n");
					httpInlineData = httpData;
				}

				if (listsym.get(i).startsWith("-f")) {
					String filepath = listsym.get(i).substring(3);
					FileReader fr = new FileReader(filepath);
					BufferedReader brreader = new BufferedReader(fr);
					String sCurrentLine;

					while ((sCurrentLine = brreader.readLine()) != null) {
						fileData = fileData + sCurrentLine;
					}

					wr.write("Content-Length:" + fileData.length() + "\r\n");
				}
			}
		}
		wr.write("\r\n");
		if (httpInlineData != null) {
			wr.write(httpInlineData);
		}
		if (fileData != null) {
			wr.write(fileData);
		}
		wr.flush();
		Read(listsym);
		//		this.checkForRedirection();
		wr.close();

	}

	// redirection condition check
	public void checkForRedirection() {
		// code for redirect

		if (statusCode != HttpURLConnection.HTTP_OK) {
			if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP || statusCode == HttpURLConnection.HTTP_MOVED_PERM
					|| statusCode == HttpURLConnection.HTTP_SEE_OTHER)
				redirect = true;
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (redirect) {
			try {
				this.redirect(Location);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		// end of redirect
	}

	// Redirect
	public void redirect(String newUrl) throws UnknownHostException, IOException {
		String newURL = newUrl;
		System.out.println("");
		System.out.println("Response Code ... " + statusCode);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		System.out.println("Redirecting to ...." + newURL);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("");

		URI uri = null;
		try {
			uri = new URI(newURL);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		String Host = uri.getHost();
		String Path = uri.getRawPath();
		String query = uri.getRawQuery();
		String protocol = uri.getScheme();
		int Port = uri.getPort();
		System.out.println("--" + Host);
		System.out.println("--" + Port);

		if (Path == null || Path.length() == 0) {
			Path = "";
		}
		if (query == null || query.length() == 0) {
			query = "";
		}
		if (query.length() > 0 || Path.length() > 0) {
			Path = Path + "?" + query;
		}

		if (Port == -1) {
			if (protocol.equals("http")) {
				Port = 80;
			}
			if (protocol.equals("https")) {
				Port = 443;
			}

		}

		socket = new Socket(Host, Port);
		request = new PrintWriter(socket.getOutputStream());

		if (Path.length() == 0) {
			request.println("GET / HTTP/1.1");
		} else {
			request.println("GET " + Path + " HTTP/1.1");
		}
		request.println("Host:" + Host);
		// request.println("Content-Type: application");
		request.println("");
		request.flush();
		this.Read(listsym);
		request.close();
		System.out.println("Done");
	}

	// Parse the Response from Server and Print the Final Output according to -v,
	// -h, --d, -f

	public void Read(ArrayList<String> listsym) throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String outputmsg;
		StringBuilder content = new StringBuilder();
		long start = System.currentTimeMillis();
		long end = start + 10 * 1000;
		do {
			outputmsg = br.readLine();
			// System.out.println(outputmsg);
			if (outputmsg.isEmpty()) {
				count++;
				if (count == 1) {
					content.append("|");
				}
			}
			content.append(outputmsg);
			content.append("^");

			// System.out.println(outputmsg);

		} while ((outputmsg != null) && !(outputmsg.endsWith("}") || outputmsg.endsWith("</html>")
				|| outputmsg.endsWith("/get") || outputmsg.endsWith("post")));

		br.close();

		// System.out.println(content);

		String[] contentdevide = content.toString().split("\\|");
		headers = contentdevide[0].split("\\^");
		messagebody = contentdevide[1].split("\\^");

		// code for redirect
		statusCode = Integer.parseInt(headers[0].substring(9, 12));
		for (int k = 0; k < headers.length; k++) {

			if (headers[k].startsWith("Location:")) {
				Location = headers[k].substring(10);
			}
		}
		// end of code for redirect

		// For Verbose
		if (listsym.contains("-v")) {
			// headers
			for (int i = 0; i < headers.length; i++) {
				System.out.println(headers[i]);

			}

		}
		System.out.println("");
		for (int m = 0; m < messagebody.length; m++) {
			System.out.println(messagebody[m]);
		}

		if (!listsym.isEmpty()) {
			for (int j = 0; j < listsym.size(); j++) {
				if (listsym.get(j).startsWith("-o")) {
					Filename = listsym.get(j).substring(3);
					this.writeToFile();
				}
			}
		}

	}
	//	
	//public void ReadV2(ArrayList<String> listsym) throws IOException {
	//		
	//		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	//		String outputmsg;
	//		StringBuilder content = new StringBuilder();
	//		long start = System.currentTimeMillis();
	//		long end = start + 10*1000;
	//		do {
	//			outputmsg = br.readLine();
	//			System.out.println(outputmsg);
	//			content.append(outputmsg);
	//			
	//		}while((outputmsg!=null)&&!(outputmsg.endsWith("}")||outputmsg.endsWith("</html>")||outputmsg.endsWith("/get")||outputmsg.endsWith("post")));
	//		
	//		br.close();
	//	
	//		//System.out.println(content);
	//		
	//		String[] contentdevide = content.toString().split("\\r\\n");
	//		System.out.println(contentdevide);
	//		headers = contentdevide[0];
	//		System.out.println("---"+headers);
	//		messagebody = contentdevide[1];
	//		
	//		
	//		//For Verbose
	//		if(listsym.contains("-v")) {	
	//			//headers	
	//			System.out.println(headers);
	//		}
	//		System.out.println("");
	//		System.out.println(messagebody);
	//		
	//		if(!listsym.isEmpty()) {
	//			for(int j=0; j<listsym.size();j++) {
	//				if(listsym.get(j).startsWith("-o")) {
	//					Filename = listsym.get(j).substring(3);
	//				//	this.writeToFile();
	//				}
	//			}
	//		}
	//		
	//		
	//		
	//	}

	// Write to file
	public void writeToFile() {

		PrintWriter writer;
		if (Filename != null) {
			try {

				writer = new PrintWriter(Filename, "UTF-8");
				writer.println("Command: " + httpccommand + "\r\n");
				if (listsym.contains("-v")) {
					// headers
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// httpc help, httpc help get, httpc help post command
	public void help(String helpcommand) {

		switch (helpcommand) {

		case "help":
			System.out.println("httpc is a curl like application but supports HTTP protocol only.\r\n" + "Usage:\r\n"
					+ "    httpc command [arguments] \r\n" + "The commands are:\n"
					+ "    get 	executes a HTTP GET request and prints the response.\r\n"
					+ "    post	executes a HTTP POST request and prints the response.\r\n"
					+ "    help	prints this screen.\r\n"
					+ "Use \"httpc help [command]\" for more information about a command.");
			break;

		case "helpget":
			System.out.println("Usage:\r\n" + "   httpc get [-v] [-h key:value] URL \r\n"
					+ "Get executes a HTTP GET request for a given URL.\r\n"
					+ "   -v            prints the detail of the response such as protocol,status and headers.\r\n"
					+ "   -h key:value  Associates headers to HTTP Request with the format \'key:value\' .");
			break;

		case "helppost":
			System.out.println("Usage:\r\n" + "   httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL \r\n"
					+ "Post executes a HTTP POST request for a given URL with inline data or from file.\r\n"
					+ "   -v            prints the detail of the response such as protocol,status and headers.\r\n"
					+ "   -h key:value  Associates headers to HTTP Request with the format \'key:value\'.\r\n"
					+ "   -d string     Associates an  inline data to the body HTTP POST request.\r\n"
					+ "   -f file       Associates the content of a file to the body HTTP POST request.");
			break;

		default:
			System.out.println(helpcommand + "is not valid");
			break;
		}

	}
}
