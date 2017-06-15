package com.zillion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class WeightSyncThread implements Runnable {
    
    String env = null;
    String startDate = null;
    String endDate = null;
    String measurementId = null;
    
    public WeightSyncThread(String env, String startDate,  String endDate, String measurementId) {
        this.env = env;
        this.startDate = startDate;
        this.endDate = endDate;
        this.measurementId = measurementId;
    }

    public void run() {
        System.out.println("Sync started for " + startDate + " - " + endDate);
        Connection mysqlConnection = null;
        Connection oracleConnection = null;
        try {
            mysqlConnection = DBConnection.getMysqlDBConnection(env);
            oracleConnection = DBConnection.getOracleDBConnection(env);
            Statement oracleStmt = oracleConnection.createStatement();
            Statement mysqlStmt = mysqlConnection.createStatement();
            
            String insertQuery = "MERGE INTO ACCOUNT_TRK_MEASUREMENT T "
                + "USING DUAL "
                + "ON (T.SOURCE=? AND T.ACCOUNT_ID=? AND T.DATE_LOGGED=? AND T.VALUE=?) "
                + "WHEN NOT MATCHED THEN "
                + "INSERT (MEASUREMENT_TYPE, DATE_LOGGED, SOURCE, SOURCE_NAME, LAST_UPDATED, "
                + "VALIDATED, IS_PRIMARY, IS_DELETED, ACCOUNT_ID, VALUE, MEASUREMENT_ID, "
                + "CREATED_DT, LAST_MODIFIED_DT) VALUES "
                + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = oracleConnection.prepareStatement(insertQuery);
            oracleConnection.setAutoCommit(false);
            
            //Fetching weight data
            String sql = "SELECT userId, measurementId, measurementDate, actualValue, entryTimestamp "
                + "FROM measuremententry WHERE measurementId = 40 AND "
                + "entryTimestamp BETWEEN '" + startDate + "' AND '" + endDate + "' "
                + "ORDER BY userId, measurementDate";
            ResultSet rs = mysqlStmt.executeQuery(sql);
            String userId = "";
            int count = 0;
            int batchSize = 500;
            while(rs.next()) {
                String accountId = "";
                ResultSet rs1;
                userId = rs.getString("userId");
                sql = "SELECT ACCOUNT_ID FROM ACCOUNT_IDENTITY_MAP "
                    + "WHERE MAP_SYSTEM_ACCOUNT_CODE LIKE '%\"userId\":\"" + userId + "\"%' AND "
                    + "MAP_SYSTEM_NAME='MYNETDIARY' AND IS_DELETED=0";
                rs1 = oracleStmt.executeQuery(sql);
                if(rs1.next()) {
                    accountId = rs1.getString("ACCOUNT_ID");
                    System.out.println("ACCOUNT: " + accountId + ", UserID: " + userId);
                    double actualValue = rs.getDouble("actualValue");
                    actualValue =  convertGramsToPounds(actualValue);
                    Date measurementDate = rs.getDate("measurementDate");
                    Date entryDate = rs.getDate("entryTimestamp");
                    
                    //Conditional query validation before inserting
                    pstmt.setString(1, "MND");
                    pstmt.setString(2, accountId);
                    pstmt.setDate(3, measurementDate);
                    pstmt.setDouble(4, actualValue);
                    //Data to be inserted
                    pstmt.setString(5, "WEIGHT");
                    pstmt.setDate(6, measurementDate);
                    pstmt.setString(7, "MND");
                    pstmt.setString(8, "MND");
                    pstmt.setDate(9, entryDate);
                    pstmt.setBoolean(10, false);
                    pstmt.setBoolean(11, true);
                    pstmt.setBoolean(12, false);
                    pstmt.setString(13, accountId);
                    pstmt.setDouble(14, actualValue);
                    pstmt.setString(15, measurementId);
                    pstmt.setDate(16, new java.sql.Date(System.currentTimeMillis()));
                    pstmt.setDate(17, new java.sql.Date(System.currentTimeMillis()));
                    pstmt.addBatch();
                    if(++count % batchSize == 0){
                        pstmt.executeBatch();
                        oracleConnection.commit();
                    }
                } else {
                    System.out.println("Account Id not found for the user " + userId);
                }
            }
            pstmt.executeBatch();
            oracleConnection.commit();
            //Closing connections
            mysqlStmt.close();
            oracleStmt.close();
            mysqlConnection.close();
            oracleConnection.close();
            System.out.println("Sync completed for " + startDate + " - " + endDate + " with " + count + " records.");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                oracleConnection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }
        
    private static double convertGramsToPounds(double weightInGrams) {
        return Math.round(weightInGrams * 0.00220462);
    }
    
    public static long mmToFeetInches(int mm) {
        double MMS_IN_INCH = 25.4;
        double MMS_IN_FOOT = MMS_IN_INCH * 12;
        long feet = Math.round(Math.floor(mm / MMS_IN_FOOT));
        long inches = Math.round((mm % MMS_IN_FOOT) / MMS_IN_INCH);
        return (feet * 12) + inches;
    }
}
