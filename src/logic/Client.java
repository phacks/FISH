package logic;
import gui.ClientWindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.tika.Tika;

/*
 * Client-Server Protocol
 * ----------------------
 * 
 * MANDATORY PART
 * 
 * - [IMPLEMENTED] A client registers at the server by sending a list of shared file names :
 * 		"register:filename1&filetype1,filename2&filetype2,...,filenameN&filetypeN:clientname:downloadport"
 * 
 * - [IMPLEMENTED] The server confirms the client is registered :
 * 		"registered"
 * 
 * - [IMPLEMENTED] A client sends to the server a search request for keywords, and optionnaly specify a file type or a client name :
 * 		"request:keyword1,keyword2,...,keywordN:(type=filetype):(client=clientname)"
 * 
 * - [IMPLEMENTED] The server replies to the requesting client by sending either a 'not found' message or addresses of client(s) sharing the requested file :
 * 		"reply:notfound"
 * 		"reply:found:filename1&filetype1&clientname1&address1&downloadport1,...,filenameN&filetypeN&clientnameN&addressN&downloadportN"
 * 
 * - [IMPLEMENTED] The client unregisters at the server (when the client stops sharing files) :
 * 		"unregister:clientname"
 * 
 * - [IMPLEMENTED] A client asks a specific file to another client for downloading it :
 * 		"download:filename"
 * 
 * - [IMPLEMENTED] The other client gives the size of the file, and start sending out the file bytes :
 * 		"download:filename:filesize\nfilebytes" (\n stands for a new line)
 * 
 * - [IMPLEMENTED] A client notifies the server that he wants to share a new file :
 * 		"addfile:filename&fileType"
 * 
 * - [IMPLEMENTED] A client notifies the server that he does not share a file anymore :
 * 		"deletefile:filename"
 * 
 * - [IMPLEMENTED] A client asks the server if a specific file is shared by a specific client ;
 * 		"isavailable:filename:remoteclientname"
 * 
 * - [IMPLEMENTED] The server replies :
 * 		"isavailable:yes:filename:clientname"
 * 		"isavailable:no:filename:clientname"
 * 
 * 
 * OPTIONAL PART
 * 
 * - A client asks the server the number of files shared :
 * 		"howmanyfiles"
 * 		"howmanyfiles -e" (excluding his files)
 * 
 * - The server replies to the requesting client :
 * 		"howmanyfiles:numberoffiles"
 * 		"howmanyfiles -e:numberoffiles"
 * 
 * - A client asks the server the number of registered clients :
 * 		"howmanyclients"
 * 		"howmanyclients -e" (excluding him)
 * 
 * - The server replies to the requesting client :
 * 		"howmanyclients:numberofclients"
 * 		"howmanyclients -e:numberofclients"
 * 
 * - A client asks the list of the names all registered clients :
 * 		"whoisregistered"
 * 		"whoisregistered -e" (excluding his name)
 * 
 * - The server replies to the requesting client :
 * 		"whoisregistered:name1,name2,...,nameN"
 * 		"whoisregistered -e:name1,name2,...,nameN"
 * 
 * - A client asks the list of the shared files of a particular client :
 * 		"filesofclient:clientname"
 * 
 * - The server replies to the requesting client :
 * 		"filesofclient:clientname:filename1&filetype1,filename2&filetype2,...,filenameN&filetypeN"
 * 
 * ----------------------
 * The protocol hereby defined contains messages that may not be implemented in the final version of the project.
 */


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
public class Client {

	/** The path towards the shared folder of the client */
	private String sharedFilePath;
	/** The IP address of the server */
	private String address;
	/** The port of the server */
	private String port;
	/** The name of the client */
	private String name;
	/** The port of the server-side part of the client */
	private String downloadPort;
	/** Hashmap containing files names and types as the key, and their path as the value */
	protected HashMap<String, String> filesList = new HashMap<String, String>();
	/** Tika is a tool provided by Apache to obtain the type of a file */
	Tika tika = new Tika();
	/** Socket used to communicate with the server */
	Socket clientSocket;
	/** JFrame holding all the GUI */
	private ClientWindow clientWindow;
	/** Aimed at sending messages to the server  */
	PrintWriter wr;
	/** Aimed at receiving messages from the server */
	private BufferedReader rd;
	/** Separate thread to receive messages from the server or other clients */
	ClientReader clientReader;
	/** Server-side part of the client, to provide the interface for other clients to download shared files */
	ClientServer clientServer;
	/** Socket used when downloading a file. The socket is connected to the server part of a remote client */
	Socket downloadSocket;
	/** The path to the directory where a downloaded file will be stored */
	private String pathForDownloadedFile;
	/** Separate thread to regularly check for new or removed files in the shared folder */
	private ClientProbe clientProbe;
	

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
	public Client(String sharedFilePath, String address, String port, String name, String downloadPort) throws IOException {
		this.setSharedFilePath(sharedFilePath);

		this.setAddress(address);

		this.setPort(port);

		this.setName(name);
		
		this.setDownloadPort(downloadPort);

		setClientWindow(new ClientWindow(this));
		getClientWindow().run();
		
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
	protected void getFileNames(HashMap<String, String> fileNameList, String sharedFilePath, String pathPrefix) throws IOException {

		File file = new File(sharedFilePath);
		File[] arrayFilesOrDirectories = file.listFiles();

		for(int i = 0; i < arrayFilesOrDirectories.length; i++){
			if(arrayFilesOrDirectories[i].isFile() && ! arrayFilesOrDirectories[i].isHidden()){
				String mediaType = tika.detect(arrayFilesOrDirectories[i]).split("/")[0];
				getFilesList().put(arrayFilesOrDirectories[i].getName() + "&" + mediaType, pathPrefix);
			}
			else if(arrayFilesOrDirectories[i].isDirectory()){
				String directoryPath = arrayFilesOrDirectories[i].getPath();
				getFileNames(fileNameList, directoryPath, pathPrefix + arrayFilesOrDirectories[i].getName() + "/");
			}
		}

	}
	
	/**
	 * Given a file name and type (i.e. the key), returns the file path
	 * @param fileNameAndType
	 * @return The path to the file
	 */
	public String findPathToFile(String fileNameAndType){
		return getFilesList().get(fileNameAndType);
	}

	/**
	 * Attempts to connect to the server with the creationSocket socket
	 * 
	 * @return true if the socket was successfully created, false otherwise
	 */
	private boolean creationSocket(){

		try {
			clientSocket = new Socket();
			clientSocket.connect(new InetSocketAddress(address, Integer.parseInt(port)), 1000);
			wr = new PrintWriter(clientSocket.getOutputStream());
			rd = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));

		} catch (SocketTimeoutException e) {
			return false;
		} catch (UnknownHostException e) {
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	/**
	 * Sends a "register" message to the server, containing the client's file names, types, and port for its server part
	 * 
	 * @throws IOException
	 */
	public void share() throws IOException{
		
		getFileNames(getFilesList(), sharedFilePath, "");

		creationSocket();
		
		clientServer = new ClientServer(this, getDownloadPort(), getClientWindow());
		new Thread(clientServer).start();

		new Thread(new ClientReader(this, getClientWindow(), rd)).start();
		
		String registerMessage = "register:";

		if(getFilesList().size() == 0){
			registerMessage += " ";
		}
		for(Entry<String, String> entry : getFilesList().entrySet()){
			registerMessage += entry.getKey() +",";
		}

		registerMessage = registerMessage.substring(0, registerMessage.length() - 1);
		registerMessage += ":"+getName();
		registerMessage += ":"+getDownloadPort();
		wr.println(registerMessage);
		wr.flush();
		
		clientProbe = new ClientProbe(this);
		new Thread(clientProbe).start();
	}

	/**
	 * Sends an "unregister" message to the server
	 * @throws IOException
	 */
	public void unshare() throws IOException{

		String unregisterMessage = "unregister";
		wr.println(unregisterMessage);
		wr.flush();
		
		clientServer.closeSocket();
		clientSocket.close();
	}

	/**
	 * Sends a "request" message to the server. The request message is composed of one or more keywords, and optionnally
	 * a file type and a client name (not implemented yet).
	 * 
	 * @param keywords The keywords for the search
	 * @param fileType The file type required by the client search ("" matches all files) 
	 * @param clientName The remote client required by the client search (not implemented yet)
	 * @throws UnknownHostException 
	 */
	public void request(String[] keywords, String fileType, String clientName) throws UnknownHostException{

		String requestMessage = "request:";
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
		
		wr.println(requestMessage);
		wr.flush();
	}
	
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
	public void download(String fileName, String address, String downloadPort, String pathForDownloadedFile){
		
		this.setPathForDownloadedFile(pathForDownloadedFile);
		
		PrintWriter dwr = null;
		BufferedReader drd = null;
		
		// Creates the socket to the client that has the requested file
		try {
			downloadSocket = new Socket();
			downloadSocket.connect(new InetSocketAddress(address, Integer.parseInt(downloadPort)), 1000);
			dwr = new PrintWriter(downloadSocket.getOutputStream());
			drd = new BufferedReader( new InputStreamReader(downloadSocket.getInputStream()));
		} catch (SocketTimeoutException e) {
			System.err.println(e);
		} catch (UnknownHostException e) {
			System.err.println(e);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		new Thread(new ClientReader(this, getClientWindow(), drd)).start();
		
		dwr.println("download:" + fileName);
		dwr.flush();
		
	}
	
	/**
	 * Adds a new file to the hashmap (when a new file is detected by the ClientProbe)
	 * @param string The file name and type of the file
	 * @param pathPrefix The path (relative to the shared directory) to the file
	 */
	public void addFile(String string, String pathPrefix) {
		getFilesList().put(string, pathPrefix);
		wr.println("addfile:" + string);
		wr.flush();
	}
	
	/**
	 * Deletes a file from the hashmap (when a removed file is detected by the ClientProbe)
	 * @param string
	 */
	public void removeFile(String string){
		for(Entry<String, String> entry : getFilesList().entrySet()){
			if(entry.getKey().startsWith(string + "&")){
				getFilesList().remove(entry.getKey());
				wr.println("deletefile:" + string);
				wr.flush();
				break;
			}
		}
	}
	


	/**
	 * Asks the server ("isavailable") if a particular file from a particular client is available for download
	 * 
	 * @param fileName The name of the file
	 * @param remoteClientName The name of the remote client
	 * @param port2 
	 * @param address2 
	 */
	public void checkDownload(String fileName, String remoteClientName, String address, String port) throws NumberFormatException, UnknownHostException, IOException{
		wr.println("isavailable:" + fileName + ":" + remoteClientName);
		wr.flush();
		getClientWindow().setWaitingCursor();
	}
	
	/**
	 * Notifies the GUI that a download has begun
	 * 
	 * @param fileName The name of the file
	 * @param remoteClientName The name of the remote client
	 */
	public void startDownload(String fileName, String remoteClientName){
		getClientWindow().startDownload(fileName, remoteClientName);
	}
	
	/**
	 * Notifies the GUI that the requested file is not available
	 * 
	 * @param fileName The name of the file
	 * @param remoteClientName The name of the remote client
	 */
	public void fileNotAvailable(String fileName, String remoteClientName){
		getClientWindow().fileNotAvailable(fileName, remoteClientName);
	}

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

		new Client(sharedFilePath, address, port, name, downloadPort);

	}

	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}


	public void setAddress(String address) {
		this.address = address;
	}

	public String getPort() {
		return port;
	}


	public void setPort(String port) {
		this.port = port;
	}

	public String getDownloadPort() {
		return downloadPort;
	}


	public void setDownloadPort(String downloadPort) {
		this.downloadPort = downloadPort;
	}

	public String getSharedFilePath() {
		return sharedFilePath;
	}


	public void setSharedFilePath(String sharedFilePath) {
		this.sharedFilePath = sharedFilePath;
	}

	public String getPathForDownloadedFile() {
		return pathForDownloadedFile;
	}


	public void setPathForDownloadedFile(String pathForDownloadedFile) {
		this.pathForDownloadedFile = pathForDownloadedFile;
	}

	public HashMap<String, String> getFilesList() {
		return filesList;
	}


	public void setFilesList(HashMap<String, String> filesList) {
		this.filesList = filesList;
	}

	public ClientWindow getClientWindow() {
		return clientWindow;
	}


	public void setClientWindow(ClientWindow clientWindow) {
		this.clientWindow = clientWindow;
	}

	public static final String SHAREDFILEPATH = "shared/";
	public static final String ADDRESS = "localhost";
	public static final String PORT = "10000";

}
