package gash.router.database;

import java.sql.SQLException;
import java.util.List;

import com.google.protobuf.ByteString;

public interface DatabaseClient {
	/**
	byte[] get(String key);
	
	String post(byte[] image, long timestamp);
	
	public void put(String key, byte[] image, long timestamp);
	
	public void delete(String key);

	long getCurrentTimeStamp();

	

	void putEntries(List<Record> list);

	List<Record> getAllEntries();

	void post(String key, byte[] image, long timestamp);
	**/
    Record getChunkData(String filename, long file_id, long chunk_id);
	List<Record> get(String filename, String file_id , String file_ext);
	void post(String file_id,String filename, String file_ext, long chunk_id, ByteString chunk_data, long chunk_size, long num_of_chunks, long timestamp) throws SQLException;
	public long getCurrentTimeStamp();
	
	List<Record> getNewEntries(long staleTimestamp);
}
