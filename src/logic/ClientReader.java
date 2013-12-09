package logic;

import gui.ClientWindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ClientReader implements Runnable {

	ClientWindow clientWindow;
	BufferedReader rd;
	Client client;

	public ClientReader(Client client, ClientWindow clientWindow, BufferedReader rd) {
		this.clientWindow = clientWindow;
		this.rd = rd;
		this.client = client;
	}

	public void run() {

		String str;
		try {
			while ((str = rd.readLine()) != null){
				if (str.equals("registered")){
					clientWindow.setRegisteredView();
				}
				if (str.startsWith("reply:")){
					String[] command = str.split(":");
					if (command[1].equals("notfound")){
						clientWindow.setResultsNotFound();
					}
					else if(command[1].equals("found")){
						clientWindow.setResults(command[2]);
					}
				}
				if (str.startsWith("download:")){
					String fileName = str.split(":")[1];
					int fileSize = Integer.parseInt(str.split(":")[2]);
					System.out.println("Downloading " + fileName + ", size : " + fileSize + " bytes" );
					
					File newFile = new File(client.getPathForDownloadedFile(), fileName);
					FileOutputStream fos = new FileOutputStream(newFile);
					
					int content;
					while ((content = rd.read()) != -1){
						fos.write(content);
					}
					fos.close();
					System.out.println("Download completed");
					
					client.downloadSocket.close();
				}
			}
		} catch (IOException e) {
		}

	}

}
