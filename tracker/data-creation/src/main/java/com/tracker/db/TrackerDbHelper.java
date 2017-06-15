package com.tracker.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

public class TrackerDbHelper {

	public static Connection connection = null;
	/**
	 * 
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws Exception
	 */
		public static Connection getDatabaseConnection(String env) throws ClassNotFoundException, SQLException, Exception{
			Class.forName("oracle.jdbc.driver.OracleDriver");
			ResourceBundle DB_PROP = ResourceBundle.getBundle("env", Locale.ENGLISH);
			String DB_CONNECTION = DB_PROP.getString(env +"_ZIL_CONNECTION");
			String DB_USER = DB_PROP.getString(env +"_ZIL_DB_USER");
			String DB_PASSWORD = DB_PROP.getString(env +"_ZIL_DB_PASSWORD");
			if(null == connection){
			 connection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
			}
			return connection;
		}
}
