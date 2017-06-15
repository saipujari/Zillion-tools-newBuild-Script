package com.zillion.availability.report;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.joda.time.DateTime;

import com.zillion.api.CsvExportUtil;
import com.zillion.api.MasterAvailabilityReportConstants;
import com.zillion.api.MasterAvailabilityReportHelper;
import com.zillion.api.ZillionUtil;
import com.zillion.availability.model.CoachInfo;
import com.zillion.availability.model.TimeBlock;
import com.zillion.availability.model.TimeSegment;
import com.zillion.db.ReportDbHelper;

public class AvailabilityLoader {

	public static Connection getDatabaseConnection() throws ClassNotFoundException, SQLException, Exception{
		System.out.println("Getting DB Connection");
		Connection connection =ReportDbHelper.getDatabaseConnection(); 
		return connection;
	}

	/*

	private static String[] getQueriesForWeek1(String coachId,String startDate,String endDate){
		String availabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Available' AND HOST_ID='" + coachId + "' AND START_DT_TIME>SYSDATE AND START_DT_TIME>TO_DATE('"+endDate+"','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-04 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String unavailabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Unavailable' AND HOST_ID='" + coachId + "' AND START_DT_TIME>SYSDATE AND START_DT_TIME>TO_DATE('"+endDate+"','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-04 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String sessionQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Session' AND HOST_ID='" + coachId + "' AND START_DT_TIME>SYSDATE AND SESSION_STATUS IN ('Scheduled','Rescheduled') AND START_DT_TIME>TO_DATE('"+endDate+"','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-04 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String queriesForWeek1[] = { availabilityQuery, unavailabilityQuery, sessionQuery };
		return queriesForWeek1;
	}
	
	private static String[] getQueriesForWeek2(String coachId){
		String availabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Available' AND HOST_ID='" + coachId + "' AND START_DT_TIME>TO_DATE('2015-10-05 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-11 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String unavailabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Unavailable' AND HOST_ID='" + coachId + "' AND START_DT_TIME>TO_DATE('2015-10-05 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-11 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String sessionQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Session' AND HOST_ID='" + coachId + "' AND SESSION_STATUS IN ('Scheduled','Rescheduled') AND START_DT_TIME>TO_DATE('2015-10-05 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-11 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String queriesForWeek1[] = { availabilityQuery, unavailabilityQuery, sessionQuery };
		return queriesForWeek1;
	}
	
	private static String[] getQueriesForWeek3(String coachId){
		String availabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Available' AND HOST_ID='" + coachId + "' AND START_DT_TIME>TO_DATE('2015-10-12 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-18 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String unavailabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Unavailable' AND HOST_ID='" + coachId + "' AND START_DT_TIME>TO_DATE('2015-10-12 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-18 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String sessionQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Session' AND HOST_ID='" + coachId + "' AND SESSION_STATUS IN ('Scheduled','Rescheduled') AND START_DT_TIME>TO_DATE('2015-10-12 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-18 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String queriesForWeek1[] = { availabilityQuery, unavailabilityQuery, sessionQuery };
		return queriesForWeek1;
	}
	
	private static String[] getQueriesForWeek4(String coachId){
		String availabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Available' AND HOST_ID='" + coachId + "' AND START_DT_TIME>TO_DATE('2015-10-19 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-25 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String unavailabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Unavailable' AND HOST_ID='" + coachId + "' AND START_DT_TIME>TO_DATE('2015-10-19 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-25 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String sessionQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Session' AND HOST_ID='" + coachId + "' AND SESSION_STATUS IN ('Scheduled','Rescheduled') AND START_DT_TIME>TO_DATE('2015-10-19 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-10-25 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String queriesForWeek1[] = { availabilityQuery, unavailabilityQuery, sessionQuery };
		return queriesForWeek1;
	}

	private static String[] getQueriesForWeek5(String coachId){
		String availabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Available' AND HOST_ID='" + coachId + "' AND START_DT_TIME>TO_DATE('2015-10-26 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-11-01 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String unavailabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Unavailable' AND HOST_ID='" + coachId + "' AND START_DT_TIME>TO_DATE('2015-10-26 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-11-01 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String sessionQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE EVENT_TYPE='Session' AND HOST_ID='" + coachId + "' AND SESSION_STATUS IN ('Scheduled','Rescheduled') AND START_DT_TIME>TO_DATE('2015-10-26 07:00:00','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('2015-11-01 07:00:00','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String queriesForWeek1[] = { availabilityQuery, unavailabilityQuery, sessionQuery };
		return queriesForWeek1;
	}
	
	*/
	
	private static String[] getQueriesForWeek(String coachId,String startDate,String endDate){
		String availabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE IS_RECURRENCE=0 AND EVENT_TYPE='Available' AND HOST_ID='" + coachId + "' AND START_DT_TIME>TO_DATE('"+startDate+"','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('"+endDate+"','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String unavailabilityQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE IS_RECURRENCE=0 AND EVENT_TYPE='Unavailable' AND HOST_ID='" + coachId + "' AND START_DT_TIME>TO_DATE('"+startDate+"','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('"+endDate+"','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String sessionQuery = "SELECT START_DT_TIME, END_DT_TIME FROM CALENDAR_EVENT WHERE IS_RECURRENCE=0 AND EVENT_TYPE='Session' AND HOST_ID='" + coachId + "' AND SESSION_STATUS IN ('Scheduled','Rescheduled') AND START_DT_TIME>TO_DATE('"+startDate+"','YYYY-MM-DD HH24:MI:SS') AND START_DT_TIME<TO_DATE('"+endDate+"','YYYY-MM-DD HH24:MI:SS') ORDER BY START_DT_TIME ASC";
		String queriesForWeek1[] = { availabilityQuery, unavailabilityQuery, sessionQuery };
		return queriesForWeek1;
	}

	/**
	 * 
	 * @param inputParams
	 */
	@SuppressWarnings("unused")
	public static void generateCapacityReport(Map<String,String> inputParams){
		String sessionTypeIds = inputParams.get(MasterAvailabilityReportConstants.SESSIONTYPEID);
		String coachIdsToIgnore = inputParams.get(MasterAvailabilityReportConstants.COACHIDS_TO_IGNORE);
		String startDate = inputParams.get(MasterAvailabilityReportConstants.STARTDATE);
		int noOfWeeks = Integer.parseInt(inputParams.get(MasterAvailabilityReportConstants.NOOFWEEKS));
		String timeZone = inputParams.get(MasterAvailabilityReportConstants.TIMEZONE);
		String resourcePath = inputParams.get(MasterAvailabilityReportConstants.RESOURCEBUNDLEPATH);
		Long duration = Long.parseLong(inputParams.get(MasterAvailabilityReportConstants.SESSION_DURATION));
		String includeId = inputParams.get(MasterAvailabilityReportConstants.INCLUDEID);
		MasterAvailabilityReportHelper.setProprties(inputParams);
		Connection connection = null;
	    Map<String,StringBuilder> capacityReportData = new HashMap<String,StringBuilder>();
		try {
			connection = AvailabilityLoader.getDatabaseConnection();
			ArrayList<CoachInfo> coachIdList = AvailabilityLoader.getActiveCoachIds(connection,sessionTypeIds,coachIdsToIgnore);
			Date date = ZillionUtil.getDateInUTC(startDate ,timeZone,MasterAvailabilityReportConstants.DATE_FORMAT_1);
			DateTime dateTime = new DateTime(date);
			DateTime startDateTime = null;
			DateTime endDateTime = null;
			String startDateString = null;
			String endDateString = null;
			String[] queriesArray  = null;
			StringBuilder reportdata = new StringBuilder();
			for(int weekNumber = 1; weekNumber <= noOfWeeks;weekNumber++){
				reportdata = new StringBuilder();
				if(includeId.equalsIgnoreCase("YES")){
					reportdata.append("Coach Id").append(",");
				}
				reportdata.append( "Coach Name," + "Available Sessions(Full),Unavailable Sessions,Scheduled Sessions,Total Sessions Available").append("\n");
				if(weekNumber == 1){   					  
					if(ZillionUtil.isDateInCurrentWeek(date)){
					    startDateTime =  new DateTime();						   
					  }
					else{
						  startDateTime = ZillionUtil.getStartDateofWeek(dateTime);
					  }
				  	  endDateTime = ZillionUtil.getEndDateofWeek(dateTime);
				  	  startDateString=  ZillionUtil.getDateAsString(startDateTime.toDate(), MasterAvailabilityReportConstants.DATE_FORMAT_2);
				  	  endDateString=  ZillionUtil.getDateAsString(endDateTime.toDate(), MasterAvailabilityReportConstants.DATE_FORMAT_2);
				}
				else{
					  startDateTime = ZillionUtil.getStartDateofWeek(endDateTime.plusDays(1));
					  endDateTime = ZillionUtil.getEndDateofWeek(startDateTime);						  
					  startDateString=  ZillionUtil.getDateAsString(startDateTime.toDate(), MasterAvailabilityReportConstants.DATE_FORMAT_2);
				      endDateString=  ZillionUtil.getDateAsString(endDateTime.toDate(), MasterAvailabilityReportConstants.DATE_FORMAT_2);
				}
 				for(CoachInfo coach : coachIdList){
 					if(includeId.equalsIgnoreCase("YES")){
						reportdata.append(coach.getId()).append(",");
					}
 					DateTime stDateTime = new DateTime();
					String coachId=coach.getId();
					String coachName = coach.getName();	
					queriesArray = AvailabilityLoader.getQueriesForWeek(coachId,startDateString,endDateString);
					reportdata.append(generateReport(queriesArray,connection,coachId,coachName, duration));
					DateTime edDateTime = new DateTime();
					System.out.println("Time taken to constract report data for" + coachId + "("+startDateString+") :"+(edDateTime.getMillis()-stDateTime.getMillis()) +"ms");
					System.out.println();
    			}
				capacityReportData.put(ZillionUtil.getDateAsString(startDateTime.toDate(), MasterAvailabilityReportConstants.DATE_FORMAT_1), reportdata);
			}
			
			System.out.println("Export CSV Started");
			CsvExportUtil.exportCsv(capacityReportData,MasterAvailabilityReportHelper.masterAvilabilityResource.getProperty("path"),MasterAvailabilityReportConstants.CAPACITY_REPORTNAME);
			System.out.println("Capacity report CSV file exported successfully ...");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			if(null != connection){
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * 
	 * @param queriesArray
	 * @param connection
	 * @param coachId
	 * @param coachName
	 * @param duration
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings("unused")
	public static String generateReport(String[] queriesArray,Connection connection,String coachId,String coachName, Long duration) throws SQLException{
		System.out.println("Generating report for "+coachId);
		StringBuilder reportData =  new StringBuilder();
		String availabilityQuery = queriesArray[0];
		String unavailabilityQuery = queriesArray[1];
		String sessionQuery = queriesArray[2];
		System.out.println("AvailabilityQuery "+availabilityQuery);
		System.out.println("UnavailabilityQuery "+unavailabilityQuery);
		System.out.println("SessionQuery "+sessionQuery);
		ArrayList<TimeBlock> availableTimeblocks = AvailabilityLoader.getTimeblocks(connection, availabilityQuery, TimeBlock.BlockType.AVAILABLE);
		ArrayList<TimeBlock> unavailableTimeblocks = AvailabilityLoader.getTimeblocks(connection, unavailabilityQuery, TimeBlock.BlockType.UNAVAILABLE);
		ArrayList<TimeBlock> scheduledTimeblocks = AvailabilityLoader.getTimeblocks(connection, sessionQuery, TimeBlock.BlockType.UNAVAILABLE);
		ArrayList<TimeBlock> allBusyTimeblocks = new ArrayList<TimeBlock>( unavailableTimeblocks );
		allBusyTimeblocks.addAll(scheduledTimeblocks);
		TreeSet<Long> coachAvailability = AvailabilityReport.getCoachAvailability( coachId, availableTimeblocks );
		TreeSet<Long> coachUnavailbilityRemoved = AvailabilityReport.removeCoachUnavailability( coachId, unavailableTimeblocks, coachAvailability );
		TreeSet<Long> coachScheduledRemoved = AvailabilityReport.removeCoachUnavailability( coachId, scheduledTimeblocks, coachAvailability );
		TreeSet<Long> coachAllBusyTimeRemoved = AvailabilityReport.removeCoachUnavailability( coachId, allBusyTimeblocks, coachAvailability );
		Iterator<Long> iterator = coachAvailability.iterator();
		while (iterator.hasNext()) {
			iterator.next();
		}
		TreeMap< Long, TimeSegment > availableSessionsAvailabilityFull = AvailabilityReport.getCoachAvailableSessions(coachId, duration, coachAvailability);
		TreeMap< Long, TimeSegment > availableSessionsUnavailabilityRemoved = AvailabilityReport.getCoachAvailableSessions(coachId, duration, coachUnavailbilityRemoved);
		TreeMap< Long, TimeSegment > availableSessionsScheduledRemoved = AvailabilityReport.getCoachAvailableSessions(coachId, duration, coachScheduledRemoved);
		TreeMap< Long, TimeSegment > availableSessionsAllBusyRemoved = AvailabilityReport.getCoachAvailableSessions(coachId, duration, coachAllBusyTimeRemoved);
		//System.out.print(coachName + "," + availableSessionsAllBusyRemoved.size());
		//reportData.append(coachName + "," + availableSessionsAllBusyRemoved.size());
		int totalBlocksAvailable = availableSessionsAvailabilityFull.size();
		int totalBlocksUnavailable = totalBlocksAvailable-availableSessionsUnavailabilityRemoved.size();
		int totalBlocksSessions = totalBlocksAvailable-availableSessionsScheduledRemoved.size();
		int netBlocksAvailable = availableSessionsAllBusyRemoved.size();
		reportData.append(coachName + "," + totalBlocksAvailable+","+ totalBlocksUnavailable+","+ totalBlocksSessions+","+ netBlocksAvailable);
		for(Map.Entry<Long,TimeSegment> entry : availableSessionsAllBusyRemoved.entrySet()) {
			Long key = entry.getKey();
			//System.out.print(key + ",");
		}
		reportData.append("\n");
		return reportData.toString();
	}
	
	/*public static void main(String args[]){
		try {
			Connection connection = AvailabilityLoader.getDatabaseConnection();
			ArrayList<CoachInfo> coachIdList = AvailabilityLoader.getActiveCoachIds(connection);
			System.out.println( "Coach Name," + "Available Sessions Full,Available Sessions removed Unavailability,Available Sessions removed Scheduled,Total Sessions Available, Sessions" );
			for(CoachInfo coach : coachIdList){
				String coachId=coach.getId();
				String coachName = coach.getName();
				//String coachId="1FB904850AB36541E053BC010B0AE770";
				//String coachName = "Myles Evans";
				//String queriesArray[] = AvailabilityLoader.getQueriesForWeek1(coachId);
				//String queriesArray[] = AvailabilityLoader.getQueriesForWeek2(coachId);
				//String queriesArray[] = AvailabilityLoader.getQueriesForWeek3(coachId);
				//String queriesArray[] = AvailabilityLoader.getQueriesForWeek4(coachId);
				//String queriesArray[] = AvailabilityLoader.getQueriesForWeek5(coachId);
				String availabilityQuery = queriesArray[0];
				String unavailabilityQuery = queriesArray[1];
				String sessionQuery = queriesArray[2];
				ArrayList<TimeBlock> availableTimeblocks = AvailabilityLoader.getTimeblocks(connection, availabilityQuery, TimeBlock.BlockType.AVAILABLE);
				ArrayList<TimeBlock> unavailableTimeblocks = AvailabilityLoader.getTimeblocks(connection, unavailabilityQuery, TimeBlock.BlockType.UNAVAILABLE);
				ArrayList<TimeBlock> scheduledTimeblocks = AvailabilityLoader.getTimeblocks(connection, sessionQuery, TimeBlock.BlockType.UNAVAILABLE);
				ArrayList<TimeBlock> allBusyTimeblocks = new ArrayList<TimeBlock>( unavailableTimeblocks );
				allBusyTimeblocks.addAll(scheduledTimeblocks);
				TreeSet<Long> coachAvailability = AvailabilityReport.getCoachAvailability( coachId, availableTimeblocks );
				TreeSet<Long> coachUnavailbilityRemoved = AvailabilityReport.removeCoachUnavailability( coachId, unavailableTimeblocks, coachAvailability );
				TreeSet<Long> coachScheduledRemoved = AvailabilityReport.removeCoachUnavailability( coachId, scheduledTimeblocks, coachAvailability );
				TreeSet<Long> coachAllBusyTimeRemoved = AvailabilityReport.removeCoachUnavailability( coachId, allBusyTimeblocks, coachAvailability );
				Iterator<Long> iterator = coachAvailability.iterator();
				while (iterator.hasNext()) {
					iterator.next();
				}
				TreeMap< Long, TimeSegment > availableSessionsAvailabilityFull = AvailabilityReport.getCoachAvailableSessions(coachId, new Long(30), coachAvailability);
				TreeMap< Long, TimeSegment > availableSessionsUnavailabilityRemoved = AvailabilityReport.getCoachAvailableSessions(coachId, new Long(30), coachUnavailbilityRemoved);
				TreeMap< Long, TimeSegment > availableSessionsScheduledRemoved = AvailabilityReport.getCoachAvailableSessions(coachId, new Long(30), coachScheduledRemoved);
				TreeMap< Long, TimeSegment > availableSessionsAllBusyRemoved = AvailabilityReport.getCoachAvailableSessions(coachId, new Long(30), coachAllBusyTimeRemoved);
				//System.out.print(coachName + "," + availableSessionsAllBusyRemoved.size());
				for(Map.Entry<Long,TimeSegment> entry : availableSessionsAllBusyRemoved.entrySet()) {
					Long key = entry.getKey();
					//System.out.print(key + ":");
				}
				System.out.println();
			}
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
	
	/**
	 * 
	 * @param connection
	 * @param query
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<TimeBlock> getTimeblocks(Connection connection, String query, TimeBlock.BlockType type) throws SQLException{
		Statement stmt = null;
	    ArrayList<TimeBlock> timeblockList = new ArrayList<TimeBlock>();
	    ResultSet rs= null;
	    try {
	        stmt = connection.createStatement();
	        rs = stmt.executeQuery(query);
	        while (rs.next()) {
	        	TimeBlock timeblock = new TimeBlock();
	        	//System.out.println( rs.getTimestamp(1).getTime() + " -- " + rs.getTimestamp(2).getTime());
	            Long startTime = rs.getTimestamp(1).getTime();
	        	Long endTime = rs.getTimestamp(2).getTime();
	        	timeblock.setStartTime(startTime);
	        	timeblock.setEndTime(endTime);
	        	timeblock.setBlockType(type);
	        	timeblockList.add(timeblock);
	        }
	    } catch (SQLException e ) {
	    	e.printStackTrace();
	    } 
	    finally {
	    	if(null != rs){
	    		rs.close();
	    	}
	        if (stmt != null) 
	        { 
	        	stmt.close(); 
	        }
	    }
	    return timeblockList;
	}
	
	/**
	 * 
	 * @param connection
	 * @param sessionTypeIds
	 * @param ignoreCoachList
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<CoachInfo> getActiveCoachIds(Connection connection,String sessionTypeIds,String ignoreCoachList) throws SQLException{
		String sessionType = "";
		String ignoreCoach = "";
		for(String value : sessionTypeIds.split(",")){
			sessionType = sessionType+"'"+value+"',";
		}
		sessionType = sessionType.replaceAll(",$", "");
		
		for(String value : ignoreCoachList.split(",")){
			ignoreCoach = ignoreCoach+"'"+value+"',";
		}
		ignoreCoach = ignoreCoach.replaceAll(",$", "");
		Statement stmt = null;
		ResultSet rs = null;
	    String query = "SELECT ID, NAME, EMAIL FROM PROVIDER WHERE IS_ACTIVE=1 AND ID IN (SELECT PROVIDER_ID FROM MP_PROVIDER_APPROVED WHERE SESSION_TYPE_ID IN ("+sessionType+") AND WILL_DELIVER=1 AND IS_APPROVED=1 AND PROVIDER_ID NOT IN ("+ignoreCoach+")) ORDER BY NAME";
	    ArrayList<CoachInfo> coachIdList = new ArrayList<CoachInfo>();
	    try {
	        stmt = connection.createStatement();
	        rs = stmt.executeQuery(query);
	        while (rs.next()) {
	            String coachId = rs.getString("ID");
	            String coachName = rs.getString("NAME");
	            CoachInfo info = new CoachInfo();
	            info.setId(coachId);
	            info.setName(coachName);
	           	coachIdList.add(info);
	        }
	    } catch (SQLException e ) {
	    	e.printStackTrace();
	    } 
	    finally {
	    	if(null != rs){
	    		rs.close();
	    	}
	        if (stmt != null) 
	        { 
	        	stmt.close(); 
	        }
	    }
	    return coachIdList;
	}
	
	/**
	 * 
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<CoachInfo> getActiveCoachIds(Connection connection) throws SQLException{
		Statement stmt = null;
		ResultSet rs = null;
	    String query = "SELECT ID, NAME, EMAIL FROM PROVIDER WHERE IS_ACTIVE=1 AND ID IN (SELECT PROVIDER_ID FROM MP_PROVIDER_APPROVED WHERE SESSION_TYPE_ID ='13' AND WILL_DELIVER=1 AND IS_APPROVED=1) ORDER BY NAME";
	    ArrayList<CoachInfo> coachIdList = new ArrayList<CoachInfo>();
	    try {
	        stmt = connection.createStatement();
	        rs = stmt.executeQuery(query);
	        while (rs.next()) {
	            String coachId = rs.getString("ID");
	            String coachName = rs.getString("NAME");
	            CoachInfo info = new CoachInfo();
	            info.setId(coachId);
	            info.setName(coachName);
	            if(!coachId.equalsIgnoreCase("07"))
	           	coachIdList.add(info);
	        }
	    } catch (SQLException e ) {
	    	e.printStackTrace();
	    } 
	    finally {
	    	if(null != rs){
	    		rs.close();
	    	}
	        if (stmt != null) 
	        { 
	        	stmt.close(); 
	        }
	    }
	    return coachIdList;
	}
	
}