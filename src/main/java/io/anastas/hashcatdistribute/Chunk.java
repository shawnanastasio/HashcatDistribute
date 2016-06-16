package io.anastas.hashcatdistribute;

public class Chunk {
	public int clientUID;
	public long start;
	public long end;
	
	public Chunk(int clientUID, long start, long end) {
		this.clientUID = clientUID;
		this.start = start;
		this.end = end;
	}
}
