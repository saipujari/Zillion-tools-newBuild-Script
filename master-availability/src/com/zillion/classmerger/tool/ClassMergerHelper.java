package com.zillion.classmerger.tool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.zillion.api.MasterAvailabilityReportConstants;
import com.zillion.api.MasterAvailabilityReportHelper;
import com.zillion.api.ReportAPIHelper;
import com.zillion.api.ZillionUtil;
import com.zillion.classmerger.model.AccountRequest;
import com.zillion.classmerger.model.Classroom;
import com.zillion.classmerger.model.MoveMembersRequest;

public class ClassMergerHelper {
	
	public static String organizationId = "02";
	private static Connection connection = null;
	private static Properties classroomProperties = null;
			
	public static void mergeClasses(String sourceClassroomName, String targetClassroomName, String username, String password, String resourceBundlePath){
		try{
			
			System.out.println("Validation Begins : " );

			//Validate Required Parameters Available Or Not
			if(ClassMergerUtil.isNull(sourceClassroomName) || ClassMergerUtil.isNull(targetClassroomName) || ClassMergerUtil.isNull(username) || ClassMergerUtil.isNull(password) || ClassMergerUtil.isNull(resourceBundlePath)){
				System.out.println("Error ::: Insufficient Parameters to merge the classes");
				return;
			}
			
			//Validate Source & Target Classroom Name
			if(sourceClassroomName.equalsIgnoreCase(targetClassroomName)){
				System.out.println("Error ::: Source & Target Classroom Should not be same");
				return;
			}
			
			//Fetch Properties
			classroomProperties = ZillionUtil.getProperties(resourceBundlePath);
			
			//Initialize Database Connection
			ClassMergerHelper.initializeConnection();

			//Getting Source & Target Classrooms
			Classroom sourceClassroom = ClassMergerHelper.getClassroom(sourceClassroomName);
			Classroom targetClassroom = ClassMergerHelper.getClassroom(targetClassroomName);
			
			//Validate Source Classroom Exist Or Not
			if(ClassMergerUtil.isInvalidObject(sourceClassroom)){
				System.out.println("Error ::: Source classroom does not exist");
				return;
			}
			//Validate Target Classroom Exist Or Not
			if(ClassMergerUtil.isInvalidObject(targetClassroom)){
				System.out.println("Error ::: Target classroom does not exist");
				return;
			}
			
			System.out.println("Source & Target Classrooms are exist");
			
			//Validate source & target classrooms are in same program
			//Classes to be merged will include both the ILI and HBMI programs (NOTE: Merges are only within the same program, not across programs)
			if(!sourceClassroom.getMastHealthProgramId().equalsIgnoreCase(targetClassroom.getMastHealthProgramId())){
				System.out.println("Error ::: Source & Target Classroom Programs are differnet");
				return;
			}
			//Validate source & target classrooms are in same classficiation
			if(sourceClassroom.getClassificationMask().intValue()!=targetClassroom.getClassificationMask()){
				System.out.println("Error ::: Source & Target Classroom Classifications are differnet");
				return;
			}
			
			System.out.println("Source & Target Classrooms are same programs & same classification");
			
			Date currentDate = new Date();
			String approvedStatus="Approved";
			//Validate If Source Classroom are approved and started already
			if((!sourceClassroom.getStatus().equalsIgnoreCase(approvedStatus)) || sourceClassroom.getStartDt().after(currentDate)){
				System.out.println("Error ::: Source Classroom Not Approved Or Start Date is in Future");
				return;
			}
			//Validate If Target Classroom are approved and started already
			if((!targetClassroom.getStatus().equalsIgnoreCase(approvedStatus)) || targetClassroom.getStartDt().after(currentDate)){
				System.out.println("Error ::: Target Classroom Not Approved Or Start Date is in Future");
				return;
			}
			
			System.out.println("Source & Target Classrooms are in valid status and started already");
			
			//Validate Enough Seats Available Or Not
			if(targetClassroom.getAvailableSeats()<sourceClassroom.getAssignedSeats()){
				System.out.println("Error ::: Target Classroom Doesn't have enough space to accommodate members from source classroom");
				return;
			}

			System.out.println("Target Classrooms have enough space to accommodate members from source classroom");

			Object[] sourceClassroomInfoArray = ClassMergerHelper.getClassroomInterval(sourceClassroom);
			Object[] targetClassroomInfoArray = ClassMergerHelper.getClassroomInterval(targetClassroom);
			
			int sourceClassroomInterval = (Integer) sourceClassroomInfoArray[0];
			int targetClassroomInterval = (Integer) targetClassroomInfoArray[0];
			
			Boolean isSourceClassroomIntervalHasRestriction = (Boolean) sourceClassroomInfoArray[1];
			Boolean isTargetClassroomIntervalHasRestriction = (Boolean) targetClassroomInfoArray[1];
			
			if((isSourceClassroomIntervalHasRestriction || isTargetClassroomIntervalHasRestriction) && sourceClassroomInterval != targetClassroomInterval){
	             System.out.println("Classroom movement between Interval #0 and a future interval or from a future interval to Interval #0 is not allowed. Please enter an interval to interval combination that is valid.");
	             return;
			}
			
			int sourceClassroomGroupSessionCount = ClassMergerHelper.getClassroomSessionCount(sourceClassroom);
			int targetClassroomGroupSessionCount = ClassMergerHelper.getClassroomSessionCount(targetClassroom);
			
            if(sourceClassroomInterval==0 && targetClassroomInterval==0){
                if(sourceClassroomGroupSessionCount!=targetClassroomGroupSessionCount){
                    System.out.println("Trying to move the members from source to target classroom are not meeting the classroom session count");
                    return;
                }
            }
			
			int intervalDifference = sourceClassroomInterval-targetClassroomInterval;
			
			// Classes being merged will follow the current rule for moving members no more than +/- 2 classes from the current class.
			if(intervalDifference<-2 || intervalDifference>2){
				System.out.println("Trying to move the members from source to target classroom not meeting the interval range (+/- 2 intervals) " + (sourceClassroomInterval + " - " + targetClassroomInterval + " = " + intervalDifference));
				return;
			}
			
			System.out.println("Source & Target Classrooms are meeting the interval range");

			List<AccountRequest> sourceClassroomMembersList = ClassMergerHelper.getClassroomMembers(sourceClassroom.getId());
			
			if(ClassMergerUtil.isListEmpty(sourceClassroomMembersList)){
				System.out.println("Source Classroom Is Empty");
				return;
			}
			
			MoveMembersRequest moveMembersRequest = new MoveMembersRequest();
			moveMembersRequest.setIsOverride(true);
			moveMembersRequest.setMembersList(sourceClassroomMembersList);
			
			String requestBody = ClassMergerUtil.convertPojoToString(moveMembersRequest);
			
			System.out.println("Request Body : " + requestBody);
			if(ClassMergerUtil.isNull(requestBody)){
				System.out.println("Incorrect Request Body");
				return;
			}
			
			System.out.println("Request Building Are Done");
			
			//Initializing Parameters.
			Map<String,String> inputParams = new HashMap<String,String>();
			inputParams.put(MasterAvailabilityReportConstants.USERNAME, username);
			inputParams.put(MasterAvailabilityReportConstants.PASSWORD, password);
			inputParams.put(MasterAvailabilityReportConstants.RESOURCEBUNDLEPATH, resourceBundlePath);
			
			//Setting Up Parameters for Properties.
			MasterAvailabilityReportHelper.setProprties(inputParams);
			
			//Login into the application
			ReportAPIHelper.doLogin(username, password);
			
			System.out.println("Initialize parameters and login are done");
			
			System.out.println("Initiated Move Member API Call");
			
			ReportAPIHelper.generateMoveMembersRequest(targetClassroom.getId(), requestBody);
			
			System.out.println("Members Moved into target classroom successfully");
			
		} catch(Exception exception){
			exception.printStackTrace();
		} finally {
			ClassMergerHelper.closeConnection();
		}
	}
	
	
	public static void mergeTestClassesTemporary(String sourceClassroomName, String targetClassroomName, String username, String password, String resourceBundlePath){
		try{
			
			System.out.println("Validation Begins : " );

			//Fetch Properties
			classroomProperties = ZillionUtil.getProperties(resourceBundlePath);
			
			//Initialize Database Connection
			ClassMergerHelper.initializeConnection();

			System.out.println("Source & Target Classrooms are meeting the interval range");

			List<AccountRequest> sourceClassroomMembersList = new ArrayList<AccountRequest>();
			sourceClassroomMembersList.add(new AccountRequest("1F3FAAA87D5C758AE055000000000001"));
			
			if(ClassMergerUtil.isListEmpty(sourceClassroomMembersList)){
				System.out.println("Source Classroom Is Empty");
				return;
			}
			
			MoveMembersRequest moveMembersRequest = new MoveMembersRequest();
			moveMembersRequest.setIsOverride(true);
			moveMembersRequest.setMembersList(sourceClassroomMembersList);
			
			String requestBody = ClassMergerUtil.convertPojoToString(moveMembersRequest);
			
			System.out.println("Request Body : " + requestBody);
			if(ClassMergerUtil.isNull(requestBody)){
				System.out.println("Incorrect Request Body");
				return;
			}
			
			System.out.println("Request Building Are Done");
			
			//Initializing Parameters.
			Map<String,String> inputParams = new HashMap<String,String>();
			inputParams.put(MasterAvailabilityReportConstants.USERNAME, username);
			inputParams.put(MasterAvailabilityReportConstants.PASSWORD, password);
			inputParams.put(MasterAvailabilityReportConstants.RESOURCEBUNDLEPATH, resourceBundlePath);
			
			//Setting Up Parameters for Properties.
			MasterAvailabilityReportHelper.setProprties(inputParams);
			
			//Login into the application
			ReportAPIHelper.doLogin(username, password);
			
			System.out.println("Initialize parameters and login are done");
			
			System.out.println("Initiated Move Member API Call");
			
			ReportAPIHelper.generateMoveMembersRequest("1F3F9F6224507574E055000000000001", requestBody);
			
			System.out.println("Members Moved into target classroom successfully");
			
		} catch(Exception exception){
			exception.printStackTrace();
		} finally {
			ClassMergerHelper.closeConnection();
		}
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
	 * @param classroomName
	 * @return
	 * @throws SQLException
	 */
	private static Classroom getClassroom(String classroomName) throws SQLException{
		String query = "SELECT * FROM CLASSROOM WHERE NAME='" + classroomName + "'";
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
	 * @param classroomId
	 * @return
	 * @throws SQLException
	 */
	private static List<AccountRequest> getClassroomMembers(String classroomId) throws SQLException{
		Statement statement = null;
		Statement accountStatement=null;
		try {
			String accountProgramQuery = "SELECT * FROM CLASSROOM_ACCOUNT_PROGRAM WHERE IS_ACTIVE=1 AND CLASSROOM_ID='"+ classroomId +"'";
			statement = connection.createStatement();
			ResultSet resultset = statement.executeQuery(accountProgramQuery);
			ArrayList<String> programList = new ArrayList<String>();
	        while (resultset.next()) {
	        	programList.add(resultset.getString("ACCOUNT_PROGRAM_ID"));
	        }
	        String inClauseResult = "";
	        for (int index = 0; index< programList.size(); index++)
	        {
	        	inClauseResult = inClauseResult + "'" + programList.get(index) + "',";
	        }
	        inClauseResult = inClauseResult.substring(0, inClauseResult.length()-1);
	        System.out.println("Members moving from source classroom " + inClauseResult );
	        String accountQuery = "SELECT ACCOUNT_ID FROM ACCOUNT_PROGRAM WHERE ID IN (" + inClauseResult + ")";
	        accountStatement = connection.createStatement();
	        resultset = accountStatement.executeQuery(accountQuery);
	        ArrayList<AccountRequest> accountIdList = new ArrayList<AccountRequest>();
	        while (resultset.next()) {
	        	accountIdList.add(new AccountRequest(resultset.getString("ACCOUNT_ID")));
	        }
	        return accountIdList;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			statement.close();
		}
		return null;
	}
	
	private static int getClassroomSessionCount(Classroom classroom) throws SQLException{
	    String query = "SELECT COUNT(*) AS TOTAL_SESSIONS FROM CALENDAR_EVENT WHERE IS_GROUP_SESSION=1 AND CLASSROOM_ID=?";
        PreparedStatement statement = null;
        try{
            statement = connection.prepareStatement(query);
            statement.setString(1, classroom.getId());
            ResultSet resultset = statement.executeQuery();
            while(resultset.next()){
                return resultset.getInt("TOTAL_SESSIONS");
            }
        }  catch (SQLException e) {
            e.printStackTrace();
        }
        finally{
            statement.close();
        }
        return 0;
	}
	
	/**
	 * 
	 * @param classroom
	 * @return
	 * @throws SQLException
	 */
	private static Object[] getClassroomInterval(Classroom classroom) throws SQLException{
		Date startDate = classroom.getStartDt();
		Date endDate = new Date();
    	String timezoneKey = "America/New_York";
    	int weeks = 0, months=0, days=0, years = 0;
    	Object[] resultArray = new Object[2];
        if(endDate.after(startDate)){
        	weeks = ClassMergerUtil.getDifferenceBetweenTwoDates(startDate, endDate, timezoneKey, ClassMergerConstants.WEEKS_FREQUENCY.toString());
        	days = ClassMergerUtil.getDifferenceBetweenTwoDates(startDate, endDate, timezoneKey, ClassMergerConstants.DAYS_FREQUENCY.toString());
        	months = ClassMergerUtil.getDifferenceBetweenTwoDates(startDate, endDate, timezoneKey, ClassMergerConstants.MONTHS_FREQUENCY.toString());
        	years = ClassMergerUtil.getDifferenceBetweenTwoDates(startDate, endDate, timezoneKey, ClassMergerConstants.YEARS_FREQUENCY.toString());
        }
    	String query = "SELECT * FROM MP_INTERVAL WHERE MAST_PROGRAM_ID=? AND ( (START_OFFSET=? AND UPPER(START_OFFSET_UNITS)='WEEKS') OR (START_OFFSET=? AND UPPER(START_OFFSET_UNITS)='MONTHS') OR (START_OFFSET=? AND UPPER(START_OFFSET_UNITS)='DAYS') OR (START_OFFSET=? AND UPPER(START_OFFSET_UNITS)='YEARS')) ORDER BY INTERVAL_NUMBER DESC";
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
    		    resultArray[0] = (Integer) resultset.getInt("INTERVAL_NUMBER");
    		    resultArray[1] = (Boolean) resultset.getBoolean("IS_MOVEMENT_HAS_RESTRICTION");
    			return resultArray;
    		}
    	}  catch (SQLException e) {
			e.printStackTrace();
		}
		finally{
			statement.close();
		}
        return null;
	}
	
}