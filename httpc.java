import java.net.URISyntaxException;


/**
 * @author Manan 
 * @author Khyati
 * @version beta
 */
public class httpc {

	public static void main(String args[]) throws URISyntaxException {

			System.out.println("");
			Client client = new Client(args);
			client.parseUserInput();

	}
}
