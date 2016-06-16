package io.anastas.hashcatdistribute;

import java.sql.*;

public class ChunkQueue {
	/**
	 * Calculates the chunk size for a given keyspace
	 * @return chunk size
	 */
	public static long calculateChunkSize(long keyspace) {
		// Algo: Keyspace/10^((keyspace.length/2)-1)*4
		return (int) (keyspace/Math.pow(10, ((keyspace+"").length())/2)-1)*4;
	}
	
	/**
	 * Returns a new chunk to work with automatic chunk size
	 * @param clientUID
	 * @param jobID
	 * @return -1 if no such job exists
	 * @throws SQLException
	 */
	public static int getNewChunk(int clientUID, int jobID) throws SQLException {
		long keyspace;
		PreparedStatement pst = App.dbcon.prepareStatement("SELECT keyspace FROM jobs WHERE jobID = ?");
		pst.setInt(1, jobID);
		ResultSet rs = pst.executeQuery();
		if (!rs.next()) return -1;
		keyspace = rs.getLong(1);
		return getNewChunk(clientUID, jobID, calculateChunkSize(keyspace));
	}

	/**
	 * Returns a new chunk to work on with specified chunk size
	 * @param clientUID UID of client who this chunk is for
	 * @param jobID ID of job to get chunk for
	 * @param chunkSize size of new chunk (may be lowered to fit in keyspace)
	 * @return id of new chunk or -1 if not available/possible
	 * @throws SQLException
	 * 
	 * Precondition: clientUID and jobUID are valid
	 */
	public static int getNewChunk(int clientUID, int jobID, long chunkSize) throws SQLException {
		PreparedStatement pst = null;
		ResultSet rs = null;
		// Check for chunks that have timed out and still not solved and mark for assignment
		pst = App.dbcon.prepareStatement("UPDATE chunks SET clientUID = NULL WHERE created < timestampadd(second, ?, now()) AND done IS NULL AND clientUID IS NOT NULL");
		pst.setInt(1, App.config.chunkTimeout*-1);
		pst.executeUpdate();	
		
		// If client is still working on a chunk, complete that
		pst = App.dbcon.prepareStatement("SELECT ID FROM chunks WHERE clientUID = ? AND jobID = ? AND done IS NULL");
		pst.setInt(1, clientUID);
		pst.setInt(2, jobID);
		rs = pst.executeQuery();
		if (rs.next()) {
			// If there exists an in-progress chunk for this client and job, return it back to them
			return rs.getInt(1);
		}
		
		// Before making a new chunk, check if there are unassigned chunks and use the first one of those instead
		pst = App.dbcon.prepareStatement("SELECT ID FROM chunks WHERE clientUID IS NULL AND jobID = ?");
		pst.setInt(1, jobID);
		rs = pst.executeQuery();
		if (rs.next()) {
			// If there is an unassigned chunk, assign it to this client then return its ID
			int newChunk = rs.getInt(1);
			pst = App.dbcon.prepareStatement("UPDATE chunks SET clientUID = ? WHERE ID = ?");
			pst.setInt(1, clientUID);
			pst.setInt(2, newChunk);
			pst.executeUpdate();
			return newChunk;
		}
		
		// Get number of chunks 
		pst = App.dbcon.prepareStatement("SELECT COUNT(*) FROM chunks WHERE jobID = ?");
		pst.setInt(1, jobID);
		rs = pst.executeQuery();
		if (!rs.next()) return -1;
		int count = rs.getInt(1);

		int lastEndAddress; // End address of the most recent chunk
		if (count == 0) { // If this is the first chunk
			lastEndAddress = -1;
		} else {
			// Get end address of last chunk
			pst = App.dbcon.prepareStatement("SELECT end FROM chunks WHERE jobID = ? ORDER BY ID DESC LIMIT 1");
			pst.setInt(1, jobID);
			rs = pst.executeQuery();
			rs.next();
			lastEndAddress = rs.getInt(1);
		};

		// Get keyspace of job
		pst = App.dbcon.prepareStatement("SELECT keyspace FROM jobs where jobID = ?");
		pst.setInt(1, jobID);
		rs = pst.executeQuery();
		rs.next();
		long jobKeyspace = rs.getInt(1);

		// Check if job is over
		if (lastEndAddress >= jobKeyspace) {
			// Mark job completion in its `jobs` table entry and return null
			pst = App.dbcon.prepareStatement("UPDATE jobs SET done = ? WHERE jobID = ? AND done IS NULL");
			pst.setString(1, "Y");
			pst.setInt(2, jobID);
			pst.executeUpdate();

			return -1;
		}

		// Check if chunkSize is too big and adjust accordingly
		long keyspaceLeft = jobKeyspace - lastEndAddress;
		if (chunkSize > keyspaceLeft) chunkSize = keyspaceLeft;

		// Assign new chunk
		long newStart = lastEndAddress+1;
		long newEnd = newStart + chunkSize;
		pst = App.dbcon.prepareStatement("INSERT INTO chunks (jobID, clientUID, start, end) VALUES (?, ?, ?, ?)");
		pst.setInt(1, jobID);
		pst.setInt(2, clientUID);
		pst.setLong(3, newStart);
		pst.setLong(4, newEnd);
		pst.executeUpdate();

		// Get ID of newly created chunk
		pst = App.dbcon.prepareStatement("SELECT ID FROM chunks WHERE jobID = ? AND clientUID = ? AND start = ? AND end = ?");
		pst.setInt(1, jobID);
		pst.setInt(2, clientUID);
		pst.setLong(3, newStart);
		pst.setLong(4, newEnd);
		rs = pst.executeQuery();
		rs.next();
		
		return rs.getInt(1);
		
	}
}

