package logic;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientServer implements Runnable {
	
	Client client;
	ServerSocket serverSocket;

	public ClientServer(Client client, String downloadPort) {
		try {
			this.serverSocket = new ServerSocket(Integer.parseInt(downloadPort));
			this.client = client;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
