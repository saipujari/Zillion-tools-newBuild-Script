package com.zillion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

public class DBConnection {
    
    public static Connection getMysqlDBConnection(String env) {
        Connection dbConnection = null;
        try {
            ResourceBundle DB_PROP = ResourceBundle.getBundle("env", Locale.ENGLISH);
            String DB_DRIVER = "com.mysql.jdbc.Driver";
            String DB_HOST = "jdbc:mysql://" + DB_PROP.getString(env +"_MND_DB_HOST") + "/" + DB_PROP.getString(env +"_MND_DB_NAME") ;
            String DB_USER = DB_PROP.getString(env +"_MND_DB_USER");
            String DB_PASSWORD = DB_PROP.getString(env +"_MND_DB_PASSWORD");
            Class.forName(DB_DRIVER);
            dbConnection = DriverManager.getConnection(DB_HOST, DB_USER,DB_PASSWORD);
            return dbConnection;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return dbConnection;
    }
    
    public static Connection getOracleDBConnection(String env) {
        Connection dbConnection = null;     
        String DB_DRIVER = "oracle.jdbc.driver.OracleDriver";
        ResourceBundle DB_PROP = ResourceBundle.getBundle("env", Locale.ENGLISH);
        String DB_CONNECTION = DB_PROP.getString(env +"_ZIL_CONNECTION");
        String DB_USER = DB_PROP.getString(env +"_ZIL_DB_USER");
        String DB_PASSWORD = DB_PROP.getString(env +"_ZIL_DB_PASSWORD");
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER,DB_PASSWORD);
            return dbConnection;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return dbConnection;
    }
}
