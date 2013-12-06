package gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import logic.Client;

public class RegistrationPanel extends JPanel implements ActionListener{
	
	private static final long serialVersionUID = 1L;
	private JTextField nameInput = new JTextField("", 20);
	private JTextField addressInput = new JTextField("", 20);
	private JTextField portInput = new JTextField("", 20);
	private JButton registerButton = new JButton("Register");
	private Client client;
	private ClientWindow window;
	
	public RegistrationPanel(Client client, ClientWindow window) {
		
		this.client = client;
		this.window = window;
		
		nameInput.setMaximumSize(new Dimension(250,nameInput.getPreferredSize().height));
		addressInput.setMaximumSize(new Dimension(250,addressInput.getPreferredSize().height));
		portInput.setMaximumSize(new Dimension(250,portInput.getPreferredSize().height));
				
		nameInput.setText(client.getName());
		addressInput.setText(client.getAddress());
		portInput.setText(client.getPort());
		
		this.setPreferredSize(new Dimension(600, 500));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		this.add(nameInput);
		this.add(addressInput);
		this.add(portInput);
		this.add(registerButton);
		
		nameInput.setAlignmentX(Component.CENTER_ALIGNMENT);
		addressInput.setAlignmentX(Component.CENTER_ALIGNMENT);
		portInput.setAlignmentX(Component.CENTER_ALIGNMENT);
		registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		registerButton.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == registerButton){
			try {
				client.setName(nameInput.getText());
				client.share();
				this.window.setTitle(nameInput.getText());
				this.window.setRegisteredView();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

}
