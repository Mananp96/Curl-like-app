import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class httpfsModel {


	boolean isDispositionInline = false;
	boolean isDispositionAttachment = false;
	boolean isDispositionWithFileName = false;
	int count = 0;
	HashMap<String, String> headers;
	HashMap<String, String> args;
	ArrayList<String> fileList;
	String status;
	String content = "";
	String origin = "127.0.0.1"; 
	String Url;
	String space = " ";
	ArrayList<String> httpfsHeaders;

	public httpfsModel(){
		this.httpfsHeaders = new ArrayList<>();
		this.headers = new HashMap<>();
		this.args = new HashMap<>();
		this.fileList = new ArrayList<>();
		headers.put("Connection", "keep-alive");
		headers.put("Host", "Localhost");
		Instant instant = Instant.now();
		headers.put("Date", instant.toString());
	}

	public void addhttpfsHeaders(String header) {
		httpfsHeaders.add(header);
	}

	public String getHttpfContentHeader() {	
		String extension = new String();
		for(int i = 0; i<httpfsHeaders.size(); i++) {
			if(httpfsHeaders.get(i).startsWith("Content-type:")) {
				String[] temp = httpfsHeaders.get(i).split(":");
				if(temp[1].equals("application/text"))
					extension = ".txt";
				if(temp[1].equals("application/json"))
					extension = ".json";
			}
		}
		return extension;
	}

	public String getHttpfsDispositionHeader() {
		String fileName = "";
		for(int i = 0; i<httpfsHeaders.size(); i++) {
			if(httpfsHeaders.get(i).startsWith("Content-Disposition:")) {
				String[] temp = httpfsHeaders.get(i).split(";");
				String[] temp2 = temp[0].split(":");
				if(temp2[1].equals("inline")) {
					isDispositionInline = true;
				}else if(temp2[1].equals("attachment")) {
					isDispositionAttachment = true;
					if(temp.length == 2) {
						String temp3[] = temp[1].split(":");
						fileName = temp3[1];
						isDispositionWithFileName = true;
					}
				}				
			}
		}
		return fileName;
	}

	public void addHeaders(String key, String value) {
		headers.put(key, value);
	}

	public String getHeaders() {
		String head = "";
		for(Entry<String, String> entry : headers.entrySet()) {
			head += " "+entry.getKey()+": "+entry.getValue()+"\r\n";
		}
		return head;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return this.status;
	}

	public String getState() {
		if(this.status == "200" ) 
			return "OK";
		else if(this.status == "400")
			return "Bad Request";
		else if(this.status == "404")
			return "Not Found";
		else 
			return "ERROR HTTP";
	}

	public void setArgs(String key, String value) {
		args.put(key, value);
	}

	public String getArgs() {
		String head = "\r\n";
		for(Entry<String, String> entry : args.entrySet()) {
			head += " \""+entry.getKey()+"\": \""+entry.getValue()+"\",\r\n";
		}
		return head;
	}

	public String getOrigin() {
		return origin;
	}

	public void setUrl(String Url) {
		this.Url = Url;
	}

	public String getUrl() {
		return Url;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	public void setFiles(String fileName) {
		fileList.add(fileName);
	}

	public String getFiles() {
		String listOfFiles = "";
		for(String file : fileList) {
			listOfFiles += file+",";
		}
		return listOfFiles;
	}

	public String getHeaderPart() {
		return "HTTP/1.0 " + this.getStatus() + " " + this.getState() +"\r\n"+ this.getHeaders();
	}

	public String getGETBodyPart() {
		return 
				"{\r\n"+
				" \"args\":{"+
				this.getArgs()+"},\r\n"+
				" \"headers\":{\r\n"+
				this.getHeaders()+"},\r\n"+
				" \"origin\": "+this.getOrigin()+",\r\n"+
				" \"url\": "+this.getUrl()+",\r\n"+
				"}";
	}

	public String getPOSTBodyPart() {
		return 
				"{\r\n"+space+
				"\"args\":{"+space+
				this.getArgs()+"},\r\n"+space+
				"\"data\":{"+space+
				this.getContent()+"},\r\n"+space+
				"\"files\":{\r\n"+space+
				this.getFiles()+"},\r\n"+space+
				"\"headers\":{\r\n"+
				this.getHeaders()+" },\r\n"+space+
				"\"json\": { },\r\n"+space+
				"\"origin\": "+this.getOrigin()+",\r\n"+space+
				"\"url\": "+this.getUrl()+",\r\n"+
				"}";
	}
}
