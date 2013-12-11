package logic;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server-side of the client, responsible for providing the uploading interface for other clients.
 * Each new connection is handled by a ClientHandler.
 * 
 * @param client The client on which this thread is started
 * @param downloadPort The port which support the server-side client's communications
 * @see Client
 * @see ClientHandler
 *
 */
public class ClientServer implements Runnable {
	
	/** The client on which this thread is started */
	Client client;
	/** The socket used for server-side client socket communications */
	ServerSocket serverSocket;

	/**
	 * @param client The client on which this thread is started
	 * @param downloadPort The port which support the server-side client's communications
	 */
	public ClientServer(Client client, String downloadPort) {
		try {
			this.serverSocket = new ServerSocket(Integer.parseInt(downloadPort));
			this.client = client;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * New connections are handled by new ClientHandler instances.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (! serverSocket.isClosed()) {
			Socket socket;
			try {
				socket = this.serverSocket.accept();
				ClientHandler clientHandler =  new ClientHandler(socket, client);
				clientHandler.setPriority( clientHandler.getPriority() + 1 );
				clientHandler.start();
			} catch (IOException e) {
			}
		}
	}

	public void closeSocket() throws IOException {
		this.serverSocket.close();	
		
	}

}
