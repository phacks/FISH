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

public class ClientFailureDetector implements Runnable {
	
	Connection connection;
	private PreparedStatement findClientsStatement;
	Socket socket;
	PrintWriter wr;
	BufferedReader rd;
	PreparedStatement unregisterClientStatement;
	String clientName;
	String address;
	int downloadPort;
	private PreparedStatement deleteFilesStatement;
	private final int UPDATE_TIME = 5000;

	public ClientFailureDetector(Connection connection) throws SQLException {
		this.connection = connection;
		findClientsStatement = connection.prepareStatement("SELECT * from USERS");
		unregisterClientStatement = connection.prepareStatement("DELETE FROM USERS WHERE CLIENTNAME = ?");
		deleteFilesStatement = connection.prepareStatement("DELETE FROM FILES WHERE CLIENTNAME = ?");
	}

	@Override
	public void run() {
		while(true){
			try {
				Thread.sleep(UPDATE_TIME);
				ResultSet result = findClientsStatement.executeQuery();
				
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
			} catch (IOException e) {
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
