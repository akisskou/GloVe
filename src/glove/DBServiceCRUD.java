package glove;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import java.sql.ResultSet;




public class DBServiceCRUD {
	static Connection db_con_obj = null;
	static PreparedStatement db_prep_obj = null;
	
	public static Boolean makeJDBCConnection(ConfigureFile configureFile_obj) {
		 Boolean connection_succes=true;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Congrats - Seems your MySQL JDBC Driver Registered!");
		} catch (ClassNotFoundException e) {
			System.out.println("Sorry, couldn't found JDBC driver. Make sure you have added JDBC Maven Dependency Correctly");
			e.printStackTrace();
			return false;
		}
 
		try {
			//System.out.println("URL: "+configureFile_obj.getDbURL()+" username: "+configureFile_obj.getUsername()+" password: "+configureFile_obj.getPassword());
			db_con_obj = DriverManager.getConnection(configureFile_obj.getDbURL(), configureFile_obj.getUsername(), configureFile_obj.getPassword());
			//db_con_obj = DriverManager.getConnection("jdbc:mysql://147.102.19.66:3306/HarmonicSS","ponte", "p0nt3");
			if (db_con_obj != null) {
				System.out.println("Connection Successful! Enjoy. Now it's time to Store data");
			} else {
				System.out.println("Failed to make connection!");
				return false;
			}
		} catch (SQLException e) {
			System.out.println("MySQL Connection Failed!");
			e.printStackTrace();
			return false;
		}
		return connection_succes;
	}
	
	public static void closeJDBCConnection() throws SQLException {
		 if (db_prep_obj != null) {
	        	db_prep_obj.close();
	        }
	        if (db_con_obj != null) {
	        	db_con_obj.close();
	        }
	        System.out.println("DB Connection closed successfully");
	}
	
	public static String getDataFromDB(String getQueryStatement) throws SQLException {
		String query_results=""; 
		
			//System.out.println("Stage-1 Prepare statement.");
			db_prep_obj = db_con_obj.prepareStatement(getQueryStatement);
			
			//System.out.println("Stage-2 Ready to execute the query.");
			// Execute the Query, and get a java ResultSet
			
			ResultSet rs = db_prep_obj.executeQuery();
			//System.out.println("We are ready to retrieve data from DB.");
			
			//System.out.println("Stage-3 The query is executed.");
			// Let's iterate through the java ResultSet
			if (rs == null) return "";
			String id = "";
			String term = "";
			while (rs.next()) {
				//System.out.println("Stage-4 We retrieve the IDs one by one.");
				String myid = rs.getString("ID");
				String myterm = rs.getString("NAME");
				if(!id.equals(myid)) {
					id = myid;
					if(query_results.equals("")) query_results += id+"!";
					else query_results += term+"|"+id+"!";
					term = myterm;
				}
				else {
					term += ","+myterm;
				}				
			}
			query_results += term;
			
			//System.out.println("Stage-5 Finished.");
			return query_results;
	}
}
