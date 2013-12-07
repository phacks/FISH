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
					System.out.println(str);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
