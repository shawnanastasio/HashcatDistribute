package io.anastas.hashcatdistribute.serverhandlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import com.sun.net.httpserver.*;

import io.anastas.hashcatdistribute.App;
import io.anastas.hashcatdistribute.LangTools;
import io.anastas.hashcatdistribute.responses.*;

public class SubmitWorkHandler implements HttpHandler {
	public void handle(HttpExchange t) throws IOException {
		// Read parameters
		InputStream in = t.getRequestBody();
		
		byte[] buffer = new byte[256];
		in.read(buffer);
		String data = new String(buffer);
		
		Map<String, String> params = LangTools.parsePOSTArguments(data);
		boolean validData = true;
		if (params == null) {
			//System.out.println("DEBUG: No Params!");
			validData = false;
		} else {
			// Parse variables and make sure we have everything needed
			if (params.keySet().size() != 4) {
				//System.out.println("Bad param len: " + params.keySet().size());
				validData = false;
			} else {
				if (params.get("UID") == null || params.get("UID").equals("") ||
					params.get("jobID") == null || params.get("jobID").equals("") ||
					params.get("chunkID") == null || params.get("chunkID").equals("") ||
					params.get("result") == null || params.get("result").equals("")) {
					
					//System.out.println("DEBUG: Missing params!");
					validData = false;
				}
				else {
					try {
						Integer.parseInt(params.get("UID"));
						Integer.parseInt(params.get("jobID"));
						Integer.parseInt(params.get("chunkID"));
					} catch (Exception e) {
						//System.out.println("Can't parse "+params.get("UID")+"as int!");
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
		
		try {
			// Validate UID
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
			
			// Validate that client is authorized to submit for this job/chunk
			pst = App.dbcon.prepareStatement("SELECT COUNT(*) FROM chunks WHERE ID = ? AND clientUID = ? AND jobID = ?");
			pst.setInt(1, Integer.parseInt(params.get("chunkID")));
			pst.setInt(2, Integer.parseInt(params.get("UID")));
			pst.setInt(3, Integer.parseInt(params.get("jobID")));
			rs = pst.executeQuery();
			rs.next();
			if (rs.getInt(1) == 0) {
				// Bad user
				String response = "401 Forbidden";
				t.sendResponseHeaders(401, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}			
			// Validate that job isn't already solved
			pst = App.dbcon.prepareStatement("SELECT COUNT(*) FROM jobs WHERE jobID = ? AND done IS NOT NULL");
			pst.setInt(1, Integer.parseInt(params.get("jobID")));
			rs = pst.executeQuery();
			rs.next();
			if (rs.getInt(1) != 0) {
				String response = new SubmitWorkResponse(false).serialize();
				t.sendResponseHeaders(200, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			
			
			// Mark that the client has completed this chunk
			pst = App.dbcon.prepareStatement("UPDATE chunks SET done = ? WHERE ID = ?");
			pst.setString(1, "Y");
			pst.setInt(2, Integer.parseInt(params.get("chunkID")));
			pst.executeUpdate();
			
			if (!params.get("result").equals("none")) {
				// Result found, mark job as completed and add result to DB
				// TODO: Actually verify result instead of blindly trusting clients
				
				// Add to results table
				pst = App.dbcon.prepareStatement("INSERT INTO results (jobID, result) VALUES (?, ?)");
				pst.setInt(1, Integer.parseInt(params.get("jobID")));
				pst.setString(2, params.get("result"));
				pst.executeUpdate();
				
				// Update entry in jobs table to signify that the job is done
				pst = App.dbcon.prepareStatement("UPDATE jobs SET done = ? WHERE jobID = ?");
				pst.setString(1, "Y");
				pst.setInt(2, Integer.parseInt(params.get("jobID")));
				pst.executeUpdate();				
			}
			
			
			// Send back response
			String response = new SubmitWorkResponse(true).serialize();
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
