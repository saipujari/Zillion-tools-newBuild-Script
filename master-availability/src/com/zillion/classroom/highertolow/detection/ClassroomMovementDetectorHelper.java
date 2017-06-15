package com.zillion.classroom.highertolow.detection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.zillion.api.MasterAvailabilityReportConstants;
import com.zillion.api.ZillionUtil;
import com.zillion.classmerger.model.Classroom;
import com.zillion.classmerger.tool.ClassMergerConstants;
import com.zillion.classmerger.tool.ClassMergerUtil;

public class ClassroomMovementDetectorHelper {
	
	public static String organizationId = "02";
	private static Connection connection = null;
	private static Properties classroomProperties = null;
			
	@SuppressWarnings("rawtypes")
	public static void detectClasses(String resourceBundlePath, String totalDaysBack){
		try{
			
			System.out.println("========================================================================================================" );

			//Fetch Properties
			classroomProperties = ZillionUtil.getProperties(resourceBundlePath);
			
			//Initialize Database Connection
			ClassroomMovementDetectorHelper.initializeConnection();

			HashMap<String, ClassroomEntity> accountProgramHistory = new HashMap<String,ClassroomEntity>();
			HashMap<String, ClassroomEntity> higherToLowerIntervalHistory = new HashMap<String,ClassroomEntity>();
			HashMap<String, ClassroomEntity> lowerToHigherIntervalHistory = new HashMap<String,ClassroomEntity>();
			HashMap<String, ClassroomEntity> sameIntervalHistory = new HashMap<String,ClassroomEntity>();
			
			List<String> accountProgramIdList = new ArrayList<String>();
			HashMap<String, ClassroomEntity> currentClassroomHistory = new HashMap<String,ClassroomEntity>();
			
			//Retrieve Classroom List
			List<ClassroomEntity> currentClassroomHistoryList =  ClassroomMovementDetectorHelper.getClassroomMovementAgainstNewClassroom(totalDaysBack);
			for(ClassroomEntity currentClassroomEntity : currentClassroomHistoryList){
				currentClassroomHistory.put(currentClassroomEntity.getAccountProgramId(), currentClassroomEntity);
				accountProgramIdList.add(currentClassroomEntity.getAccountProgramId());
				//System.out.println("New Classroom Values : " + entity.getClassroomId() + "-" + entity.getAccountProgramId() + "-" + entity.getNewClassroomDate());
			}
			System.out.println("New Classroom Count : " + currentClassroomHistory.size());
			List<ClassroomEntity> oldClassroomHistory =  ClassroomMovementDetectorHelper.getOldClassroomMovement(accountProgramIdList);
			System.out.println("Old Classroom History Count : " + oldClassroomHistory.size());
			for(ClassroomEntity oldclassroomEntity : oldClassroomHistory){
				String key = oldclassroomEntity.getAccountProgramId();
				ClassroomEntity currentClassroomEntity = currentClassroomHistory.get(key);
				if(currentClassroomEntity!=null){
					Classroom oldClassroom = ClassroomMovementDetectorHelper.getClassroom(oldclassroomEntity.getClassroomId());
					int oldClassroomIntervalNumber = getClassroomInterval(oldClassroom, oldclassroomEntity.getAssignedEndDate());
					Classroom newClassroom = ClassroomMovementDetectorHelper.getClassroom(currentClassroomEntity.getClassroomId());
					int newClassroomIntervalNumber = getClassroomInterval(newClassroom, currentClassroomEntity.getAssignedStartDate());
					//System.out.println("Final Observation Values : Account Program Id :: " + oldclassroomEntity.getAccountProgramId() + " :: NewClassroomId :: " + newClassroom.getId()  + " :: NewClassroomDate :: " + newClassroom.getStartDt()  + " :: OldClassroomId :: " + oldClassroom.getId()  + " :: OldClassroomDate :: " + oldClassroom.getStartDt());
					ClassroomEntity classroomEntity = new ClassroomEntity();
					classroomEntity.setClassroomId(newClassroom.getId());
					classroomEntity.setOldClassroomId(oldClassroom.getId());
					classroomEntity.setAccountProgramId(oldclassroomEntity.getAccountProgramId());
					classroomEntity.setOldClassroomDate(oldClassroom.getStartDt());
					classroomEntity.setNewClassroomDate(newClassroom.getStartDt());
					classroomEntity.setOldClassroomIntervalNumber(String.valueOf(oldClassroomIntervalNumber));
					classroomEntity.setNewClassroomIntervalNumber(String.valueOf(newClassroomIntervalNumber));
					
					if(accountProgramHistory.get(key)==null){
						accountProgramHistory.put(currentClassroomEntity.getAccountProgramId(),classroomEntity);
					}
					else{
						accountProgramHistory.put(currentClassroomEntity.getAccountProgramId(),classroomEntity);
						sameIntervalHistory.remove(currentClassroomEntity.getAccountProgramId());
						lowerToHigherIntervalHistory.remove(currentClassroomEntity.getAccountProgramId());
						higherToLowerIntervalHistory.remove(currentClassroomEntity.getAccountProgramId());
					}
					
					if(oldClassroomIntervalNumber==newClassroomIntervalNumber){
						sameIntervalHistory.put(currentClassroomEntity.getAccountProgramId(),classroomEntity);
					}
					else if(oldClassroomIntervalNumber>newClassroomIntervalNumber){
						higherToLowerIntervalHistory.put(currentClassroomEntity.getAccountProgramId(),classroomEntity);
					}
					else if(oldClassroomIntervalNumber<newClassroomIntervalNumber){
						lowerToHigherIntervalHistory.put(currentClassroomEntity.getAccountProgramId(),classroomEntity);
					}
				}
			}
			
			System.out.println("========================================================================================================" );
			System.out.println("Same Interval Movement");
			System.out.println("========================================================================================================" );
			System.out.println("Account Id, Account Program Id, Old Classroom Id, Old Classroom Interval #, New Classroom Id, New Classroom Interval #" );
			System.out.println("========================================================================================================" );
			Iterator it = sameIntervalHistory.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        ClassroomEntity entity = (ClassroomEntity) pair.getValue();
		        String accountId = ClassroomMovementDetectorHelper.getAccountId(entity.getAccountProgramId());
		        System.out.println(accountId + "," + entity.getAccountProgramId() + "," + entity.getOldClassroomId() + "," + entity.getOldClassroomIntervalNumber() + "," + entity.getClassroomId() + "," + entity.getNewClassroomIntervalNumber() );
		    }
		    System.out.println("========================================================================================================" );
			System.out.println("Higher To Lower Interval Movement");
			System.out.println("========================================================================================================" );
			System.out.println("Account Id, Account Program Id, Old Classroom Id, Old Classroom Interval #, New Classroom Id, New Classroom Interval #" );
			System.out.println("========================================================================================================" );
			it = higherToLowerIntervalHistory.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        ClassroomEntity entity = (ClassroomEntity) pair.getValue();
		        String accountId = ClassroomMovementDetectorHelper.getAccountId(entity.getAccountProgramId());
		        System.out.println(accountId + "," + entity.getAccountProgramId() + "," + entity.getOldClassroomId() + "," + entity.getOldClassroomIntervalNumber() + "," + entity.getClassroomId() + "," + entity.getNewClassroomIntervalNumber() );
		        it.remove(); // avoids a ConcurrentModificationException
		    }
			System.out.println("========================================================================================================" );
		    System.out.println("Lower To Higher Interval Movement");
			System.out.println("========================================================================================================" );
			System.out.println("Account Id, Account Program Id, Old Classroom Id, Old Classroom Interval #, New Classroom Id, New Classroom Interval #" );
			System.out.println("========================================================================================================" );
			it = lowerToHigherIntervalHistory.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        ClassroomEntity entity = (ClassroomEntity) pair.getValue();
		        String accountId = ClassroomMovementDetectorHelper.getAccountId(entity.getAccountProgramId());
		        System.out.println(accountId + "," + entity.getAccountProgramId() + "," + entity.getOldClassroomId() + "," + entity.getOldClassroomIntervalNumber() + "," + entity.getClassroomId() + "," + entity.getNewClassroomIntervalNumber() );
		        it.remove(); // avoids a ConcurrentModificationException
		    }
			System.out.println("========================================================================================================" );
		} catch(Exception exception){
			exception.printStackTrace();
		} finally {
			ClassroomMovementDetectorHelper.closeConnection();
		}
	}
	
	/**
	 * 
	 * @param classroomName
	 * @return
	 * @throws SQLException
	 */
	private static Classroom getClassroom(String classroomId) throws SQLException{
		String query = "SELECT * FROM CLASSROOM WHERE ID='" + classroomId + "'";
		Statement statement = null;
		try {
			statement = connection.createStatement();
			ResultSet resultset = statement.executeQuery(query);
			Classroom classroom = new Classroom();
	        while (resultset.next()) {
	        	classroom.setId(resultset.getString("ID"));
	        	classroom.setName(resultset.getString("NAME"));
	        	classroom.setStartDt(resultset.getDate("START_DT"));
	        	classroom.setEndDt(resultset.getDate("END_DT"));
	        	classroom.setIsApproved(resultset.getBoolean("IS_APPROVED"));
	        	classroom.setStatus(resultset.getString("STATUS"));
	        	classroom.setClassificationMask(resultset.getLong("CLASSIFICATION_MASK"));
	        	classroom.setAssignedSeats(resultset.getInt("ASSIGNED_SEATS"));
	        	classroom.setAvailableSeats(resultset.getInt("AVAILABLE_SEATS"));
	        	classroom.setAssignedCoachId(resultset.getString("PROVIDER_ID"));
	        	classroom.setMastHealthProgramId(resultset.getString("MAST_PROGRAM_ID"));
	        	return classroom;
	        }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally{
			if(statement!=null)
				statement.close();
		}
		return null;
	}
	
	
	/**
	 * 
	 */
	private static void initializeConnection(){
		try{
			String databaseDriver = classroomProperties.getProperty(MasterAvailabilityReportConstants.DBDRIVER);
			String databaseUrl = classroomProperties.getProperty(MasterAvailabilityReportConstants.DBURL);
			String databaseUser = classroomProperties.getProperty(MasterAvailabilityReportConstants.DBUSER);
			String databasePassword = classroomProperties.getProperty(MasterAvailabilityReportConstants.DBPASSWORD);
			Class.forName(databaseDriver);
			connection = DriverManager.getConnection(databaseUrl, databaseUser,databasePassword);
		} catch(Exception exception){
			exception.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	private static void closeConnection(){
		try{
			if(connection!=null)
				connection.close();
		} catch(Exception exception){
			exception.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param totalDaysBack
	 * @return
	 * @throws SQLException
	 */
	private static List<ClassroomEntity> getClassroomMovementAgainstNewClassroom(String totalDaysBack) throws SQLException{
		List<ClassroomEntity> entityList = new ArrayList<ClassroomEntity>(); 
		String query = "SELECT ACCOUNT_PROGRAM_ID, CLASSROOM_ID, ASSIGNED_END_DT, ASSIGNED_START_DT FROM CLASSROOM_ACCOUNT_PROGRAM WHERE IS_ACTIVE=1 AND CREATED_DT<SYSDATE AND CREATED_DT>SYSDATE + INTERVAL '-" + totalDaysBack +"' DAY ORDER BY CREATED_DT ASC";
		Statement statement = null;
		try {
			statement = connection.createStatement();
			ResultSet resultset = statement.executeQuery(query);
	        while (resultset.next()) {
				ClassroomEntity classroom = new ClassroomEntity();
				classroom.setClassroomId(resultset.getString("CLASSROOM_ID"));
				classroom.setAccountProgramId(resultset.getString("ACCOUNT_PROGRAM_ID"));
				classroom.setAssignedEndDate(resultset.getDate("ASSIGNED_END_DT"));
				classroom.setAssignedStartDate(resultset.getDate("ASSIGNED_START_DT"));
				entityList.add(classroom);
	        }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally{
			if(statement!=null)
				statement.close();
		}
		return entityList;
	}
	
	/**
	 * 
	 * @param accountProgramIdList
	 * @return
	 * @throws SQLException
	 */
	private static List<ClassroomEntity> getOldClassroomMovement(List<String> accountProgramIdList) throws SQLException{
		List<ClassroomEntity> entityList = new ArrayList<ClassroomEntity>();
		String queryString ="";
		int index=0;
		List<String> queryStringList = new ArrayList<String>();
		for(String accountProgramId : accountProgramIdList){
			queryString = queryString + "'" + accountProgramId + "',";
			index++;
			if(index==500){
				queryString = queryString.substring(0,queryString.length()-1);
				queryStringList.add(queryString);
				queryString = "";
				index=0;
			}
		}
		queryString = queryString.substring(0,queryString.length()-1);
		queryStringList.add(queryString);
		
		for(String queryStringFromList : queryStringList){
			String query = "SELECT ACCOUNT_PROGRAM_ID, CLASSROOM_ID, ASSIGNED_END_DT, ASSIGNED_START_DT FROM CLASSROOM_ACCOUNT_PROGRAM WHERE IS_ACTIVE=0 AND ACCOUNT_PROGRAM_ID IN ("+ queryStringFromList +") ORDER BY CREATED_DT ASC";
			Statement statement = null;
			try {
				statement = connection.createStatement();
				ResultSet resultset = statement.executeQuery(query);
		        while (resultset.next()) {
					ClassroomEntity classroom = new ClassroomEntity();
					classroom.setClassroomId(resultset.getString("CLASSROOM_ID"));
					classroom.setAccountProgramId(resultset.getString("ACCOUNT_PROGRAM_ID"));
					classroom.setAssignedEndDate(resultset.getDate("ASSIGNED_END_DT"));
					classroom.setAssignedStartDate(resultset.getDate("ASSIGNED_START_DT"));
					entityList.add(classroom);
		        }
			} catch (SQLException e) {
				e.printStackTrace();
			}
			finally{
				if(statement!=null)
					statement.close();
			}
		}
		return entityList;
	}
	
	/**
	 * 
	 * @param accountId
	 * @return
	 * @throws SQLException
	 */
	private static String getAccountId(String accountProgramId) throws SQLException{
		String query = "SELECT ACCOUNT_ID FROM ACCOUNT_PROGRAM WHERE IS_ACTIVE=1 AND ID='"+ accountProgramId +"'";
		Statement statement = null;
		try {
			statement = connection.createStatement();
			ResultSet resultset = statement.executeQuery(query);
	        while (resultset.next()) {
				return resultset.getString("ACCOUNT_ID");
	        }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally{
			if(statement!=null)
				statement.close();
		}
		return "";
	}
	
	/**
	 * 
	 * @param classroom
	 * @return
	 * @throws SQLException
	 */
	private static int getClassroomInterval(Classroom classroom, Date endDate) throws SQLException{
		Date startDate = classroom.getStartDt();
    	String timezoneKey = "America/New_York";
    	int weeks = 0, months=0, days=0, years = 0;
        if(endDate.after(startDate)){
        	weeks = ClassMergerUtil.getDifferenceBetweenTwoDates(startDate, endDate, timezoneKey, ClassMergerConstants.WEEKS_FREQUENCY.toString());
        	days = ClassMergerUtil.getDifferenceBetweenTwoDates(startDate, endDate, timezoneKey, ClassMergerConstants.DAYS_FREQUENCY.toString());
        	months = ClassMergerUtil.getDifferenceBetweenTwoDates(startDate, endDate, timezoneKey, ClassMergerConstants.MONTHS_FREQUENCY.toString());
        	years = ClassMergerUtil.getDifferenceBetweenTwoDates(startDate, endDate, timezoneKey, ClassMergerConstants.YEARS_FREQUENCY.toString());
        }
    	String query = "SELECT * FROM MP_INTERVAL WHERE MAST_PROGRAM_ID=? AND ( (START_OFFSET=? AND START_OFFSET_UNITS='WEEKS') OR (START_OFFSET=? AND START_OFFSET_UNITS='MONTHS') OR (START_OFFSET=? AND START_OFFSET_UNITS='DAYS') OR (START_OFFSET=? AND START_OFFSET_UNITS='YEARS')) ORDER BY INTERVAL_NUMBER DESC";
    	PreparedStatement statement = null;
    	try{
        	statement = connection.prepareStatement(query);
        	statement.setString(1, classroom.getMastHealthProgramId());
        	statement.setInt(2, weeks);
        	statement.setInt(3, months);
        	statement.setInt(4, days);
        	statement.setInt(5, years);
    		ResultSet resultset = statement.executeQuery();
    		while(resultset.next()){
    			return resultset.getInt("INTERVAL_NUMBER");
    		}
    	}  catch (SQLException e) {
			e.printStackTrace();
		}
		finally{
			statement.close();
		}
        return 1;
	}
	
}