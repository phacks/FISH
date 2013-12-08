package gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import logic.Client;

public class ResultButton extends JButton implements ActionListener{
	
	private String address;
	private int port;
	private Client client;
	private String name;

	public ResultButton(Client client, String name, String type, String clientName, String address, int port) {
		super(name + " - " + type + " @ " + clientName);
		setFocusPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.blue);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        this.name = name;
        this.address = address;
        this.port = port;
        this.client = client;
        
        this.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == this){
			client.download(name, address, Integer.toString(port));
		}
	}

}
