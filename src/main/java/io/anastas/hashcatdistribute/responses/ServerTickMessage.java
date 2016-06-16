package io.anastas.hashcatdistribute.responses;

import com.google.gson.Gson;

public class ServerTickMessage {
	private int clientUID; // Intended recipient of this message
	private int jobID; // jobID client should begin working on. -1 if no change
	private int chunkID; // chunkID client should begin working on. -1 if no change
	
	public ServerTickMessage(int clientUID, int jobID, int chunkID) {
		this.clientUID = clientUID;
		this.jobID = jobID;
		this.chunkID = chunkID;
	}
	
	public int getClientUID() {
		return clientUID;
	}
	
	public int getJobID() {
		return jobID;
	}
	
	public int getChunkID() {
		return chunkID;
	}
	
	public String serialize() {
		return new Gson().toJson(this);
	}
	
	public static ServerTickMessage deserialize(String m) {
		return new Gson().fromJson(m, ServerTickMessage.class);
	}
}
