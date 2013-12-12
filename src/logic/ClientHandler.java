package logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles a client communications with others clients through sockets. The handler provides the interface for 
 * uploading files to other clients. The handler is also responsible for communicating with the server's ClientFailureDetector
 *
 * @see Client
 * @see ClientServer
 * @see ClientReader
 * @see ClientFailureDetector
 */
public class ClientHandler extends Thread {

	/** The socket used to communicate with the client */
	Socket socket;
	/** Used to read incoming messages from the client */
	BufferedReader rd;
	/** Used to send messages to the client */
	PrintWriter wr;
	/** The client the handler is started on */
	Client client;

	public ClientHandler(Socket socket, Client client) throws IOException {
		this.socket = socket;
		this.client = client;

		rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		wr = new PrintWriter(socket.getOutputStream());
		
	}

	/**
	 * Read incoming messages from the client, parses them and call the appropriate methods to 
	 * reply and/or execute actions.
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		String str;		
		try {
			while((str = rd.readLine()) != null){
				if(str.startsWith("download")){
					String fileNameAndType = str.split(":")[1];
					String path = client.findPathToFile(fileNameAndType);
					String sharedFolder = client.getSharedFilePath();

					File file = new File(sharedFolder + path + fileNameAndType.split("&")[0]);

					try (FileInputStream fis = new FileInputStream(file)) {
						String reply = "download:" + fileNameAndType.split("&")[0] + ":" + fis.available();
						wr.println(reply);
						wr.flush();
						int content;
						while ((content = fis.read()) != -1) {
							wr.print((char)content);
						}
						wr.flush();
						break;

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if(str.equals("ping")){
					wr.println("pong");
					wr.flush();
					break;
				}
				
				// P2P part
				
				if(str.startsWith("notify")){
					String clientName = str.split(":")[1];
					String clientAddress = socket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
					String clientPort = str.split(":")[2];
					
					((ClientP2P) client).notifyPredecessor(clientName, clientAddress, clientPort);
					((ClientP2P) client).setPredecessor(clientName, clientAddress, clientPort);
					((ClientP2P) client).finalize();
				}
				
				if(str.startsWith("successor")){
					String clientName = str.split(":")[1];
					String clientAddress = str.split(":")[2];
					String clientPort = str.split(":")[3];
					((ClientP2P) client).connectTo(clientName, clientAddress, clientPort);
					((ClientP2P) client).setSuccessorName(clientName);
				}
				
				if(str.startsWith("predecessor")){
					String clientName = str.split(":")[1];
					String clientAddress = str.split(":")[2];
					String clientPort = str.split(":")[3];
					((ClientP2P) client).setPredecessor(clientName, clientAddress, clientPort);
					((ClientP2P) client).displayNeighbours(client.getName());
				}
				
				if(str.startsWith("displayneighbours")){
					String sendersName = str.split(":")[1];
					if(! sendersName.equals(client.getName())){					
						((ClientP2P) client).displayNeighbours(sendersName);
					}
				}
				
				if(str.startsWith("connected")){
					String clientName = str.split(":")[1];
					((ClientP2P) client).setSuccessorName(clientName);
				}
				if(str.startsWith("quit")){
					break;
				}
				
			}
			
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		} 

	}

}
