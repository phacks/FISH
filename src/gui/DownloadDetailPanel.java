package gui;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class DownloadDetailPanel extends JPanel {
	
	String name;
	
	JLabel detail = new JLabel("");
	JProgressBar progress = new JProgressBar();

	public DownloadDetailPanel(String string) {
		
		name = string.split(" -")[0];
		
		this.setMaximumSize(new Dimension(500, 50));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		detail.setText(string);
		
		//progress.setMaximumSize(new Dimension(progress.getHeight(), 200));
		progress.setStringPainted(true);
		progress.setIndeterminate(true);
		
		this.add(detail);
		this.add(progress);
	}
	
	public void initializeProgress(int max){
		
		progress.setIndeterminate(false);
		progress.setMinimum(0);
		progress.setMaximum(max);
		this.repaint();
		this.revalidate();
	}
	
	public void updateProgress(int n){
		progress.setValue(n);
		this.repaint();
		this.revalidate();
	}

}
