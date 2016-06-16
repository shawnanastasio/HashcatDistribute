package io.anastas.hashcatdistribute;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

import com.mysql.jdbc.*;
import java.sql.Connection;
import com.sun.net.httpserver.*;

import io.anastas.hashcatdistribute.serverhandlers.*;
import io.anastas.hashcatdistribute.serverhandlers.ClientTickMessageHandler;

/**
 * Server entry point
 */
public class App {	
	public static Connection dbcon;
	public static ServerConfig config;


	public static void main(String[] args) {
		try {
			// Load config
			config = ServerConfig.loadConfig();

			// Establish connection to MySQL server
			dbcon = DriverManager.getConnection("jdbc:mysql://" + config.databaseIP +":"
												+ config.databasePort +"/" + config.databaseName
												, config.databaseUser, config.databasePassword);

			// Create Java HttpServer and start listening
			HttpServer server = HttpServer.create(new InetSocketAddress(config.port), 0);
			server.createContext("/getwork", new ClientTickMessageHandler());
			server.createContext("/submitwork", new SubmitWorkHandler());
			server.createContext("/getinfo", new WorkLookupRequestHandler());
			server.setExecutor(null);
			server.start();
			
			System.out.println("Listening for connections at 0.0.0.0:" + config.port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
