import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class MainClient {
	
	private static Scanner sc;
	
	static URI uri;
	static String host;
	static String path;
	static String query;
	static String protocol;
	static int port;
	public static void main(String[] args) throws URISyntaxException, InterruptedException {
		
		while(true) {
		String url = null;	
		System.out.print(">>");
		ArrayList<String> listsym = new ArrayList<>();
		
		sc = new Scanner(System.in);
		String line = sc.nextLine();
		String linearray[] = line.split(" ");
		String httpCommand = linearray[0];
		String requestCommand = linearray[1];
		
		Client client = new Client(line);
		
		if(httpCommand.equals("httpc")) {
			
			for(int i = 0; i<linearray.length; i++) {
				
				if(linearray[i].equals("-v")) {
					listsym.add("-v");
				}
				if(linearray[i].startsWith("-h")) {
					listsym.add("-h,"+linearray[++i]);
				}
				if(linearray[i].startsWith("-d")) {
					listsym.add("-d,"+linearray[++i]);
				}
				if(linearray[i].startsWith("-o")) {
					listsym.add("-o,"+linearray[++i]);
				}
				if(linearray[i].startsWith("-f")) {
					listsym.add("-f,"+linearray[++i]);
				}
				if(linearray[i].startsWith("http://")) {
					url = linearray[i];
				}
			}
			
			if(url!=null) {
				
				uri = new URI(url);
				host = uri.getHost();
				path = uri.getRawPath();
				query = uri.getRawQuery();
				protocol = uri.getScheme();
				port = uri.getPort();
				
				if(path == null || path.length() == 0) {
					path = "";
				}
				if(query == null || query.length() ==0) {
					query = "";
				}
				if(query.length() > 0 || path.length() > 0) {
					path = path+"?"+query;	
				}
				
				if(port == -1) {
					if(protocol.equals("http")) {
						port = 80;
					}
					if(protocol.equals("https")) {
						port = 443;
					}
					
				}
			
			}
//			String helpline = requestCommand+url;
			if( (requestCommand.equals("get")) || (requestCommand.equals("post")) || (requestCommand.equals("help")) ){
				
				if(requestCommand.equals("get")) {
					try {
						client.get(host, port, path, url, listsym);
					}catch(Exception e) {
						System.out.println("Invalid Host");
					}
				}
				
				if(requestCommand.equals("post")) {
					try {
						client.post(host, port, path, url, listsym);
					} catch (UnknownHostException e) {
						System.out.println("Unknown Host");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if(requestCommand.equals("help")) {
					
					if(linearray.length==3) {
						String helpNextCommand = linearray[2];
						if(helpNextCommand.equals("get")) {
							client.help("helpget");
						}
						else if(helpNextCommand.equals("post")) {
							client.help("helppost");
						}
						else {
							System.out.println("command is not valid");
							System.out.println("");
							client.help("help");
						}
					}else {
						client.help("help");
					}
				}
			
			}else {
				System.out.println(requestCommand+" command is not valid.");
			}
			
		}else {
			System.out.println(httpCommand+" is not a valid command.");
			
		}
		
		//reference : http://www.drdobbs.com/jvm/making-http-requests-from-java/240160966

	}	

	}
}
