package gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import logic.Client;

public class SearchPanel extends JPanel implements ActionListener{

	Client client;
	ClientWindow window;
	JTextField keywordInput = new JTextField("keywords (separated by spaces)");
	JButton searchButton = new JButton("Search");
	
	JRadioButton text = new JRadioButton("text");
    JRadioButton image = new JRadioButton("image");
    JRadioButton audio = new JRadioButton("audio");
    JRadioButton all = new JRadioButton("all");
    ButtonGroup fileType = new ButtonGroup();


	public SearchPanel(Client client, ClientWindow clientWindow) {
		this.client = client;
		this.window = clientWindow;

		keywordInput.setMaximumSize(new Dimension(250,keywordInput.getPreferredSize().height));

		this.setPreferredSize(new Dimension(600, 500));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		fileType.add(all);
		fileType.add(text);
		fileType.add(image);
		fileType.add(audio);
		
		all.setSelected(true);
		
		this.add(keywordInput);
		this.add(all);
		this.add(text);
		this.add(image);
		this.add(audio);
		
		
		this.add(searchButton);


		keywordInput.setAlignmentX(Component.CENTER_ALIGNMENT);
		all.setAlignmentX(Component.CENTER_ALIGNMENT);
		text.setAlignmentX(Component.CENTER_ALIGNMENT);
		image.setAlignmentX(Component.CENTER_ALIGNMENT);
		audio.setAlignmentX(Component.CENTER_ALIGNMENT);
		searchButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		searchButton.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == searchButton){
			String[] keywords = keywordInput.getText().split(" ");
			String fileType = "";
			
			if (text.isSelected()){
				fileType = "text";
			}
			if (image.isSelected()){
				fileType = "image";
			}
			if (audio.isSelected()){
				fileType = "audio";
			}
			
			client.request(keywords, fileType, "");
			String requestText = "Keywords: ";
			for (String keyword : keywords){
				requestText += keyword + " ";
			}
			if (! fileType.equals(""))
				requestText += "| File type: " + fileType;
				
			window.resultsPanelSetRequestText(requestText);
		}

	}

}
