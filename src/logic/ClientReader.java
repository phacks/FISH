package logic;

import gui.ClientWindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Receives and interpret the messages received by the client, both from the server or other clients.
 * 
 * @param client The client on which this thread is started
 * @param clientWindow JFrame holding all the GUI
 * @param rd Aimed at receiving messages from the server or another client
 * @see Client
 * @see ClientHandler
 * @see Handler
 */
public class ClientReader implements Runnable {

	/** JFrame holding all the GUI */
	ClientWindow clientWindow;
	/** Aimed at receiving messages from the server or another client */
	BufferedReader rd;
	/** The client on which this thread is started */
	Client client;

	/**
	 * @param client The client on which this thread is started
	 * @param clientWindow JFrame holding all the GUI
	 * @param rd Aimed at receiving messages from the server or another client
	 */
	public ClientReader(Client client, ClientWindow clientWindow, BufferedReader rd) {
		this.clientWindow = clientWindow;
		this.rd = rd;
		this.client = client;
	}

	/**
	 * Read incoming messages from the server or another client, parses them and call the appropriate methods to 
	 * reply and/or execute actions.
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {

		String str;
		try {
			while ((str = rd.readLine()) != null){
				/*
				 * Received from the server, acknoledges that the client is properly registered
				 * 
				 */
				if (str.equals("registered")){
					clientWindow.setRegisteredView();
				}
				
				/*
				 * Received from the server, contains the results of a search
				 */
				if (str.startsWith("reply:")){
					String[] command = str.split(":");
					if (command[1].equals("notfound")){
						clientWindow.setResultsNotFound();
					}
					else if(command[1].equals("found")){
						clientWindow.setResults(command[2]);
					}
				}
				
				/*
				 * Received from another client, starts and processes the dowloading of a file
				 */
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
				
				/*
				 * Received from the server, about the availability of a file to download
				 */
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
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
