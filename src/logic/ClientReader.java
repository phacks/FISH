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
					
					clientWindow.initializeDownload(fileName, fileSize);
					
					File newFile = new File(client.getPathForDownloadedFile(), fileName);
					FileOutputStream fos = new FileOutputStream(newFile);
					
					int content;
					int n = 0;
					while ((content = rd.read()) != -1){
						fos.write(content);
						n += 2;
						clientWindow.updateDownload(fileName, n);
					}
					fos.close();
					break;
				}
				if(str.startsWith("isavailable")){
					String isAvailable = str.split(":")[1];
					String fileName = str.split(":")[2];
					String remoteClientName = str.split(":")[3];
					if(isAvailable.equals("yes")){
						client.startDownload(fileName, remoteClientName);
					}
					if(isAvailable.equals("no")){
						client.fileNotAvailable(fileName, remoteClientName);
					}
				}
			}
			client.downloadSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
