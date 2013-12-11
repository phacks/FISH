package logic;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.tika.Tika;

/**
 * Regularly checks for additions / deletions in the shared files folder, and triggers appropriate actions on the client process
 * 
 * @param client The client on which this thread is started
 * @see Client
 */
public class ClientProbe implements Runnable{

	/** The path towards the shared folder of the client */
	String sharedFilePath;
	/** The client on which this thread is started */
	Client client;
	/** Hashmap containing files names and types as the key. The value is used to determine is a file has been deleted */
	HashMap<String, Boolean> filesMap = new HashMap<String, Boolean>(); // If the boolean denotes the presence of the file in the shared folder
	/** The time after which the folder is checked again */
	private final int UPDATE_TIME = 5000;
	/** Tika is a tool provided by Apache to obtain the type of a file */
	Tika tika = new Tika();

	/**
	 * Initializes the hashmap with the contents of the folder
	 * 
	 * @param client The client on which this thread is started
	 * @throws IOException
	 */
	public ClientProbe(Client client) throws IOException{
		this.client = client;
		this.sharedFilePath = client.getSharedFilePath();
		initializeFilesMap(filesMap, sharedFilePath, "");
	}

	/**
	 * Detects additions and/or deletions in the shared files folder. This operation is repeated constantly, 
	 * and sleeps for a given mount of time defined by the UPDATE_TIME constant.
	 * 
	 * @see java.lang.Runnable#run()
	 */
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
				// All booleans are set at false
				setAllFalse(filesMap);
				/* 
				 * Additions are files that are not in the hashmap. An action is triggered on the client, then 
				 * the new files are added to the hash map. For all files that are already in the hashmap, their
				 * values are set at true.
				 */
				probeSharedFiles(filesMap, sharedFilePath, "");
				/*
				 * All entries in the hashmap with the value false correspond to deleted files. An action is triggered 
				 * on the client, and they are deleted from the hashmap.
				 */
				searchForDeletions(filesMap);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}


	/**
	 * All the values of the hashmap are set at false
	 * 
	 * @param filesMap Hashmap containing files names and types as the key. The value is used to determine is a file has been deleted 
	 */
	private void setAllFalse(HashMap<String, Boolean> filesMap) {
		for(Entry<String, Boolean> entry : filesMap.entrySet()){
			entry.setValue(false);
		}
	}

	/**
	 * For all deleted files, an action is triggered on the client, and the corresponding entry is deleted from the hashmap
	 * 
	 * @param filesMap Hashmap containing files names and types as the key. The value is used to determine is a file has been deleted 
	 * @see Client#removeFile(String)
	 */
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

	/**
	 * Additions are files that are not in the hashmap. An action is triggered on the client, then 
	 * the new files are added to the hash map. For all files that are already in the hashmap, their
	 * values are set at true.
	 * 
	 * @param filesMap Hashmap containing files names and types as the key. The value is used to determine is a file has been deleted
	 * @param sharedFilePath The path towards the shared folder of the client
	 * @param pathPrefix Allows to call recursively the method to browse directories. Initially set to "" to browse the shared folder.
	 * @throws IOException
	 * @see Client#addFile(String, String)
	 */
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

	/**
	 * Fills the hashmap with file names and type for files found in the shared folder or any directory, 
	 * subdirectory and so on.
	 * File type is obtained with the help of the Tika library, provyded by Apache: see http://tika.apache.org for more information
	 * All the values are set to false
	 * 
	 * @param filesMap Hashmap containing files names and types as the key. The value is used to determine is a file has been deleted
	 * @param sharedFilePath The path towards the shared folder of the client
	 * @param pathPrefix Allows to call recursively the method to browse directories. Initially set to "" to browse the shared folder.
	 * @throws IOException
	 */
	private void initializeFilesMap(HashMap<String, Boolean> filesMap, String sharedFilePath, String pathPrefix) throws IOException {

		File file = new File(sharedFilePath);
		File[] arrayFilesOrDirectories = file.listFiles();

		for(int i = 0; i < arrayFilesOrDirectories.length; i++){
			if(arrayFilesOrDirectories[i].isFile() && ! arrayFilesOrDirectories[i].isHidden()){
				filesMap.put(arrayFilesOrDirectories[i].getName(), false);
			}
			else if(arrayFilesOrDirectories[i].isDirectory()){
				String directoryPath = arrayFilesOrDirectories[i].getPath();
				initializeFilesMap(filesMap, directoryPath, pathPrefix + arrayFilesOrDirectories[i].getName() + "/");
			}

		}
	}



}
