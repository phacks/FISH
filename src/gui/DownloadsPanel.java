package gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import logic.Client;

public class DownloadsPanel extends JPanel implements ActionListener{
	
	Client client;
	ClientWindow window;
	JButton clear = new JButton("Clear");
	
	public DownloadsPanel(Client client, ClientWindow clientWindow) {
		this.client = client;
		this.window = clientWindow;
		
		this.setPreferredSize(new Dimension(600, 500));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		clear.setAlignmentX(Component.CENTER_ALIGNMENT);
		clear.addActionListener(this);
		this.add(clear);
		
	}

	public void newDownload(String string) {
		DownloadDetailPanel download = new DownloadDetailPanel(string);
		this.add(download);
	}

	public void initializeDownload(String name, int max) {
		int c = this.getComponentCount();
		
		for (int i = 1; i < c; i++){
			if (((DownloadDetailPanel) this.getComponent(i)).name.equals(name)){
				((DownloadDetailPanel) this.getComponent(i)).initializeProgress(max);
			}
		}
	}

	public void updateDownload(String name, int n) {
		int c = this.getComponentCount();
		
		for (int i = 1; i < c; i++){
			if (((DownloadDetailPanel) this.getComponent(i)).name.equals(name)){
				((DownloadDetailPanel) this.getComponent(i)).updateProgress(n);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == clear){
			int c = this.getComponentCount();
			for (int i = 1; i < c; i++){
				if (((DownloadDetailPanel) this.getComponent(i)).progress.getValue() == ((DownloadDetailPanel) this.getComponent(i)).progress.getMaximum()){
					this.remove(i);
					c--;
				}
			}
			this.repaint();
			this.revalidate();
		}
	}
}
