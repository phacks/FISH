package gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
	private SearchPanel searchPanel;
	private ClientWindow clientWindow = this;
	private ResultsPanel resultsPanel; 

	public ClientWindow(Client client) {

		this.client = client;

		registrationPanel = new RegistrationPanel(client, this);
		searchPanel = new SearchPanel(client, this);
		resultsPanel = new ResultsPanel(client, this);

		this.setLocationRelativeTo(null);

		this.setSize(600, 600);

		this.setContentPane(mainPanel);

		this.setVisible(true);

		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		this.addWindowListener(exitListener);
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
		tabbedPane.add("Search", searchPanel);
		tabbedPane.add("Results", resultsPanel);

		this.repaint();
		this.revalidate();
	}

	@Override
	public void run() {
		this.setTitle(client.getName());
		initializeMainPanel();
	}
	
	public void resultsPanelSetRequestText(String request){
		resultsPanel.setRequestText(request);
		this.repaint();
		this.revalidate();
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

	WindowListener exitListener = new WindowAdapter() {

		@Override
		public void windowClosing(WindowEvent e) {
			try {
				client.unshare();
				clientWindow.dispose();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
		}
	};

}
