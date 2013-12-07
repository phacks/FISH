package gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;

import javax.swing.JButton;

public class ResultButton extends JButton {
	
	private String address;
	private int port;

	public ResultButton(String name, String type, String clientName, String address, int port) {
		super(name + " - " + type + " @ " + clientName);
		setFocusPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.blue);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        this.address = address;
        this.port = port;
	}

}
