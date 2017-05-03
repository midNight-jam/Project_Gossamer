package gash.router.database;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.sql.rowset.serial.SerialBlob;

import com.google.protobuf.ByteString;

public class MySQLDatabase implements DatabaseClient {
	Connection conn = null;

	public MySQLDatabase(String url, String username, String password, String ssl) throws SQLException {
		
		Properties props = new Properties();
		props.setProperty("user", username);
		props.setProperty("password", password);
		props.setProperty("ssl", ssl);
		conn = DriverManager.getConnection(url, props);
		
	}

	@Override
	public List<Record> get(String filename, String file_id, String file_ext) {
		Statement stmt = null;
		List<Record> list = new ArrayList<Record>();
		
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("Select * FROM fileStorage1 WHERE file_name = '"+filename+"'");
			
			while (rs.next()) {
				ByteString bytes= ByteString.copyFrom(rs.getBytes(7));
				System.out.print("Filename : "+rs.getString(2)+" ChunkID: "+rs.getLong(4)+"ChunkNum: "+rs.getLong(5));
				list.add(new Record(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4),rs.getLong(5),rs.getLong(6),bytes,rs.getLong(8)));
			
				
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return list;		
	}
	@Override
	public Record getChunkData(String filename, long file_id, long chunk_id) {
		Statement stmt = null;
		Record record= null;
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("Select * FROM fileStorage1 WHERE file_name = '"+filename+"' and chunk_id = "+chunk_id+"");
			
			while (rs.next()) {
				ByteString bytes= ByteString.copyFrom(rs.getBytes(7));
				System.out.print("Filename : "+rs.getString(2)+" ChunkID: "+rs.getInt(4)+"chunk data : "+bytes.toStringUtf8());
				 record = new Record(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4),rs.getLong(5),rs.getLong(6),bytes,rs.getLong(8));
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return record;		
	}
	
	@Override
	public List<Record> getNewEntries(long staleTimestamp) {
		Statement stmt = null;
		List<Record> list = new ArrayList<Record>();
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("Select * FROM fileStorage1 where timestamp > " + staleTimestamp);
			
			while (rs.next()) {
				ByteString bytes= ByteString.copyFrom(rs.getBytes(7));
				list.add(new Record(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4),rs.getLong(5),rs.getLong(6),bytes,rs.getLong(8)));
		
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return list;		
	}

	/**
	@throws SQLException 
	 * @Override
	public List<Record> getAllEntries() {
		Statement stmt = null;
		List<Record> list = new ArrayList<Record>();
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("Select key, image, timestamp FROM testtable");
			
			while (rs.next()) {
			//	list.add(new Record(rs.getString(1), rs.getBytes(2), rs.getLong(3)));
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return list;		
	}

	
	
	
	@Override
	public String post(byte[] image, long timestamp){
		String key = UUID.randomUUID().toString();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("INSERT INTO testtable VALUES ( ?, ?, ?)");
			ps.setString(1, key);
			ps.setBytes(2, image);
			ps.setLong(3, timestamp);
			ResultSet set = ps.executeQuery();
			
		} catch (SQLException e) {
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return key;
	}

	
	@Override
	public void post(String key, byte[] image, long timestamp){
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("INSERT INTO testtable VALUES ( ?, ?, ?)");
			ps.setString(1, key);
			ps.setBytes(2, image);
			ps.setLong(3, timestamp);
			ResultSet set = ps.executeQuery();
			
		} catch (SQLException e) {
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void put(String key, byte[] image, long timestamp){		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("UPDATE testtable SET image= ? , timestamp = ?  WHERE key LIKE ?");
			ps.setBytes(1, image);
			ps.setLong(2, timestamp);
			ps.setString(3, key);
			ResultSet set = ps.executeQuery();
			
		} catch (SQLException e) {
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	
	
	@Override
	public void putEntries(List<Record> list){		
			for (Record record : list) {
			//	put(record.getKey(), record.getImage(), record.getTimestamp());
			}
	}

	
	@Override
	public void delete(String key){
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			StringBuilder sql = new StringBuilder();
			sql.append("DELETE FROM testtable WHERE key LIKE '"+key+"';");			
			stmt.executeUpdate(sql.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// initiate new everytime
		}
	}
	**/
	@Override
	public void post(String file_id,String filename, String file_ext, long chunk_id, ByteString chunk_data, long chunk_size, long num_of_chunks, long timestamp) throws SQLException{
	//	Statement stmt = null;
		PreparedStatement ps = null;
		    byte[] bytes = chunk_data.toByteArray();
			System.out.println("Here in DB :");
			
	
			
			ps = conn.prepareStatement("INSERT INTO fileStorage1 VALUES (?,?,?,?,?,?,?,?)");
			
			ps.setString(1,file_id );
			ps.setString(2,filename );
			ps.setString(3,file_ext );
			ps.setLong(4, chunk_id);
			ps.setLong(5, num_of_chunks);
			ps.setLong(6,timestamp);
			ps.setBytes(7, bytes);
			ps.setLong(8, chunk_size);
			
			
			
		
			
			int i=ps.executeUpdate();  
		/**
			 stmt = conn.createStatement();
		      
		      String sql = "INSERT INTO fileStorage VALUES ( '"+file_id+"', '"+ filename +"', '"+file_ext+"',"+chunk_id+","+num_of_chunks+","+timestamp+",'"+str+"',"+chunk_size+")";
			  stmt.executeUpdate(sql);
		      **/
		      System.out.println("Inserted records into the table...");
	
		
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		
		
	}
	
	@Override
	public long getCurrentTimeStamp() {
		Statement stmt = null;
		long timestamp = 0; 
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("Select max(timestamp) FROM fileStorage1");
			
			while (rs.next()) {
				timestamp = rs.getLong(1);
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return timestamp;		
	}


}
