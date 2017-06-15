package com.tracker.utility;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import com.tracker.db.TrackerDbHelper;

public class DataUtil {

/**
 * @param args
 */
	public static void main(String[] args) {
		try {
			if(validateInput(args)) {
				if(args[1].equals("weights")) {
					saveWeights(args);
				} else if(args[1].equals("steps")) {
					saveSteps(args);
				}
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
/**
 * 
 * @param args
 * @throws Exception
 * Save random weight between 100 to 200  for given member 
 */
	private static void saveWeights(String[] args) throws Exception {
		Connection connection = null;
		Statement  stmt = null;
		Date etDate = new Date();
		Calendar c = Calendar.getInstance(); 
		c.setTime(etDate); 
		c.add(Calendar.MONTH, (-1 * Integer.parseInt(args[3])));
		Date stDate = c.getTime();
		
		Double weight = 0.0;
		Double height = 70.0;
		Double bmi =0.0;
		String weightSql ="";
		String bmiSql ="";
		String accountId = args[2];
		// Weight tracker measurement id
		String weightMeasurementId="3393EE56BC646E00E053B80CA8C0CA43";
		// BMI tracker measurement id
		String bmiMeasurementId="3393EE56BC656E00E053B80CA8C0CA43";
		
		connection = TrackerDbHelper.getDatabaseConnection(args[0]);
		System.out.println("Connection done");
		stmt = connection.createStatement();
		Random random = new Random();
		while(stDate.before(etDate) || stDate.compareTo(etDate)==0 ){
			
			weight = new Double(random.nextInt(200 - 100 + 1) + 100);			
			
			bmi = (weight/(height.doubleValue()*height.doubleValue()))*703;		
			weightSql = "INSERT INTO ACCOUNT_TRK_MEASUREMENT (ID,MEASUREMENT_TYPE,DATE_LOGGED,SOURCE,SOURCE_NAME,VALIDATED,IS_PRIMARY,IS_DELETED,ACCOUNT_ID,VALUE,MEASUREMENT_ID)"
				   +" VALUES(SYS_GUID(),'WEIGHT',to_date('"+new java.sql.Date(stDate.getTime())+"','yyyy-MM-dd'),'MANUAL','MANUAL',0,1,0,'"+accountId+"',"+weight+",'"+weightMeasurementId+"')";
			bmiSql = "INSERT INTO ACCOUNT_TRK_MEASUREMENT (ID,MEASUREMENT_TYPE,DATE_LOGGED,SOURCE,SOURCE_NAME,VALIDATED,IS_PRIMARY,IS_DELETED,ACCOUNT_ID,VALUE,MEASUREMENT_ID)"
					   +" VALUES(SYS_GUID(),'WEIGHT',to_date('"+new java.sql.Date(stDate.getTime())+"','yyyy-MM-dd'),'MANUAL','MANUAL',0,1,0,'"+accountId+"',"+bmi+",'"+bmiMeasurementId+"')";
			
			 stmt.executeUpdate(weightSql);
			 System.out.println("Weight inserted for "+stDate);
			 stmt.executeUpdate(bmiSql);
			 System.out.println("BMI inserted for "+stDate);
			 stDate = getEndDate(stDate,1);
		}
		stmt.close();
		connection.close();
	}
/**
 * 
 * @param args
 * @throws Exception
 * Save random Steps between 500 to 700  for given member 
 */
	private static void saveSteps(String[] args) throws Exception {
		Connection connection = null;
		Statement  stmt = null;
		Date etDate = new Date();
		Calendar c = Calendar.getInstance(); 
		c.setTime(etDate); 
		c.add(Calendar.MONTH, (-1 * Integer.parseInt(args[3])));
		Date stDate = c.getTime();
		
		String accountId = args[2];
		// Steps tracker measurement id
		String stepsMeasurementId="3393EE56BC676E00E053B80CA8C0CA43";
		int steps =0;
		String sql ="";
		
		connection = TrackerDbHelper.getDatabaseConnection(args[0]);
		System.out.println("Connection done");
		stmt = connection.createStatement();
		Random random = new Random();
		while(stDate.before(etDate) || stDate.compareTo(etDate)==0 ){
			steps = random.nextInt(700 - 500 + 1) + 500;		
			
			sql = "INSERT INTO ACCOUNT_TRK_MEASUREMENT (ID,MEASUREMENT_TYPE,DATE_LOGGED,SOURCE,SOURCE_NAME,VALIDATED,IS_PRIMARY,IS_DELETED,ACCOUNT_ID,VALUE,MEASUREMENT_ID)"
					   +" VALUES(SYS_GUID(),'ROUTINE',to_date('"+new java.sql.Date(stDate.getTime())+"','yyyy-MM-dd'),'MANUAL','MANUAL',0,1,0,'"+accountId+"',"+steps+",'"+stepsMeasurementId+"')";
			
			 stmt.executeUpdate(sql);
			 System.out.println("Steps inserted for "+stDate);
			 stDate = getEndDate(stDate,1);
		}
	}
/**
 * 	
 * @param args
 * @return
 */
	private static boolean validateInput(String[] args) {
		boolean val = true;
		if(args.length != 4) {
			val = false;
			System.out.println("Need Environment Name eg: RR / CHOCO / KULFI / PRD \n " +
					"Type eg: steps / weights \n " +
					"Member Id eg: 20EC7117F62C4C96E055000000000001 \n" +
					"Months eg: 3 \n");
		}
		return val;
	}
/**
 * 	
 * @param startDate
 * @param days
 * @return
 */
	private static Date getEndDate(Date startDate,int days){
		 Calendar cal = Calendar.getInstance();
		 cal.setTime(startDate);
		 cal.add(Calendar.DATE, days);
		 return cal.getTime();
	}

}
