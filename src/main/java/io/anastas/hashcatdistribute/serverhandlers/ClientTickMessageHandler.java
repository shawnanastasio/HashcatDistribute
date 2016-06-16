package io.anastas.hashcatdistribute.serverhandlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.*;
import java.sql.*;

import com.sun.net.httpserver.*;

import io.anastas.hashcatdistribute.App;
import io.anastas.hashcatdistribute.ChunkQueue;
import io.anastas.hashcatdistribute.JobQueue;
import io.anastas.hashcatdistribute.LangTools;
import io.anastas.hashcatdistribute.responses.ServerTickMessage;

public class ClientTickMessageHandler implements HttpHandler {
	public void handle(HttpExchange t) throws IOException {
		// Read parameters
		InputStream in = t.getRequestBody();
		
		byte[] buffer = new byte[256];
		in.read(buffer);
		String data = new String(buffer);
		
		Map<String, String> params = LangTools.parsePOSTArguments(data);
		boolean validData = true;
		if (params == null) {
			System.out.println("DEBUG: No Params!");
			validData = false;
		} else {
			// Parse variables and make sure we have everything needed
			if (params.keySet().size() != 3) 
				validData = false;
			else {
				if (params.get("UID") == null || params.get("UID").equals("") ||
					params.get("jobID") == null || params.get("jobID").equals("") ||
					params.get("chunkID") == null || params.get("chunkID").equals("")) {
					
					System.out.println("DEBUG: Missing params!");
					validData = false;
				}
				else {
					try {
						Integer.parseInt(params.get("UID"));
						Integer.parseInt(params.get("jobID"));
						Integer.parseInt(params.get("chunkID"));
					} catch (Exception e) {
						System.out.println("Can't parse "+params.get("UID")+"as int!");
						validData = false;
					}
				}
			}
		}
		
		if (!validData) {
			String response = "Invalid request!";
			t.sendResponseHeaders(400, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
			return;
		}
		
		// Validate UID and respond
		try {
			PreparedStatement pst = App.dbcon.prepareStatement("SELECT COUNT(*) FROM clients WHERE UID = ?");
			pst.setInt(1, Integer.parseInt(params.get("UID")));
			ResultSet rs = pst.executeQuery();
			rs.next();
			//System.out.println("USERS W/ THIS UID: " + rs.getInt(1));
			
			if (rs.getInt(1) == 0) {
				// Bad user
				String response = "401 Forbidden";
				t.sendResponseHeaders(401, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			
			// Send back new ServerTickMessage
			ServerTickMessage stm = null;
			int clientUID = Integer.parseInt(params.get("UID"));
			
			if (params.get("jobID").equals("-1")) { // If client needs a needs a new job
				// Get job and chunk from queue and respond to client
				int newJob = JobQueue.getCurrentJobID();
				stm = new ServerTickMessage(clientUID, newJob, ChunkQueue.getNewChunk(clientUID, newJob));
			} else if (params.get("chunkID").equals("-1")) { // If client needs a new chunk
				// Get chunk from queue and respond to client
				stm = new ServerTickMessage(clientUID, -1, ChunkQueue.getNewChunk(clientUID, Integer.parseInt(params.get("jobID"))));
			} else { // Client is busy, nothing to do
				stm = new ServerTickMessage(clientUID, -1, -1);
			}
			
			String response = stm.serialize();
			// Respond
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
			return;
		} catch (Exception e) {
			//e.printStackTrace();
			String response = "An error has occurred while processing your request.";
			if (e.getMessage() != null) response = e.getMessage();
			t.sendResponseHeaders(400, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
			return;
		}
		
	}
}
