package io.anastas.hashcatdistribute;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ServerConfig {
	public int port; // Port the server should listen on
	public int chunkTime = 300; // Time in seconds it should take to complete a chunk
	public int chunkTimeout = 600; // Time in seconds it should take for a chunk to time out and be reassigned
	
	public String databaseIP;
	public int databasePort;
	public String databaseName;
	public String databaseUser;
	public String databasePassword;
	
	public ServerConfig(int port, int chunkTime, int chunkTimeout, String databaseIP, int databasePort, 
			            String databaseName, String databaseUser, String databasePassword) {
		this.port = port;
		this.chunkTime = chunkTime;
		this.chunkTimeout = chunkTimeout;
		this.databaseIP = databaseIP;
		this.databasePort = databasePort;
		this.databaseName = databaseName;
		this.databaseUser = databaseUser;
		this.databasePassword = databasePassword;
	}
	
	/**
	 * Loads a config from ./serverconfig.json or creates one with default values if nonexistant
	 * @return ServerConfig object
	 */
	public static ServerConfig loadConfig() {
		try {
			File configFile = new File("serverconfig.json");
			Path configPath = Paths.get("serverconfig.json");
			Charset cs = Charset.forName("UTF-8");
			if (configFile.exists()) {
				return new Gson().fromJson(new String(Files.readAllBytes(configPath), cs), ServerConfig.class);
			} else {
				// Create new default config file
				ServerConfig newConfig = new ServerConfig(5123, 300, 600, "localhost", 3306, "hd", "hd", "hd");
				String newConfigString = newConfig.serialize();
				LangTools.writeFile(newConfigString, "serverconfig.json", cs);
				System.out.println("Config file created at serverconfig.json.");
				System.out.println("Please update your database credentials in it and restart the program.");
				System.exit(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String serialize() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}
}
