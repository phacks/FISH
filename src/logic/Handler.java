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


public class Handler extends Thread{
	Socket socket;
	BufferedReader rd;
	PrintWriter wr;

	Connection connection;

	PreparedStatement registerClientStatement;
	PreparedStatement findClientStatement;
	PreparedStatement unregisterClientStatement;
	private PreparedStatement addFileStatement;
	private PreparedStatement findFilesFromClientNameStatement;
	private PreparedStatement deleteFilesStatement;
	String clientName;
	private PreparedStatement findFilesFromKeywordsStatement;
	private PreparedStatement findFilesFromFileTypeStatement;
	private PreparedStatement countFilesFromKeywordsStatement;
	private PreparedStatement findFileStatement;
	private PreparedStatement findFileOwnerStatement;

	Handler(Socket socket, Connection connection) throws IOException, SQLException { // thread constructor
		this.socket = socket;
		this.connection = connection;

		prepareStatements(connection);

		rd = new BufferedReader( new InputStreamReader(socket.getInputStream()));
		wr = new PrintWriter(socket.getOutputStream());
	}

	public void run(){

		String str;
		try {
			while ((str = rd.readLine()) != null){
				String[] parseCommand = str.split(":");
				String command = parseCommand[0];

				if (command.equals("register")){
					String[] parseFiles = parseCommand[1].split(",");
					clientName = parseCommand[2];

					String address = socket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
					int downloadPort = Integer.parseInt(parseCommand[3]);

					registerClient(clientName, address, downloadPort);

					addFiles(clientName, parseFiles);

					wr.println("registered");
					wr.flush();
				}

				if (command.equals("unregister")){
					deleteFiles(clientName);
					unregisterClient(clientName);
				}

				if (command.equals("request")){
					String[] keywords = parseCommand[1].split(",");

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
			}


		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


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


	private void send(String message) {

		wr.println(message);
		wr.flush();

	}

	private void unregisterClient(String clientName2) throws SQLException {
		unregisterClientStatement.setString(1, clientName);
		int rows = unregisterClientStatement.executeUpdate();
		if (rows > 0) {
			//System.out.println(rows);
		} else {
			System.err.println("Error : " + clientName + " files could not be deleted");
		}
	}

	private void deleteFiles(String clientName2) throws SQLException {

		deleteFilesStatement.setString(1, clientName);
		int rows = deleteFilesStatement.executeUpdate();
		if (rows > 0) {
			//System.out.println(rows);
		} else {
			System.err.println("Error : " + clientName + " files could not be deleted");
		}

	}

	private void addFiles(String clientName, String[] parseFiles) throws SQLException {

		for(String file : parseFiles){
			String fileName = file.split("&")[0];
			String fileType = file.split("&")[1];

			addFileStatement.setString(1, fileName);
			addFileStatement.setString(2, fileType);
			addFileStatement.setString(3, clientName);

			int rows = addFileStatement.executeUpdate();
			if (rows == 1) {
				// System.out.println(clientName + " has been registered");
			} else {
				System.err.println("Error : " + clientName + " could not be registered");
			}
		}
	}


	private void registerClient(String clientName, String address, int downloadPort) throws Exception {
		findClientStatement.setString(1, clientName);

		ResultSet result = null;
		result = findClientStatement.executeQuery();

		if (result.next()) {
			// account exists, instantiate, put in cache and throw exception.
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

	private void prepareStatements(Connection connection) throws SQLException {
		registerClientStatement = connection.prepareStatement("INSERT INTO USERS VALUES (?, ?, ?)");
		findClientStatement = connection.prepareStatement("SELECT * from USERS WHERE CLIENTNAME = ?");
		unregisterClientStatement = connection.prepareStatement("DELETE FROM USERS WHERE CLIENTNAME = ?");

		addFileStatement = connection.prepareStatement("INSERT INTO FILES (filename, type, clientname) VALUES (?, ?, ?)");
		findFilesFromClientNameStatement = connection.prepareStatement("SELECT * from FILES WHERE CLIENTNAME = ?");
		findFileOwnerStatement = connection.prepareStatement("SELECT CLIENTNAME from FILES WHERE FILENAME = ?");
		//findFilesFromFileTypeStatement = connection.prepareStatement("SELECT * from FILES WHERE FILENAME = %?%");
		deleteFilesStatement = connection.prepareStatement("DELETE FROM FILES WHERE CLIENTNAME = ?");
	}


}
