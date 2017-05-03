package gash.router.database;

import com.google.protobuf.ByteString;

public class Record {
	
	private String file_id;
	private String filename;
	private String file_ext;
	private long chunk_id;
	private long chunk_size;
	private long num_of_chunks;
	private long timestamp = 0;
	private ByteString chunk_data;
	
	public String getFile_id() {
		return file_id;
	}
	public void setFile_id(String file_id) {
		this.file_id = file_id;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getFile_ext() {
		return file_ext;
	}
	public void setFile_ext(String file_ext) {
		this.file_ext = file_ext;
	}
	public long getChunk_id() {
		return chunk_id;
	}
	public void setChunk_id(long chunk_id) {
		this.chunk_id = chunk_id;
	}
	public ByteString getChunk_data() {
		return chunk_data;
	}
	public void setChunk_data(ByteString chunk_data) {
		this.chunk_data = chunk_data;
	}
	public long getChunk_size() {
		return chunk_size;
	}
	public void setChunk_size(long chunk_size) {
		this.chunk_size = chunk_size;
	}
	public long getNum_of_chunks() {
		return num_of_chunks;
	}
	public void setNum_of_chunks(long num_of_chunks) {
		this.num_of_chunks = num_of_chunks;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public Record(String file_id, String filename, String file_ext, long chunk_id, long num_of_chunks,
			long timestamp, ByteString chunk_data,long chunk_size) {
		
		this.file_id = file_id;
		this.filename = filename;
		this.file_ext = file_ext;
		this.chunk_id = chunk_id;
		this.chunk_size = chunk_size;
		this.num_of_chunks = num_of_chunks;
		this.timestamp = timestamp;
		this.chunk_data = chunk_data;
	}
			
}
