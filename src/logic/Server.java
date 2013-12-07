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

public class Server{

	int host;
	ServerSocket serverSocket;
	public static final String TABLE_NAME = "FILES";
	public static final String DATASOURCE = "Database";
	Connection connection;

	public Server(int host) throws IOException, ClassNotFoundException, SQLException{

		this.host = host;
		connection = createDatasource();

		try {
			this.serverSocket = new ServerSocket(host);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

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

	private Connection getConnection() throws ClassNotFoundException, SQLException {
		Class.forName("org.apache.derby.jdbc.ClientXADataSource");
		return DriverManager.getConnection("jdbc:derby://localhost:1527/" + DATASOURCE + ";create=true");

	}

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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}