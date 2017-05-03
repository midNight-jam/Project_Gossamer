package gash.router.database;

import java.sql.SQLException;

public class DatabaseService {
	
	protected DatabaseClient db;
	private String url = null;
	private String username = null;
	private String password = null;

	protected String ssl="true";

	private static DatabaseService instance = null;
	
	public static DatabaseService getInstance() {
		if (instance == null) {
			instance = new DatabaseService();			
		}
		return instance;
	}
	
	private DatabaseService() {
	}
	
	public DatabaseClient getDb() {
	
		this.url = "jdbc:mysql://localhost:3306/FileStorageSystem";
		this.username = "root";
		this.password = "root";
		try {
		if (db == null) {
		 				
	     db = new MySQLDatabase(url, username, password, ssl);
				
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return db;
	}

	public String getUrl() {
		return url;
	}

	
}
