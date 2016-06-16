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
import io.anastas.hashcatdistribute.responses.WorkLookupRequestResponse;

public class WorkLookupRequestHandler implements HttpHandler {
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
			int UID, jobID, chunkID;
			UID = Integer.parseInt(params.get("UID"));
			jobID = Integer.parseInt(params.get("jobID"));
			chunkID = Integer.parseInt(params.get("chunkID"));
			
			// Validate UID
			PreparedStatement pst = App.dbcon.prepareStatement("SELECT COUNT(*) FROM clients WHERE UID = ?");
			pst.setInt(1, UID);
			ResultSet rs = pst.executeQuery();
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
			
			// Validate that client is authorized to request for this job/chunk
			pst = App.dbcon.prepareStatement("SELECT COUNT(*) FROM chunks WHERE ID = ? AND clientUID = ? AND jobID = ?");
			pst.setInt(1, chunkID);
			pst.setInt(2, UID);
			pst.setInt(3, jobID);
			rs = pst.executeQuery();
			rs.next();
			if (rs.getInt(1) == 0) {
				// Not authorized
				String response = "401 Forbidden";
				t.sendResponseHeaders(401, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			
			// Grab requested job data
			WorkLookupRequestResponse wlr = new WorkLookupRequestResponse();
			pst = App.dbcon.prepareStatement("SELECT hashtype, hash, keyspace, command FROM jobs WHERE jobID = ?");
			pst.setInt(1, jobID);
			rs = pst.executeQuery();
			rs.next();
			wlr.jobID = jobID;
			wlr.hashtype = rs.getInt(1);
			wlr.hash = rs.getString(2);
			wlr.keyspace = rs.getLong(3);
			
			// Grab requested chunk data
			pst = App.dbcon.prepareStatement("SELECT start, end FROM chunks WHERE ID = ?");
			pst.setInt(1, chunkID);
			rs = pst.executeQuery();
			rs.next();
			wlr.chunkStart = rs.getLong(1);
			wlr.chunkEnd = rs.getLong(2);
			
			// Return Response to client
			String response = wlr.serialize();
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
