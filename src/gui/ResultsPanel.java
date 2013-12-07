package gui;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import logic.Client;

public class ResultsPanel extends JPanel {
	
	Client client;
	ClientWindow window;
	JLabel request = new JLabel();

	public ResultsPanel(Client client, ClientWindow clientWindow) {
		this.client = client;
		this.window = clientWindow;
		
		this.setPreferredSize(new Dimension(600, 500));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		this.add(request);
	}
	
	public void setRequestText(String request){
		this.request.setText("Request -> " + request);
	}

}
