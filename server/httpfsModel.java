import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class httpfsModel {

	int count = 0;
	HashMap<String, String> headers;
	HashMap<String, String> args;
	ArrayList<String> fileList;
	String status;
	String content;
	String origin = "127.0.0.1"; 
	String Url;
	
	public httpfsModel(){
		this.headers = new HashMap<>();
		this.args = new HashMap<>();
		this.fileList = new ArrayList<>();
	}
	
	public void addHeaders(String key, String value) {
		headers.put(key, value);
	}
	
	public String getHeaders() {
		String head = "\r\n";
		for(Entry<String, String> entry : headers.entrySet()) {
			head += entry.getKey()+": "+entry.getValue()+"\r\n";
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
			head += entry.getKey()+": "+entry.getValue()+",\r\n";
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
		return "HTTP/1.0 " + this.getStatus() + " " + this.getState() + this.getHeaders() + "\r\n";
	}
	
	public String getGETBodyPart() {
		return 
				"{\r\n"+
				"\"args\":{\r\n"+
				this.getArgs()+"},\r\n"+
				"\"headers\":{\r\n"+
				this.getHeaders()+"},\r\n"+
				"\"origin\": "+this.getOrigin()+",\r\n"+
				"\"url\": "+this.getUrl()+",\r\n"+
				"}";
	}
	
	public String getPOSTBodyPart() {
		return 
				"{\r\n"+
				"\"args\":{\r\n"+
				this.getArgs()+"},\r\n"+
				"\"data\":{\r\n"+
				this.getContent()+"},\r\n"+
				"\"files\":{\r\n"+
				this.getFiles()+"},\r\n"+
				"\"headers\":{\r\n"+
				this.getHeaders()+"},\r\n"+
				"\"origin\": "+this.getOrigin()+",\r\n"+
				"\"url\": "+this.getUrl()+",\r\n"+
				"}";
	}
}
