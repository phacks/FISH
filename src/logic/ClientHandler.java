package logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {

	Socket socket;
	BufferedReader rd;
	PrintWriter wr;
	Client client;

	public ClientHandler(Socket socket, Client client) throws IOException {
		this.socket = socket;
		this.client = client;

		rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		wr = new PrintWriter(socket.getOutputStream());
	}

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
						socket.close();
						break;

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
