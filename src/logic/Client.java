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
import java.util.Iterator;
import java.util.LinkedList;
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
 * 		"reply:found:filename1&filetype1&clientname1&address1&downloadport1,...,filenameN&filetypeN&clientnameN&addressN&downloadportN" (more than one file match the request)
 * 
 * - [IMPLEMENTED] The client unregisters at the server (when the client stops sharing files) :
 * 		"unregister:clientname"
 * 
 * - A client asks a specific file to another client for downloading it
 * 		"download:filename"
 * 
 * - The other client gives the size of the file, and start sending out the file bytes :
 * 		"download:filename:filesize\nfilebytes" (\n stands for a new line)
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


public class Client {

	private String sharedFilePath;
	private String address;
	private String port;
	private String name;
	private String downloadPort;
	HashMap<String, String> filesList = new HashMap<String, String>();
	Tika tika = new Tika();
	Socket clientSocket;
	ClientWindow clientWindow;
	PrintWriter wr;
	private BufferedReader rd;
	ClientReader clientReader;
	ClientServer clientServer;
	Socket downloadSocket;


	public Client(String sharedFilePath, String address, String port, String name, String downloadPort) throws IOException {
		this.setSharedFilePath(sharedFilePath);

		this.setAddress(address);

		this.setPort(port);

		this.setName(name);
		
		this.setDownloadPort(downloadPort);

		clientWindow = new ClientWindow(this);
		clientWindow.run();
		
	}


	private void getFileNames(HashMap<String, String> fileNameList, String sharedFilePath, String pathPrefix) throws IOException {

		File file = new File(sharedFilePath);
		File[] arrayFilesOrDirectories = file.listFiles();

		for(int i = 0; i < arrayFilesOrDirectories.length; i++){
			if(arrayFilesOrDirectories[i].isFile() && ! arrayFilesOrDirectories[i].isHidden()){
				String mediaType = tika.detect(arrayFilesOrDirectories[i]).split("/")[0];
				filesList.put(arrayFilesOrDirectories[i].getName() + "&" + mediaType, pathPrefix);
			}
			else if(arrayFilesOrDirectories[i].isDirectory()){
				String directoryPath = arrayFilesOrDirectories[i].getPath();
				getFileNames(fileNameList, directoryPath, pathPrefix + arrayFilesOrDirectories[i].getName() + "/");
			}
		}

	}
	
	public String findPathToFile(String fileNameAndType){
		return filesList.get(fileNameAndType);
	}

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

	public void share() throws IOException{
		
		getFileNames(filesList, sharedFilePath, "");

		creationSocket();
		
		clientServer = new ClientServer(this, getDownloadPort());
		new Thread(clientServer).start();

		new Thread(new ClientReader(this, clientWindow, rd)).start();
		
		String registerMessage = "register:";
		//Iterator<String> it = filesList.iterator();
		if(filesList.size() == 0){
			registerMessage += " ";
		}
		for(Entry<String, String> entry : filesList.entrySet()){
			registerMessage += entry.getKey() +",";
		}
//		while(it.hasNext()){
//			String s = it.next();
//			registerMessage += s+",";
//		}
		registerMessage = registerMessage.substring(0, registerMessage.length() - 1);
		registerMessage += ":"+getName();
		registerMessage += ":"+getDownloadPort();
		wr.println(registerMessage);
		wr.flush();
	}

	public void unshare() throws IOException{

		String unregisterMessage = "unregister";
		wr.println(unregisterMessage);
		wr.flush();
		
		clientServer.closeSocket();
		clientSocket.close();
	}

	public void request(String[] keywords, String fileType, String clientName){

		String requestMessage = "request:";
		for (String keyword : keywords){
			requestMessage += keyword + ",";
		}
		requestMessage = requestMessage.substring(0, requestMessage.length() - 1);

		if(! fileType.equals("")){
			requestMessage+= ":type=" + fileType;
		}
		if(! clientName.equals("")){
			requestMessage+= ":client=" + clientName;
		}

		wr.println(requestMessage);
		wr.flush();
	}
	
	public void download(String fileName, String address, String downloadPort){
		
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
		
		new Thread(new ClientReader(this, clientWindow, drd)).start();
		
		dwr.println("download:" + fileName);
		dwr.flush();
		
	}

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

	public static final String SHAREDFILEPATH = "shared/";
	public static final String ADDRESS = "localhost";
	public static final String PORT = "10000";
}
