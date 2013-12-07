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
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.tika.Tika;

/*
 * Client-Server Protocol
 * ----------------------
 * 
 * MANDATORY PART
 * 
 * - [IMPLEMENTED] A client registers at the server by sending a list of shared file names :
 * 		"register:filename1&filetype1,filename2&filetype2,...,filenameN&filetypeN:clientname"
 * 
 * - [IMPLEMENTED] The server confirms the client is registered :
 * 		"registered"
 * 
 * - A client sends to the server a search request for keywords, and optionnaly specify a file type or a client name :
 * 		"request:keyword1,keyword2,...,keywordN:(type=filetype):(client=clientname)"
 * 
 * - The server replies to the requesting client by sending either a 'not found' message or addresses of client(s) sharing the requested file :
 * 		"reply:notfound"
 * 		"reply:found:filename1&filetype1,filename2&filetype2,...,filenameN&filetypeN" (more than one file match the request)
 * 
 * - [IMPLEMENTED] The client unregisters at the server (when the client stops sharing files) :
 * 		"unregister:clientname"
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

	String sharedFilePath;
	private String address;
	private String port;
	private String name;
	LinkedList<String> fileNameList = new LinkedList<String>();
	Tika tika = new Tika();
	Socket clientSocket;
	ClientWindow clientWindow;
	PrintWriter wr;
	private BufferedReader rd;


	public Client(String sharedFilePath, String address, String port, String name) throws IOException {
		this.sharedFilePath = sharedFilePath;

		this.setAddress(address);

		this.setPort(port);

		this.setName(name);

		getFileNames(fileNameList, sharedFilePath, "");

		clientWindow = new ClientWindow(this);
		clientWindow.run();
		
	}


	private void getFileNames(LinkedList<String> fileNameList, String sharedFilePath, String pathPrefix) throws IOException {

		File file = new File(sharedFilePath);
		File[] arrayFilesOrDirectories = file.listFiles();

		for(int i = 0; i < arrayFilesOrDirectories.length; i++){
			if(arrayFilesOrDirectories[i].isFile() && ! arrayFilesOrDirectories[i].isHidden()){
				String mediaType = tika.detect(arrayFilesOrDirectories[i]).split("/")[0];
				fileNameList.add(arrayFilesOrDirectories[i].getName() + "&" + mediaType);
			}
			else if(arrayFilesOrDirectories[i].isDirectory()){
				String directoryPath = arrayFilesOrDirectories[i].getPath();
				getFileNames(fileNameList, directoryPath, pathPrefix + arrayFilesOrDirectories[i].getName() + "/");
			}
		}

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

		if (! creationSocket()){
			// TODO Generate a popup if the connexion failed
		}
		String registerMessage = "register:";
		Iterator<String> it = fileNameList.iterator();
		while(it.hasNext()){
			String s = it.next();
			registerMessage += s+",";
		}
		registerMessage = registerMessage.substring(0, registerMessage.length() - 1);
		registerMessage += ":"+getName();
		wr.println(registerMessage);
		wr.flush();
		
		String str = rd.readLine();
		
		System.out.println(str);

	}

	public void unshare() throws IOException{

		String unregisterMessage = "unregister";
		wr.println(unregisterMessage);
		wr.flush();
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

		System.out.println(requestMessage);
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
			address = args[1];
		} else {
			address = ADDRESS;
		}

		String port = null;
		if (args.length > 2) {
			port = args[2];
		} else {
			port = PORT;
		}

		String name = null;
		if (args.length > 3) {
			name = args[3];
		} else {
			name = "Client " + Integer.toString((int) Math.ceil(Math.random()*1000));
		}

		new Client(sharedFilePath, address, port, name);

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

	public static final String SHAREDFILEPATH = "shared/";
	public static final String ADDRESS = "localhost";
	public static final String PORT = "10000";
}