package gui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import logic.Client;

public class ResultsPanel extends JPanel {
	
	Client client;
	ClientWindow window;
	JLabel request = new JLabel();
	JLabel notFoundLabel = new JLabel("No results found for this request");

	public ResultsPanel(Client client, ClientWindow clientWindow) {
		this.client = client;
		this.window = clientWindow;
		
		this.setPreferredSize(new Dimension(600, 500));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		notFoundLabel.setForeground(Color.red);
		
		this.add(request);
		
	}
	
	public void setRequestText(String request){
		this.request.setText("Request -> " + request);
	}

	public void setNotFound() {
		this.removeAll();
		this.add(request);
		this.add(notFoundLabel);
	}

	public void setResults(String string) {
		this.removeAll();
		this.add(request);
		
		String[] results = string.split(",");
		String fileName;
		String fileType;
		String clientName;
		String address;
		int downloadPort;
		String[] arguments;
		
		for(String result : results){
			arguments = result.split("&");
			fileName = arguments[0];
			fileType = arguments[1];
			clientName = arguments[2];
			address = arguments[3];
			downloadPort = Integer.parseInt(arguments[4]);
			
			this.add(new ResultButton(fileName, fileType, clientName, address, downloadPort));
		}
	}

}
