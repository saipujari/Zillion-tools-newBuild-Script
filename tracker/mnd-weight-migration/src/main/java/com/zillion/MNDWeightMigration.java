package com.zillion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MNDWeightMigration {
    
    public static void main(String[] args) throws SQLException {
        System.out.println("Sync Started");
        if(validateInput(args)) {
            try {
                Connection oracleConnection = DBConnection.getOracleDBConnection(args[0]);
                Statement oracleStmt = oracleConnection.createStatement();
                String env =  args[0];
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                Date startDate = df.parse(args[1]);
                Date endDate = df.parse(args[2]);
                
                String measurementId = null;
                String sql = "SELECT ID FROM TRACKER_MEASUREMENT WHERE MEASUREMENT_NAME='WEIGHT'";
                ResultSet rs = oracleStmt.executeQuery(sql);
                if(rs.next()) {
                    measurementId = rs.getString("ID");
                } else {
                    throw new RuntimeException("Measurement id is not found"); 
                }
                
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);  
                Date monthEnd = null;
                do {
                    calendar.add(Calendar.MONTH, 1);  
                    calendar.set(Calendar.DAY_OF_MONTH, 1); 
                    calendar.add(Calendar.DATE, -1);  
                    //calendar.add(Calendar.DATE, 0);  
                    
                    monthEnd = calendar.getTime();
                    if(endDate.before(monthEnd)) {
                        monthEnd = endDate;
                    }
                    WeightSyncThread weightSyncThread = new WeightSyncThread(env, df.format(startDate), 
                        df.format(monthEnd), measurementId);
                    Thread thread = new Thread(weightSyncThread);
                    thread.start();
                    
                    calendar.add(Calendar.DATE, 1); 
                    startDate = calendar.getTime();
                } while (startDate.before(endDate));
                oracleStmt.close();
                oracleConnection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private static boolean validateInput(String[] args) {
        boolean val = true;
        if(args.length != 3) {
            val = false;
            System.out.println("Need Environment Name eg: CHOCO / CHOCO / TEST / UAT / PRD");
            System.out.println("From Date eg: 2016-12-23");
            System.out.println("To Date eg: 2016-12-24");
        }
        return val;
    }
}
