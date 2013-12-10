package logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Detects and handles the clients crashes. This thread is launched by the server in its constructor.
 * To detect crashes, it tries to connect to the server part of a client, and sends a "ping" message.
 * If the connection failed, or the reply was something other than "pong", the client is considered crashed.
 * It is then removed, along with its files, from the database.
 * 
 * @param connection The connection to the datasource
 * @see Server
 * @see Client
 *
 */
public class ClientFailureDetector implements Runnable {
	
	/** The connection to the datasource */
	Connection connection;
	/** A SQL statement aiming at finding all the clients in the database */
	private PreparedStatement findClientsStatement;
	/** The socket used to communicate with the server side of the client */
	Socket socket;
	/** The BufferedReader used to send messages to the client */
	PrintWriter wr;
	/** The BufferedReader used to read incoming messages from the client */
	BufferedReader rd;
	/** A SQL statement aiming at deleting a client in the database */
	PreparedStatement unregisterClientStatement;
	/** The name of the client which is being tested */
	String clientName;
	/** The IP address of the client which is being tested */
	String address;
	/** The download port of the client which is being tested */
	int downloadPort;
	/** A SQL statement aiming at deleting a client's files in the database */	
	private PreparedStatement deleteFilesStatement;
	/** The time after which all clients are pinged again */
	private final int UPDATE_TIME = 5000;

	/**
	 * Prepares the SQL statements
	 * 
	 * @param connection
	 * @throws SQLException
	 */
	public ClientFailureDetector(Connection connection) throws SQLException {
		this.connection = connection;
		findClientsStatement = connection.prepareStatement("SELECT * from USERS");
		unregisterClientStatement = connection.prepareStatement("DELETE FROM USERS WHERE CLIENTNAME = ?");
		deleteFilesStatement = connection.prepareStatement("DELETE FROM FILES WHERE CLIENTNAME = ?");
	}

	/** 
	 * All the clients in the databased are pinged. If the client is detected as crashed, 
	 * he and his files are removed from the database. This operation is repeated constantly, 
	 * and sleeps for a given mount of time defined by the UPDATE_TIME constant
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(true){
			try {
				Thread.sleep(UPDATE_TIME);
				ResultSet result = findClientsStatement.executeQuery(); // Get all registered clients
				
				while(result.next()){
					clientName = result.getString("CLIENTNAME");
					address = result.getString("ADDRESS");
					downloadPort = result.getInt("PORT");
					
					pingClient(address, downloadPort);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) { // If the ping failed, the client has crashed
				try {
					deleteFilesStatement.setString(1, clientName);
					int rows = deleteFilesStatement.executeUpdate();
					if (rows > 0) {
						// System.out.println(clientName + " has been unregistered");
					} else {
						System.err.println("Error : " + clientName + " files could not be deleted");
					}
					
					unregisterClientStatement.setString(1, clientName);
					rows = unregisterClientStatement.executeUpdate();
					if (rows > 0) {
						System.out.println(clientName + " has crashed and has been unregistered");
					} else {
						System.err.println("Error : " + clientName + " cannot be unregistered");
					}
					
					
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				//e.printStackTrace();
			}
		}
	}

	/**
	 * Pings a particular client by trying to connect to its server-side port, and sending 
	 * a "ping" message. If the connexion fails or the reply is not "pong", the client is considered as crashed 
	 * and handled in an IO Exception.
	 * 
	 * @param address The IP address of the client
	 * @param downloadPort The server-side port of the client
	 * @throws IOException The exception will trigger the unregistration process
	 */
	private void pingClient(String address, int downloadPort) throws IOException {
		socket = new Socket();
		socket.connect(new InetSocketAddress(address, downloadPort), 1000);
		wr = new PrintWriter(socket.getOutputStream());
		rd = new BufferedReader( new InputStreamReader(socket.getInputStream()));
		
		wr.println("ping");
		wr.flush();
		
		if(! rd.readLine().equals("pong")){
			throw new IOException();
		}
		else{
			socket.close();
		}
	}

}
