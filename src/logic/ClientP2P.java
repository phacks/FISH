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
import java.util.Map.Entry;



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
	
	public void checkDownload(String name, String clientName, String address, String port) throws NumberFormatException, UnknownHostException, IOException{
		
		Socket tempSocket = new Socket(address, Integer.parseInt(port));
		PrintWriter tempWriter = new PrintWriter(tempSocket.getOutputStream());
		BufferedReader tempReader = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()));
		tempWriter.println("isavailable:" + name + ":" + clientName);
		tempWriter.flush();
		new ClientReader(this, getClientWindow(), tempReader).run();
		
		getClientWindow().setWaitingCursor();
	}
	
	public boolean isAvailable(String fileName) {
		Set<String> setKeys =  filesList.keySet();
		Iterator<String> it = setKeys.iterator();
		while(it.hasNext()){
			if(it.next().split("&")[0].equals(fileName)){
				return true;
			}
		}
		
		return false;
	}

	
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
	 * Adds a new file to the hashmap (when a new file is detected by the ClientProbe)
	 * @param string The file name and type of the file
	 * @param pathPrefix The path (relative to the shared directory) to the file
	 */
	public void addFile(String string, String pathPrefix) {
		getFilesList().put(string, pathPrefix);
	}
	
	/**
	 * Deletes a file from the hashmap (when a removed file is detected by the ClientProbe)
	 * @param string
	 */
	public void removeFile(String string){
		for(Entry<String, String> entry : getFilesList().entrySet()){
			if(entry.getKey().startsWith(string + "&")){
				getFilesList().remove(entry.getKey());
				break;
			}
		}
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
