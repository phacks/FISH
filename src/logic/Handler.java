package logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Handles a client communications with the server through sockets. The Handler can register or unregister a client,
 * add, delete, or check the availability of particular files in the database.
 * 
 * @param socket The socket used to communicate with the client
 * @param connection The connection to the datasource
 * 
 * @see Client
 * @see Server
 *
 */
public class Handler extends Thread{
	
	/** The socket used to communicate with the client */
	Socket socket;
	/** Used to read incoming messages from the client */
	BufferedReader rd;
	/** Used to send messages to the client */
	PrintWriter wr;
	/** The connection to the datasource */
	Connection connection;
	/** A SQL statement aiming at adding a client in the database */
	PreparedStatement registerClientStatement;
	/** A SQL statement aiming at finding a client in the database */
	PreparedStatement findClientStatement;
	/** A SQL statement aiming at deleting a client in the database */
	PreparedStatement unregisterClientStatement;
	/** A SQL statement aiming at adding a file in the database */
	private PreparedStatement addFileStatement;
	/** A SQL statement aiming at deleting a client's files in the database */
	private PreparedStatement deleteFilesStatement;
	/** The name of the client with whom the Handler is communicating */
	String clientName;
	/** A SQL statement aiming at finding files matching one or more keywords */
	private PreparedStatement findFilesFromKeywordsStatement;
	/** A SQL statement aiming at deleting a file in the database */
	private PreparedStatement deleteFileStatement;
	/** A SQL statement aiming at finding a file in the database */
	private PreparedStatement findFileStatement;

	/**
	 * Instanciates the handler, prepares all the statements and creates the BufferedReader and PrintWriter
	 * 
	 * @param socket The socket used to communicate with the client
	 * @param connection The connection to the datasource
	 * @throws IOException
	 * @throws SQLException
	 */
	Handler(Socket socket, Connection connection) throws IOException, SQLException { // thread constructor
		this.socket = socket;
		this.connection = connection;

		prepareStatements(connection);

		rd = new BufferedReader( new InputStreamReader(socket.getInputStream()));
		wr = new PrintWriter(socket.getOutputStream());
	}

	/** 
	 * Read incoming messages from the client, parses them and call the appropriate methods to 
	 * reply and/or execute actions.
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run(){

		String str;
		try {
			while ((str = rd.readLine()) != null){
				String[] parseCommand = str.split(":");
				String command = parseCommand[0];
				
				/* If the client wants to register :
				 * incoming message : "register:filename1&filetype1,filename2&filetype2,...,filenameN&filetypeN:clientname:downloadport"
				 * reply : "registered"
				 */
				if (command.equals("register")){ 
					String[] parseFiles = parseCommand[1].split(",");
					clientName = parseCommand[2];

					String address = socket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
					int downloadPort = Integer.parseInt(parseCommand[3]);

					registerClient(address, downloadPort);

					addFiles(parseFiles);

					wr.println("registered");
					wr.flush();
				}

				/* If the client wants to unregister :
				 * incoming message : "unregister"
				 */
				if (command.equals("unregister")){
					deleteFiles();
					unregisterClient();
				}

				/* If the client wants to request a file (one or many keywords and/or a file type and/or a client name :
				 * /!\ The search by client name has not been yet implemented /!\
				 * incoming message : "request:keyword1,keyword2,...,keywordN:(type=filetype):(client=clientname)"
				 * reply : "reply:notfound"
				 * 	  	   "reply:found:filename1&filetype1&clientname1&address1&downloadport1,...,filenameN&filetypeN&clientnameN&addressN&downloadportN"
				 */
				if (command.equals("request")){
					String[] keywords;
					if (parseCommand[1].equals(" ")){
						keywords = new String[1];
						keywords[0] = ""; 
					}
					else{
						keywords = parseCommand[1].split(",");
					}

					if (parseCommand.length == 2){
						fileSearch(keywords, "", "");
					}

					else if(parseCommand.length == 3){
						if (parseCommand[2].contains("type")){
							String fileType = parseCommand[2].split("=")[1]; 

							fileSearch(keywords, fileType, "");
						}

						else if (parseCommand[2].contains("client")){
							String clientNameRequest = parseCommand[2].split("=")[1]; 

							fileSearch(keywords, "", clientNameRequest);
						}

						else{
							System.err.println("Request command not valid");
						}
					}

					else if (parseCommand.length == 4){
						String fileType = parseCommand[3].split("=")[1];
						String clientNameRequest = parseCommand[4].split("=")[1];

						fileSearch(keywords, fileType, clientNameRequest);
					}

					else{
						System.err.println("Request command not valid");
					}
				}

				/* If the client wants to share a new file :
				 * incoming message : "addfile:filename&fileType"
				 */
				if (command.equals("addfile")){
					String fileName = str.split(":")[1].split("&")[0];
					String fileType = str.split(":")[1].split("&")[1];
					addFile(fileName, fileType);
				}

				/* If the client wants to stop sharing a file :
				 * incoming message : "deletefile:filename"
				 */
				if (command.equals("deletefile")){
					String fileName = str.split(":")[1].split("&")[0];
					deleteFile(fileName);
				}
				
				/* If the client wants to know if a file is available before starting a download :
				 * incoming message : "isavailable:filename:remoteclientname"
				 * reply: "isavailable:yes:filename:clientname"
				 * 		  "isavailable:no:filename:clientname"
				 */
				if(command.equals("isavailable")){
					String fileName = parseCommand[1];
					String remoteClientName = parseCommand[2];
					
					isAvailable(fileName, remoteClientName);
				}
			}


		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	/**
	 * Checks if a particular file is available for downloading (i.e. present in the database). If so, 
	 * a message is sent to the requesting client.
	 * 
	 * @param fileName The name of the requested file
	 * @param remoteClientName The name of the remote client the client whishes to download the file from 
	 * @throws SQLException
	 */
	private void isAvailable(String fileName, String remoteClientName) throws SQLException {
		findFileStatement.setString(1, remoteClientName);
		findFileStatement.setString(2, fileName);
		
		ResultSet result = findFileStatement.executeQuery();
		String reply = "isavailable:";
		
		if (result.next()){
			reply += "yes";
		}
		else{
			reply += "no";
		}
		
		reply += ":" + fileName + ":" + remoteClientName;
		
		send(reply);
	}

	/**
	 * Adds a new entry in the database corresponding to a newly shared file
	 * 
	 * @param fileName The name of the newly shared file
	 * @param fileType The type of the newly shared file
	 * @throws SQLException
	 */
	private void addFile(String fileName, String fileType) throws SQLException {

		addFileStatement.setString(1, fileName);
		addFileStatement.setString(2, fileType);
		addFileStatement.setString(3, clientName);

		int rows = addFileStatement.executeUpdate();
		if (rows == 1) {
			System.out.println("File " + fileName + " from " + clientName + " has been added to the nameserver");
		} else {
			System.err.println("File " + fileName + " from " + clientName + " cannot be added to the nameserver");
		}

	}
	
	/**
	 * Deletes an entry in the database corresponding to a file that is not shared anymore.
	 * 
	 * @param fileName The name of the file that is not shared anymore
	 * @throws SQLException
	 */
	private void deleteFile(String fileName) throws SQLException {

		deleteFileStatement.setString(1, clientName);
		deleteFileStatement.setString(2, fileName);
		int rows = deleteFileStatement.executeUpdate();
		if (rows > 0) {
			System.out.println("File " + fileName + " from " + clientName + " has been deleted from the nameserver");
		} else {
			System.err.println("File " + fileName + " from " + clientName + " cannot be be deleted from the nameserver");
		}

	}

	/**
	 * Search the database for files matching with the keywords, file type and client name (not implemented yet).
	 * One done, the reply is built and sent to the requesting client.
	 * 
	 * @param keywords The keywords the client is searching for
	 * @param fileType The type the files have to be 
	 * @param clientName The client the files have to belong
	 * @throws Exception
	 */
	private void fileSearch(String[] keywords, String fileType, String clientName) throws Exception {

		String findStatement = "SELECT * FROM FILES WHERE FILENAME LIKE '%" + keywords[0] + "%' AND CLIENTNAME != '" + this.clientName + "'";

		if (keywords.length > 1){
			for (int i=1; i < keywords.length; i++){
				findStatement += " INTERSECT SELECT * FROM FILES WHERE FILENAME LIKE '%" + keywords[i] + "%' AND CLIENTNAME != '" + this.clientName + "'";
			}
		}

		if (! fileType.equals("")){
			findStatement += " INTERSECT SELECT * FROM FILES WHERE TYPE = '" + fileType + "' AND CLIENTNAME != '" + this.clientName + "'";
		}

		if (! clientName.equals("")){
			findStatement += " INTERSECT SELECT * FROM FILES WHERE CLIENTNAME = '" + clientName + "'";
		}

		findFilesFromKeywordsStatement = connection.prepareStatement(findStatement);
		ResultSet result = findFilesFromKeywordsStatement.executeQuery();


		String reply = "reply:";

		if (result.next()){

			findClientStatement.setString(1, result.getString("CLIENTNAME"));
			ResultSet resultAddress = findClientStatement.executeQuery();

			String ownerAddress = "";
			int ownerDownloadPort;

			resultAddress.next();
			ownerAddress = resultAddress.getString("ADDRESS");
			ownerDownloadPort = resultAddress.getInt("PORT");


			reply += "found:" + result.getString("FILENAME") + "&" + result.getString("TYPE") + "&" + result.getString("CLIENTNAME") + "&" + ownerAddress + "&" + ownerDownloadPort + ",";
			while(result.next()){

				findClientStatement.setString(1, result.getString("CLIENTNAME"));
				resultAddress = findClientStatement.executeQuery();

				resultAddress.next();
				ownerAddress = resultAddress.getString("ADDRESS");
				ownerDownloadPort = resultAddress.getInt("PORT");

				reply += result.getString("FILENAME") + "&" + result.getString("TYPE") + "&" + result.getString("CLIENTNAME") + "&" + ownerAddress + "&" + ownerDownloadPort + ",";
			}
			reply = reply.substring(0, reply.length() - 1);
		}
		else{
			reply += "notfound";
		}

		send(reply);
	}


	/**
	 * Sends a message to the client through the socket
	 * @param message The message to be sent
	 */
	private void send(String message) {

		wr.println(message);
		wr.flush();

	}

	/**
	 * Deletes the client from the users database
	 * @throws SQLException
	 */
	private void unregisterClient() throws SQLException {
		unregisterClientStatement.setString(1, clientName);
		int rows = unregisterClientStatement.executeUpdate();
		if (rows > 0) {
			System.out.println(clientName + " has been unregistered");
		} else {
			System.err.println("Error : " + clientName + " cannot be unregistered");
		}
	}

	/**
	 * Deletes the client's files from the files database
	 * @throws SQLException
	 */
	private void deleteFiles() throws SQLException {

		deleteFilesStatement.setString(1, clientName);
		int rows = deleteFilesStatement.executeUpdate();
		if (rows > 0) {
			// System.out.println(clientName + " has been unregistered");
		} else {
			System.err.println("Error : " + clientName + " files could not be deleted");
		}

	}

	/**
	 * Add the client's files the the files database
	 * @param parseFiles The list of file names and types
	 * @throws SQLException
	 */
	private void addFiles(String[] parseFiles) throws SQLException {

		for(String file : parseFiles){
			if (! file.equals("")){
				String fileName = file.split("&")[0];
				String fileType = file.split("&")[1];

				addFileStatement.setString(1, fileName);
				addFileStatement.setString(2, fileType);
				addFileStatement.setString(3, clientName);

				int rows = addFileStatement.executeUpdate();
				if (rows == 1) {
					// System.out.println(clientName + " has been registered");
				} else {
					System.err.println("Error : " + clientName + " files cannot be added");
				}
			}
		}
	}


	/**
	 * Adds a client to the users database
	 * 
	 * @param address The IP address of the client
	 * @param downloadPort The Download port of the client
	 * @throws Exception
	 */
	private void registerClient(String address, int downloadPort) throws Exception {
		findClientStatement.setString(1, clientName);

		ResultSet result = null;
		result = findClientStatement.executeQuery();

		if (result.next()) {
			throw new Exception("Error : " + clientName + " is already registered");
		}
		else{
			result.close();

			registerClientStatement.setString(1, clientName);
			registerClientStatement.setString(2, address);
			registerClientStatement.setInt(3, downloadPort);



			int rows = registerClientStatement.executeUpdate();
			if (rows == 1) {
				System.out.println(clientName + " has been registered");
			} else {
				System.err.println("Error : " + clientName + " could not be registered");
			}
		}		
	}

	/**
	 * Prepare all the SQL statements
	 * 
	 * @param connection The connection to the datasource
	 * @throws SQLException
	 */
	private void prepareStatements(Connection connection) throws SQLException {
		registerClientStatement = connection.prepareStatement("INSERT INTO USERS VALUES (?, ?, ?)");
		findClientStatement = connection.prepareStatement("SELECT * from USERS WHERE CLIENTNAME = ?");
		unregisterClientStatement = connection.prepareStatement("DELETE FROM USERS WHERE CLIENTNAME = ?");

		findFileStatement = connection.prepareStatement("SELECT * from FILES WHERE CLIENTNAME = ? AND FILENAME = ?");
		
		addFileStatement = connection.prepareStatement("INSERT INTO FILES (filename, type, clientname) VALUES (?, ?, ?)");
		deleteFileStatement = connection.prepareStatement("DELETE FROM FILES WHERE CLIENTNAME = ? AND FILENAME = ?");
		deleteFilesStatement = connection.prepareStatement("DELETE FROM FILES WHERE CLIENTNAME = ?");
	}


}
