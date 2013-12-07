package logic;

import gui.ClientWindow;

import java.io.BufferedReader;
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
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
