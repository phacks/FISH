package logic;
import gui.ClientWindow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;



/**
 * Client process, that can connect to a server, add or delete shared files, request files from the server,
 * check the availability of a file and download files. 
 * 
 * Connections with the server or other clients are made through sockets. The client listens to the server replies through a separate thread (ClientReader)
 * and another thread (ClientProbe) regularly checks for new or removed shared files. The client has a server
 * part (ClientServer), aimed at providing interfaces for other clients to download his files, in which
 * new connections are hendled by a ClientHandler. 
 * 
 * The GUI for the client is contained in a ClientWindow.
 * 
 * @param sharedFilePath The path to the shared files folder
 * @param address The IP address of the server
 * @param port The port of the server
 * @param name The name of the client
 * @param downloadPort The port used for the server-side of the client
 * 
 * @see ClientReader
 * @see ClientProbe
 * @see ClientHandler
 * @see ClientServer
 * @see Server
 * @see ClientWindow
 *
 */
public class ClientP2P extends Client{


	/** Separate thread to receive messages from the server or other clients */
	//ClientReader clientReader;
	/** Server-side part of the client, to provide the interface for other clients to download shared files */
	ClientServer clientServer;
	/** Socket used when downloading a file. The socket is connected to the server part of a remote client */
	private ClientProbe clientProbe;
	/** Socket used to communicate with the predecessor */
	Socket predecessorSocket;
	/** Aimed at sending messages to the predecessor  */
	PrintWriter pwr;
	/** Aimed at receiving messages from the predecessor */
	private BufferedReader prd;
	/** Socket used to communicate with the successor */
	Socket successorSocket;
	/** Aimed at sending messages to the successor  */
	PrintWriter swr;
	/** Aimed at receiving messages from the successor */
	private BufferedReader srd;

	private String successorName;
	private String predecessorName;

	/**
	 * Sets each field to their default value, and starts the GUI thread
	 * 
	 * @param sharedFilePath The path towards the shared folder of the client
	 * @param address The IP address of the server
	 * @param port The port of the server
	 * @param name The name of the client
	 * @param downloadPort The port of the server-side part of the client
	 * @throws IOException
	 * @see ClientWindow
	 */
	public ClientP2P(String sharedFilePath, String address, String port, String name, String downloadPort) throws IOException {
		super(sharedFilePath, address, port, name, downloadPort);
		System.out.println("===== " + getName() + " =====");
	}

	public void displayNeighbours(String sendersName) {
		System.out.println("Successor : " + successorName);
		System.out.println("Predecessor : " + predecessorName);
		System.out.println("----------");
		swr.println("displayneighbours:" + sendersName);
		swr.flush();
	}


	public void setSuccessorName(String successorName){
		this.successorName = successorName;
	}

	private void enterRing(String address, String port) throws NumberFormatException, UnknownHostException, IOException {
		successorSocket = new Socket(address, Integer.parseInt(port));
		swr = new PrintWriter(successorSocket.getOutputStream());
		srd = new BufferedReader(new InputStreamReader(successorSocket.getInputStream()));
		new Thread(new ClientReader(this, getClientWindow(), srd)).start();
		swr.println("notify:" + super.getName() + ":" + getDownloadPort());
		swr.flush();
	}

	public void finalize(){
		pwr.println("connected:" + getName());
		pwr.flush();
	}

	public void notifyPredecessor(String clientName, String clientAddress, String ClientPort){
		pwr.println("successor:" + clientName + ":" + clientAddress + ":" + ClientPort);
		pwr.flush();
	}


	public void connectTo(String clientName, String clientAddress, String clientPort) throws NumberFormatException, UnknownHostException, IOException {
		successorSocket = new Socket(clientAddress, Integer.parseInt(clientPort));
		swr = new PrintWriter(successorSocket.getOutputStream());
		srd = new BufferedReader(new InputStreamReader(successorSocket.getInputStream()));
		new Thread(new ClientReader(this, getClientWindow(), srd)).start();
		swr.println("predecessor:" + super.getName() + ":" + super.getAddress() + ":" + super.getPort());
		swr.flush();
	}

	public void setPredecessor(String clientName, String clientAddress,	String clientPort) throws NumberFormatException, UnknownHostException, IOException {
		predecessorSocket = new Socket(clientAddress, Integer.parseInt(clientPort));
		pwr = new PrintWriter(predecessorSocket.getOutputStream());
		prd = new BufferedReader(new InputStreamReader(predecessorSocket.getInputStream()));
		new Thread(new ClientReader(this, getClientWindow(), prd)).start();
		predecessorName = clientName;
	}

	public void unshare() throws IOException{
		pwr.println("successor:" + successorName + ":" + successorSocket.getRemoteSocketAddress().toString().split("/")[0] + ":" +successorSocket.getPort());
		pwr.flush();
		//		swr.println("predecessor:" + predecessorName + ":" + predecessorSocket.getRemoteSocketAddress().toString().split(":")[0].substring(1) + ":" +predecessorSocket.getPort());
		//		swr.flush();
		pwr.println("quit");
		pwr.flush();
		swr.println("quit");
		swr.flush();
		predecessorSocket.close();
		successorSocket.close();
	}

	public void request(String[] keywords, String fileType, String clientName) throws UnknownHostException{

		String requestMessage = "request:" + getName() + ":" + InetAddress.getLocalHost().getHostAddress() + ":" + getDownloadPort() + ":";
		
		if (keywords[0].length() == 0 && keywords.length == 1){
			requestMessage += " ";
		}
		for (String keyword : keywords){
			requestMessage += keyword + ",";
		}
		requestMessage = requestMessage.substring(0, requestMessage.length() - 1);
		requestMessage += ":";

		if(! fileType.equals("")){
			requestMessage+= "type=" + fileType + ":";
		}
		if(! clientName.equals("")){
			requestMessage+= "client=" + clientName + ":";
		}

		swr.println(requestMessage);
		swr.flush();
		
		System.out.println(requestMessage);
	}

	public void fileSearch(String[] keywords, String fileType, String clientNameRequest, String askersName, String askersAddress, String askersDownloadPort, String request) throws NumberFormatException, IOException {

		Set<String> setKeys =  filesList.keySet();
		setKeys = findMatches(setKeys, keywords, fileType);
		String reply;
		if(setKeys.size() != 0){
			reply = "found:";
			Iterator<String> it = setKeys.iterator();
			String key;
			while(it.hasNext()){
				key = it.next();
				reply += key + "&" + getName() + "&" + InetAddress.getLocalHost().getHostAddress() + "&" + getDownloadPort() + ",";
			}
			reply = reply.substring(0, reply.length() - 1);
			Socket tempSocket = new Socket(askersAddress, Integer.parseInt(askersDownloadPort));
			PrintWriter tempWriter = new PrintWriter(tempSocket.getOutputStream());
			tempWriter.println(reply);
			tempWriter.flush();
			tempSocket.close();
		}
		
		swr.println(request);
		swr.flush();
		
	}

	public Set<String> findMatches(Set<String> setKeys, String[] keywords, String fileType){

		Iterator<String> it = setKeys.iterator();
		String key;
		Set<String> keysNotMatching = new HashSet<String>();

		for (String keyword : keywords){
			while(it.hasNext()){
				key = it.next();

				if((! fileType.equals("")) && (! key.contains(fileType)) ){
					keysNotMatching.add(key);
				}
				else if (! key.contains(keyword)){
					keysNotMatching.add(key);
				}
			}
			it = setKeys.iterator();
		}

		it = keysNotMatching.iterator();
		while(it.hasNext()){
			key = it.next();
			setKeys.remove(key);
		}

		return setKeys;

	}
	
	public void checkDownload(String name, String clientName){
		getClientWindow().startDownload(name, clientName);
	}


	/**
	 * Fills the hashmap with file names, type and paths for files found in the shared folder or any directory, 
	 * subdirectory and so on.
	 * File type is obtained with the help of the Tika library, provyded by Apache: see http://tika.apache.org for more information
	 * 
	 * @param fileNameList The hashmap containing the file names, types and paths
	 * @param sharedFilePath The path towards the shared folder of the client
	 * @param pathPrefix Allows to call recursively the method to browse directories. Initially set to "" to browse the shared folder.
	 * @throws IOException
	 */
	//	private void getFileNames(HashMap<String, String> fileNameList, String sharedFilePath, String pathPrefix) throws IOException {
	//
	//		File file = new File(sharedFilePath);
	//		File[] arrayFilesOrDirectories = file.listFiles();
	//
	//		for(int i = 0; i < arrayFilesOrDirectories.length; i++){
	//			if(arrayFilesOrDirectories[i].isFile() && ! arrayFilesOrDirectories[i].isHidden()){
	//				String mediaType = tika.detect(arrayFilesOrDirectories[i]).split("/")[0];
	//				filesList.put(arrayFilesOrDirectories[i].getName() + "&" + mediaType, pathPrefix);
	//			}
	//			else if(arrayFilesOrDirectories[i].isDirectory()){
	//				String directoryPath = arrayFilesOrDirectories[i].getPath();
	//				getFileNames(fileNameList, directoryPath, pathPrefix + arrayFilesOrDirectories[i].getName() + "/");
	//			}
	//		}
	//
	//	}

	/**
	 * Given a file name and type (i.e. the key), returns the file path
	 * @param fileNameAndType
	 * @return The path to the file
	 */
	//	public String findPathToFile(String fileNameAndType){
	//		return filesList.get(fileNameAndType);
	//	}

	/**
	 * Attempts to connect to the server with the creationSocket socket
	 * 
	 * @return true if the socket was successfully created, false otherwise
	 */
	//	private boolean creationSocket(){
	//
	//		try {
	//			clientSocket = new Socket();
	//			clientSocket.connect(new InetSocketAddress(address, Integer.parseInt(port)), 1000);
	//			wr = new PrintWriter(clientSocket.getOutputStream());
	//			rd = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
	//
	//		} catch (SocketTimeoutException e) {
	//			return false;
	//		} catch (UnknownHostException e) {
	//			return false;
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//			return false;
	//		}
	//		return true;
	//
	//	}

	/**
	 * Sends a "register" message to the server, containing the client's file names, types, and port for its server part
	 * 
	 * @throws IOException
	 */
	public void share() throws IOException{

		super.getFileNames(super.getFilesList(), super.getSharedFilePath(), "");

		clientServer = new ClientServer(this, getDownloadPort(), getClientWindow());
		new Thread(clientServer).start();
		clientProbe = new ClientProbe(this);
		new Thread(clientProbe).start();

		setPredecessor(getName(), getAddress(), getDownloadPort());

		enterRing(super.getAddress(), super.getPort());



	}

	/**
	 * Sends an "unregister" message to the server
	 * @throws IOException
	 */
	//	public void unshare() throws IOException{
	//
	//		String unregisterMessage = "unregister";
	//		wr.println(unregisterMessage);
	//		wr.flush();
	//		
	//		clientServer.closeSocket();
	//		clientSocket.close();
	//	}

	/**
	 * Sends a "request" message to the server. The request message is composed of one or more keywords, and optionnally
	 * a file type and a client name (not implemented yet).
	 * 
	 * @param keywords The keywords for the search
	 * @param fileType The file type required by the client search ("" matches all files) 
	 * @param clientName The remote client required by the client search (not implemented yet)
	 */
	//	public void request(String[] keywords, String fileType, String clientName){
	//
	//		String requestMessage = "request:";
	//		if (keywords[0].length() == 0 && keywords.length == 1){
	//			requestMessage += " ";
	//		}
	//		for (String keyword : keywords){
	//			requestMessage += keyword + ",";
	//		}
	//		requestMessage = requestMessage.substring(0, requestMessage.length() - 1);
	//		requestMessage += ":";
	//
	//		if(! fileType.equals("")){
	//			requestMessage+= "type=" + fileType + ":";
	//		}
	//		if(! clientName.equals("")){
	//			requestMessage+= "client=" + clientName + ":";
	//		}
	//		
	//		wr.println(requestMessage);
	//		wr.flush();
	//	}

	/**
	 * Sends a "download" message to a remote client to start a download.
	 * 
	 * This methods creates a new ClientReader thread to handle the download.
	 * 
	 * @param fileName The name of the file to be downloaded
	 * @param address The IP address of the owner the file to be downloaded
	 * @param downloadPort The port of the server part of the owner the file to be downloaded
	 * @param pathForDownloadedFile The path of the directory where the file should be saved 
	 * @see ClientReader
	 */
	//	public void download(String fileName, String address, String downloadPort, String pathForDownloadedFile){
	//		
	//		this.setPathForDownloadedFile(pathForDownloadedFile);
	//		
	//		PrintWriter dwr = null;
	//		BufferedReader drd = null;
	//		
	//		// Creates the socket to the client that has the requested file
	//		try {
	//			downloadSocket = new Socket();
	//			downloadSocket.connect(new InetSocketAddress(address, Integer.parseInt(downloadPort)), 1000);
	//			dwr = new PrintWriter(downloadSocket.getOutputStream());
	//			drd = new BufferedReader( new InputStreamReader(downloadSocket.getInputStream()));
	//		} catch (SocketTimeoutException e) {
	//			System.err.println(e);
	//		} catch (UnknownHostException e) {
	//			System.err.println(e);
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//		
	//		new Thread(new ClientReader(this, clientWindow, drd)).start();
	//		
	//		dwr.println("download:" + fileName);
	//		dwr.flush();
	//		
	//	}

	/**
	 * Adds a new file to the hashmap (when a new file is detected by the ClientProbe)
	 * @param string The file name and type of the file
	 * @param pathPrefix The path (relative to the shared directory) to the file
	 */
	//	public void addFile(String string, String pathPrefix) {
	//		filesList.put(string, pathPrefix);
	//		wr.println("addfile:" + string);
	//		wr.flush();
	//	}

	/**
	 * Deletes a file from the hashmap (when a removed file is detected by the ClientProbe)
	 * @param string
	 */
	//	public void removeFile(String string){
	//		for(Entry<String, String> entry : filesList.entrySet()){
	//			if(entry.getKey().startsWith(string + "&")){
	//				filesList.remove(entry.getKey());
	//				wr.println("deletefile:" + string);
	//				wr.flush();
	//				break;
	//			}
	//		}
	//	}



	/**
	 * Asks the server ("isavailable") if a particular file from a particular client is available for download
	 * 
	 * @param fileName The name of the file
	 * @param remoteClientName The name of the remote client
	 */
	//	public void checkDownload(String fileName, String remoteClientName) {
	//		wr.println("isavailable:" + fileName + ":" + remoteClientName);
	//		wr.flush();
	//		clientWindow.setWaitingCursor();
	//	}

	/**
	 * Notifies the GUI that a download has begun
	 * 
	 * @param fileName The name of the file
	 * @param remoteClientName The name of the remote client
	 */
	//	public void startDownload(String fileName, String remoteClientName){
	//		clientWindow.startDownload(fileName, remoteClientName);
	//	}

	/**
	 * Notifies the GUI that the requested file is not available
	 * 
	 * @param fileName The name of the file
	 * @param remoteClientName The name of the remote client
	 */
	//	public void fileNotAvailable(String fileName, String remoteClientName){
	//		clientWindow.fileNotAvailable(fileName, remoteClientName);
	//	}

	/**
	 * Main function, whose purpose is to set default values and to instanciate the Client.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		String sharedFilePath = null;
		if (args.length > 0) {
			sharedFilePath = args[0];
		} else {
			sharedFilePath = SHAREDFILEPATH;
		}

		String address = null;
		if (args.length > 1) {
			address = args[0];
		} else {
			address = ADDRESS;
		}

		String port = null;
		if (args.length > 2) {
			port = args[1];
		} else {
			port = PORT;
		}

		String name = null;
		if (args.length > 3) {
			name = args[2];
		} else {
			name = "Client " + Integer.toString((int) Math.ceil(Math.random()*1000));
		}

		String downloadPort = null;
		if (args.length > 4) {
			downloadPort = args[3];
		} else {
			downloadPort = "10001";
		}

		new ClientP2P(sharedFilePath, address, port, name, downloadPort);

	}

	//	public String getName() {
	//		return name;
	//	}
	//
	//
	//	public void setName(String name) {
	//		this.name = name;
	//	}
	//
	//	public String getAddress() {
	//		return address;
	//	}
	//
	//
	//	public void setAddress(String address) {
	//		this.address = address;
	//	}
	//
	//	public String getPort() {
	//		return port;
	//	}
	//
	//
	//	public void setPort(String port) {
	//		this.port = port;
	//	}
	//
	//	public String getDownloadPort() {
	//		return downloadPort;
	//	}
	//
	//
	//	public void setDownloadPort(String downloadPort) {
	//		this.downloadPort = downloadPort;
	//	}
	//
	//	public String getSharedFilePath() {
	//		return sharedFilePath;
	//	}
	//
	//
	//	public void setSharedFilePath(String sharedFilePath) {
	//		this.sharedFilePath = sharedFilePath;
	//	}
	//
	//	public String getPathForDownloadedFile() {
	//		return pathForDownloadedFile;
	//	}
	//
	//
	//	public void setPathForDownloadedFile(String pathForDownloadedFile) {
	//		this.pathForDownloadedFile = pathForDownloadedFile;
	//	}

	public static final String SHAREDFILEPATH = "shared/";
	public static final String ADDRESS = "localhost";
	public static final String PORT = "10000";

}
