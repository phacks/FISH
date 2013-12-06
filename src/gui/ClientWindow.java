package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import logic.Client;


public class ClientWindow extends JFrame implements Runnable, ActionListener{
	
	private JPanel mainPanel = new JPanel();
	private JTabbedPane tabbedPane = new JTabbedPane();
	private Client client; 
	private JButton unregister = new JButton("unregister");
	private RegistrationPanel registrationPanel;

	public ClientWindow(Client client) {
		
		this.client = client;
		
		registrationPanel = new RegistrationPanel(client, this);
		
        this.setLocationRelativeTo(null);
        
        this.setSize(600, 600);
              
        this.setContentPane(mainPanel);
        
        this.setVisible(true);
        
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	private void initializeMainPanel() {
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		tabbedPane.addTab("Registration", registrationPanel);
		mainPanel.add(tabbedPane);
		
		unregister.setAlignmentX(CENTER_ALIGNMENT);
		unregister.addActionListener(this);
		mainPanel.add(unregister);
	}

	public void setRegisteredView(){
		tabbedPane.removeAll();
		tabbedPane.add("Test", new JPanel());
		
		this.repaint();
		this.revalidate();
	}
	
	@Override
	public void run() {
		this.setTitle(client.getName());
		initializeMainPanel();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == unregister){
			try {
				client.unshare();
				
				tabbedPane.removeAll();
				tabbedPane.add("Registration", registrationPanel);
				
				this.repaint();
				this.revalidate();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	

}
