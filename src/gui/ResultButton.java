package gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import logic.Client;

public class ResultButton extends JButton implements ActionListener{

	private String address;
	private int port;
	private Client client;
	private String name;
	private String type;
	private ClientWindow window;
	String clientName;
	public String id;

	public ResultButton(Client client, ClientWindow window, String name, String type, String clientName, String address, int port) {
		super(name + " - " + type + " @ " + clientName);
		setFocusPainted(false);
		setMargin(new Insets(0, 0, 0, 0));
		setContentAreaFilled(false);
		setBorderPainted(false);
		setOpaque(false);
		setForeground(Color.blue);
		setCursor(new Cursor(Cursor.HAND_CURSOR));

		this.clientName = clientName;
		this.window = window;
		this.name = name;
		this.address = address;
		this.port = port;
		this.client = client;
		this.type = type;
		
		this.id = name + clientName; 

		this.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == this){
			
			try {
				client.checkDownload(name, clientName, address, Integer.toString(port));
			} catch (NumberFormatException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			
		}
	}

	public void startDownload() {
		String pathForDownloadedFile = "";

		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int option = chooser.showSaveDialog(null);
		if (option == JFileChooser.APPROVE_OPTION)
		{
			pathForDownloadedFile = chooser.getCurrentDirectory().getAbsolutePath() + "/";
			window.newDownload(name + " - " + type + " @ " + clientName);

			window.setDownloads();
			client.download(name + "&" + type, address, Integer.toString(port), pathForDownloadedFile);
		}
		else{
			window.setResultsPanel();
		}
	}

	public void fileNotAvailable() {
		JOptionPane.showMessageDialog(null,"The requested file is not shared by the client anymore.");
		window.setSearchPanel();
	}

}
