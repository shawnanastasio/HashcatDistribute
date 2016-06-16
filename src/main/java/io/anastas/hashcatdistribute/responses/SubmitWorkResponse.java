package io.anastas.hashcatdistribute.responses;

import com.google.gson.Gson;

public class SubmitWorkResponse {
	private boolean success;
	
	public SubmitWorkResponse(boolean success) {
		this.success = success;
	}
	
	public String serialize() {
		return new Gson().toJson(this);
	}
}
