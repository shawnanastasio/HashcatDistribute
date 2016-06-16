package io.anastas.hashcatdistribute.responses;

import com.google.gson.Gson;

public class WorkLookupRequestResponse {
	public int jobID;
	public int hashtype;
	public String hash;
	public long keyspace;
	
	public long chunkStart;
	public long chunkEnd;
	
	public WorkLookupRequestResponse(int jobID, int hashtype, String hash, long keyspace, long chunkStart, long chunkEnd) {
		this.jobID = jobID;
		this.hashtype = hashtype;
		this.hash = hash;
		this.keyspace = keyspace;
		this.chunkStart = chunkStart;
		this.chunkEnd = chunkEnd;
	}
	
	public WorkLookupRequestResponse() {}
	
	public String serialize() {
		return new Gson().toJson(this);
	}
}
