package logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
	
	Socket socket;
	BufferedReader rd;
	PrintWriter wr;

	public ClientHandler(Socket socket) throws IOException {
		this.socket = socket;

		rd = new BufferedReader( new InputStreamReader(socket.getInputStream()));
		wr = new PrintWriter(socket.getOutputStream());
	}

	@Override
	public void run() {
		String str;
		
		try {
			while((str = rd.readLine()) != null){
				System.out.println(str);
				wr.println("Connection established");
				wr.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
