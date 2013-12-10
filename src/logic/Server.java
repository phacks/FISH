package logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import org.apache.derby.jdbc.ClientDriver;


/**
 * Server managing a directory of shared file names. Connections with clients are made through sockets,
 * and each new connection creates a new Handler instance. The file names are stored in a table (using Java DB),
 * and registered users are stored in a separate one. An instance of ClientFailureDetector is created along
 * when initializing the server, allowing it to detect and handle crashes. 
 * The protocol used for socket communications is detailed in the Client class.
 * 
 * @param host The port number the Server is using for socket communications
 * @see Handler
 * @see ClientFailureDetector
 * @see Client 
 *
 */

public class Server{

	/** The port number the Server is using for socket communications, 10000 by default */
	int host;
	
	/** The socket used for server-side socket communications */
	ServerSocket serverSocket;
	
	/** The default name of the table that contains information about files */
	public static final String TABLE_NAME = "FILES";
	
	/** The default name of the database */
	public static final String DATASOURCE = "Database";
	
	/** The connection to the database (when using Java DB) */
	Connection connection;
	
	/** The ClientFailureDetector instance used to detect and handle client crashes */
	ClientFailureDetector clientFailureDetector;

	
	/** 
	 * Instantiate and start a ClientFailureDetector, creates the connection to the database.
	 * 
	 * @param host The port number the Server is using for socket communications
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public Server(int host) throws IOException, ClassNotFoundException, SQLException{

		this.host = host;
		connection = createDatasource();
		clientFailureDetector = new ClientFailureDetector(connection);
		new Thread(clientFailureDetector).start();

		try {
			this.serverSocket = new ServerSocket(host);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/** Assuming the connexion to the datasource has already been established, this method deletes everything 
	 * in the database and create empty tables with the right columns, thus avoiding outdated data. 
	 * 
	 * @return The connection to the datasource
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private Connection createDatasource() throws ClassNotFoundException, SQLException {
		Connection connection = getConnection();

		Statement statementFiles = connection.createStatement();
		statementFiles.executeUpdate("DROP TABLE FILES");
		statementFiles.executeUpdate("CREATE TABLE FILES (id INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), filename VARCHAR(64), type VARCHAR(64), clientname VARCHAR(64))");
		Statement statementUsers = connection.createStatement();
		statementFiles.executeUpdate("DROP TABLE USERS");
		statementUsers.executeUpdate("CREATE TABLE USERS (clientname VARCHAR(64) PRIMARY KEY, address VARCHAR(64), port INT)");

		return connection;
	}

	/**
	 * Creates the connection to the datasource (under JDBC Derby), and creates the database if 
	 * it is not already existing
	 * 
	 * @return The connection to the datasource
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private Connection getConnection() throws ClassNotFoundException, SQLException {
		Class.forName("org.apache.derby.jdbc.ClientXADataSource");
		return DriverManager.getConnection("jdbc:derby://localhost:1527/" + DATASOURCE + ";create=true");

	}

	/** 
	 * Main function. The user is prompted the port the server should use for socket communications.
	 * A Server is then instanciated, and new connections are handled by new Handler instances.
	 * 
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
		BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter port number :");
		int host = Integer.parseInt(consoleIn.readLine());

		Server server = new Server(host);

		System.out.println("The server is created. IP Address : " + InetAddress.getLocalHost().getHostAddress() + " | Port number : " + host);


		while (true) {
			Socket socket;
			try {
				socket = server.serverSocket.accept();
				Handler handler =  new Handler(socket, server.connection);
				handler.setPriority( handler.getPriority() + 1 );
				handler.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}