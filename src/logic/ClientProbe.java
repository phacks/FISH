package logic;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.tika.Tika;

public class ClientProbe implements Runnable{

	String sharedFilePath;
	Client client;
	HashMap<String, Boolean> filesMap = new HashMap<String, Boolean>(); // If the boolean denotes the presence of the file in the shared folder
	private final int UPDATE_TIME = 5000;
	
	Tika tika = new Tika();

	public ClientProbe(Client client) throws IOException{
		this.client = client;
		this.sharedFilePath = client.getSharedFilePath();
		initializeFilesMap(filesMap, sharedFilePath, "");
	}

	@Override
	public void run() {
		while(true){
			try {
				Thread.sleep(UPDATE_TIME);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				setAllFalse(filesMap);
				probeSharedFiles(filesMap, sharedFilePath, "");
				searchForDeletions(filesMap);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}


	private void setAllFalse(HashMap<String, Boolean> filesMap2) {
		for(Entry<String, Boolean> entry : filesMap.entrySet()){
			entry.setValue(false);
		}
	}

	private void searchForDeletions(HashMap<String, Boolean> filesMap) {
		if(filesMap.containsValue(false)){
			for(Entry<String, Boolean> entry : filesMap.entrySet()){
				if(entry.getValue() == false){
					client.removeFile(entry.getKey());
					filesMap.remove(entry.getKey());
					break;
				}
			}
			searchForDeletions(filesMap);
		}
	}

	private void probeSharedFiles(HashMap<String, Boolean> filesMap, String sharedFilePath, String pathPrefix) throws IOException {
		File file = new File(sharedFilePath);
		File[] arrayFilesOrDirectories = file.listFiles();

		for(int i = 0; i < arrayFilesOrDirectories.length; i++){
			if(arrayFilesOrDirectories[i].isFile() && ! arrayFilesOrDirectories[i].isHidden()){
				if(! filesMap.containsKey(arrayFilesOrDirectories[i].getName())){
					String mediaType = tika.detect(arrayFilesOrDirectories[i]).split("/")[0];
					client.addFile(arrayFilesOrDirectories[i].getName() + "&" + mediaType, pathPrefix);
				}
				filesMap.put(arrayFilesOrDirectories[i].getName(), true);
			}
			else if(arrayFilesOrDirectories[i].isDirectory()){
				String directoryPath = arrayFilesOrDirectories[i].getPath();
				probeSharedFiles(filesMap, directoryPath, pathPrefix + arrayFilesOrDirectories[i].getName() + "/");
			}
		}
	}

	private void initializeFilesMap(HashMap<String, Boolean> filesMap, String sharedFilePath, String pathPrefix) throws IOException {

		File file = new File(sharedFilePath);
		File[] arrayFilesOrDirectories = file.listFiles();

		for(int i = 0; i < arrayFilesOrDirectories.length; i++){
			if(arrayFilesOrDirectories[i].isFile() && ! arrayFilesOrDirectories[i].isHidden()){
				filesMap.put(arrayFilesOrDirectories[i].getName(), true);
			}
			else if(arrayFilesOrDirectories[i].isDirectory()){
				String directoryPath = arrayFilesOrDirectories[i].getPath();
				initializeFilesMap(filesMap, directoryPath, pathPrefix + arrayFilesOrDirectories[i].getName() + "/");
			}

		}
	}



}
