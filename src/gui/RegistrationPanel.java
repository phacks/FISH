package gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import logic.Client;

public class RegistrationPanel extends JPanel implements ActionListener{
	
	private static final long serialVersionUID = 1L;
	private JTextField nameInput = new JTextField("", 20);
	private JTextField addressInput = new JTextField("", 20);
	private JTextField portInput = new JTextField("", 20);
	private JTextField downloadPortInput = new JTextField("", 20);
	private JTextField sharedInput = new JTextField("", 14);
	private JButton sharedButton = new JButton("Choose");
	private JPanel sharedPanel = new JPanel();
	private JButton registerButton = new JButton("Register");
	private Client client;
	private ClientWindow window;
	
	public RegistrationPanel(Client client, ClientWindow window) {
		
		this.client = client;
		this.window = window;
		
		sharedPanel.add(sharedInput);
		sharedPanel.add(sharedButton);
		
		nameInput.setMaximumSize(new Dimension(250,nameInput.getPreferredSize().height));
		addressInput.setMaximumSize(new Dimension(250,addressInput.getPreferredSize().height));
		portInput.setMaximumSize(new Dimension(250,portInput.getPreferredSize().height));
		downloadPortInput.setMaximumSize(new Dimension(250,portInput.getPreferredSize().height));
		sharedInput.setMaximumSize(new Dimension(150,sharedInput.getPreferredSize().height));
		sharedButton.setMaximumSize(new Dimension(50,sharedInput.getPreferredSize().height));
		sharedPanel.setMaximumSize(new Dimension(400, sharedInput.getPreferredSize().height + 20));
		
		nameInput.setText(client.getName());
		addressInput.setText(client.getAddress());
		portInput.setText(client.getPort());
		downloadPortInput.setText(client.getDownloadPort());
		sharedInput.setText(client.getSharedFilePath());
		
		this.setPreferredSize(new Dimension(600, 500));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		this.add(nameInput);
		this.add(addressInput);
		this.add(portInput);
		this.add(downloadPortInput);
		this.add(sharedPanel);
		this.add(registerButton);
		
		nameInput.setAlignmentX(Component.CENTER_ALIGNMENT);
		addressInput.setAlignmentX(Component.CENTER_ALIGNMENT);
		portInput.setAlignmentX(Component.CENTER_ALIGNMENT);
		downloadPortInput.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		sharedButton.addActionListener(this);
		
		registerButton.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == registerButton){
			try {
				client.setName(nameInput.getText());
				client.setAddress(addressInput.getText());
				client.setPort(portInput.getText());
				client.setDownloadPort(downloadPortInput.getText());
				client.setSharedFilePath(sharedInput.getText());
				client.share();
				this.window.setTitle(nameInput.getText());
				//this.window.setRegisteredView();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		if(e.getSource() == sharedButton){

			JFileChooser chooser = new JFileChooser();
		    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		    int option = chooser.showSaveDialog(null);
		    if (option == JFileChooser.APPROVE_OPTION)
		    {
		        sharedInput.setText(chooser.getCurrentDirectory().getAbsolutePath() + "/");
		    }
		}
	}

}
