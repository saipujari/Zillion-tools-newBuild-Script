package com.zillion.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.joda.time.DateTime;

import com.zillion.api.CsvExportUtil;
import com.zillion.api.MasterAvailabilityReportConstants;
import com.zillion.api.MasterAvailabilityReportHelper;
import com.zillion.api.ReportAPIHelper;
import com.zillion.api.ZillionUtil;
import com.zillion.masteravailability.model.Classroom;
import com.zillion.masteravailability.model.Event;
import com.zillion.masteravailability.model.Events;
import com.zillion.masteravailability.model.Provider;

public class ReportDbHelper {

	private static Properties masterAvilabilityResource =  MasterAvailabilityReportHelper.masterAvilabilityResource;
	private static final String         dbDriver         = masterAvilabilityResource.getProperty(MasterAvailabilityReportConstants.DBDRIVER);
	private static final String         dbUrl         = masterAvilabilityResource.getProperty(MasterAvailabilityReportConstants.DBURL);
	private static final String         dbUser         = masterAvilabilityResource.getProperty(MasterAvailabilityReportConstants.DBUSER);
	private static final String         dbPassword         = masterAvilabilityResource.getProperty(MasterAvailabilityReportConstants.DBPASSWORD);
	private static final String         DESTINA_STRING         = masterAvilabilityResource.getProperty("path");
	
	public static Map<String,String> stMasterProgramMap = new HashMap<String,String>();
	public static Map<String,String> masterProgramMap =  new HashMap<String,String>();
	public static Map<String,Provider> providerMap =  new HashMap<String,Provider>();
	public static Map<String,String> classroomMap = new HashMap<String,String>();
	public static Map<String,String> classificationMaskMap = new HashMap<String,String>();
	public static Map<String,List<Classroom>> classroomListByProviderMap = new HashMap<String,List<Classroom>>();
	public static Map<String,List<Classroom>> raClassroomListByProviderMap = new HashMap<String,List<Classroom>>();
	public static Map<String,List<Classroom>> hbmiClassroomListByProviderMap = new HashMap<String,List<Classroom>>();
	public static Map<String,Classroom> classroomStartDateOfLastMap = new HashMap<String,Classroom>();
	public static Connection connection = null;
	

/**
 * 
 * @return
 * @throws ClassNotFoundException
 * @throws SQLException
 * @throws Exception
 */
	public static Connection getDatabaseConnection() throws ClassNotFoundException, SQLException, Exception{
		Class.forName(dbDriver);
		if(null == connection || connection.isClosed()){
		 connection = DriverManager.getConnection(dbUrl, dbUser,dbPassword);
		}
		return connection;
	}
/**
 * 
 * @param sessionTypesIds
 * @return
 */
	private static String[] classroomReportQuerys(String sessionTypesIds){
	
		String sessionType = "";
		for(String value : sessionTypesIds.split(",")){
			sessionType = sessionType+"'"+value+"',";
		}
		sessionType = sessionType.replaceAll(",$", "");
		String sessionTypes = "SELECT ID, NAME,MAST_PROGRAM_ID FROM MP_SESSION_TYPE WHERE ID IN  ("+sessionType+")";
		String masterProgram="SELECT MP.ID , MP.NAME FROM MP_SESSION_TYPE ST, MP_A_MASTER_PROGRAM MP WHERE MP.ID =ST.MAST_PROGRAM_ID AND ST.ID IN ("+sessionType+")";
		String providers =" SELECT P.ID, P.NAME, P.SCH_COMMITTED_HRS_WEEKLY, P.EMAIL, P.PHONE,A.IS_APPROVED, A.WILL_DELIVER FROM PROVIDER P, MP_PROVIDER_APPROVED A "
				+" WHERE P.ID = A.PROVIDER_ID AND A.SESSION_TYPE_ID IN ("+sessionType+") AND A.IS_APPROVED = 1 AND A.WILL_DELIVER = 1";
		String classroomByProvider="SELECT ID FROM CLASSROOM WHERE IS_APPROVED=1 AND PROVIDER_ID=?";
		String classroomReport ="SELECT ID, NAME, STATUS, MAST_PROGRAM_ID, to_char(START_DT,'"+MasterAvailabilityReportConstants.DATE_FORMAT_3+"') FORMATEDSTARTDATE,CLASSIFICATION_MASK, START_DT, PROVIDER_ID, ASSIGNED_SEATS, AVAILABLE_SEATS, SCH_CLASSROOM_HRS_WEEKLY, SCH_INDIVIDUAL_HRS_WEEKLY, SCH_COMMITTED_HRS_WEEKLY "
                                 +" FROM CLASSROOM WHERE MAST_PROGRAM_ID IN (SELECT MAST_PROGRAM_ID FROM MP_SESSION_TYPE WHERE ID IN ("+sessionType+")) AND END_DT >= SYSDATE AND ASSIGNED_SEATS > 0 ORDER BY START_DT ASC ";
		String classificationMask="SELECT ID, MAST_PROGRAM_ID, CLASSIFICATION_MASK, CLASSIFICATION_CODE FROM MP_CLASSIFICATION";
		String getSessionTypes[] = { sessionTypes, masterProgram,providers,classroomByProvider,classroomReport,classificationMask};
		return getSessionTypes;
	}
	
	private static String[] masterAvailabilityQuerys(){
		
		String totalClasses = "SELECT ID,NAME,START_DT,END_DT,STATUS,MAST_PROGRAM_ID FROM CLASSROOM WHERE IS_APPROVED = 1 AND PROVIDER_ID = ? ";
		String startDateOfLast = "select ID, START_DT from (SELECT ID, START_DT,ROWNUM FROM CLASSROOM WHERE IS_APPROVED = 1 AND PROVIDER_ID = ? ORDER BY START_DT DESC) WHERE ROWNUM= 1";
		String masterAvilabilityReprotQuerys[]={totalClasses,startDateOfLast};
		return masterAvilabilityReprotQuerys;
	}
/**
 * 	
 * @param sessionTypesIds
 * @throws Exception
 */
	public static void getReportDatas(String sessionTypesIds) throws Exception{
		
		System.out.println("Starting to constract class room querys based on session type ids :  "+sessionTypesIds);
		String queriesArray[] = classroomReportQuerys(sessionTypesIds);
		String sessionTypeQuery = queriesArray[0];
		String masterProgramQuery = queriesArray[1];
		String providerQuery = queriesArray[2];
		String classroomByProviderQuery = queriesArray[3];
		String  classificationMaskQuery = queriesArray[5];
		System.out.println("Getting database connection ");
		connection = getDatabaseConnection();
		System.out.println("Getting Session type name from session type ids ");
		getSessionTypes(sessionTypeQuery,connection,stMasterProgramMap);
		System.out.println("Getting master program name from session type ids ");
		getMasterProgram(masterProgramQuery,connection,masterProgramMap);
		System.out.println("Loading providers list");
		getProviders(providerQuery,connection,providerMap);
		System.out.println("Loading classrooms by providers");
		getClassroomByProvider(classroomByProviderQuery,connection,classroomMap);
		System.out.println("Getting classificationMaskQuery by session type id, start date and end date ");
		classificationMask(classificationMaskQuery,connection,classificationMaskMap);
		
	}
	
	public static void getMasterAvilabilityReportData() throws ClassNotFoundException, Exception{
		String queriesArray[] =  masterAvailabilityQuerys();
		String totalClassroomQuery = queriesArray[0];
		String startDateOfLastQuery = queriesArray[1];
		connection = getDatabaseConnection();
		getClassroomListByProvider(totalClassroomQuery,connection,classroomListByProviderMap);
		getClassroomStartDateOfLast(startDateOfLastQuery,connection,classroomStartDateOfLastMap);
	}
	
public static Map<String,Provider> getProvidersMap(){
	return providerMap;
}
/**
 * 
 * @param reportDates
 * @param timeZone
 * @param sessionTypesIds
 * @return
 * @throws ParseException
 * @throws SQLException
 */
public static Map<String,StringBuilder> classroomReportGeneration(List<String> reportDates,String timeZone,String sessionTypesIds,String includeClassroomId) throws ParseException, SQLException{

System.out.println("Start classroom  report generation : include id "+includeClassroomId);
	PreparedStatement pStmt = null;
	ResultSet rs = null;
	StringBuilder classroomReportByilder =  null;
	Map<String,StringBuilder> classroomReportData = new HashMap<String,StringBuilder>();
	String queriesArray[] = classroomReportQuerys(sessionTypesIds);
	String classroomReportQuery = queriesArray[4];

		DateTime stDateTime = new DateTime();
		System.out.println("Getting classroom data on :"+stDateTime.toString( "yyyy-MM-dd'T'HH:mm:ss.SSS'ZZ'" ));
		classroomReportByilder = new StringBuilder(); 
		classroomReportByilder.append("Status,Program,Classification,Class Name,");
		if(includeClassroomId.equalsIgnoreCase("YES")){
			classroomReportByilder.append("Classroom Id,");
		}
		classroomReportByilder.append("Start Date,Day of Week,Start Time,Week #,Coach Name,# Mbr,#Open,Class Hr,Mbr Hrs,Total Hrs");
		classroomReportByilder.append("\n");
		try{
			pStmt = connection.prepareStatement(classroomReportQuery);
			rs = pStmt.executeQuery();
			while (rs.next()) {
				
				classroomReportByilder.append(rs.getString("STATUS"));
				classroomReportByilder.append(",");
				classroomReportByilder.append(masterProgramMap.get(rs.getString("MAST_PROGRAM_ID")) == null ? "" : masterProgramMap.get(rs.getString("MAST_PROGRAM_ID")));
				classroomReportByilder.append(",");
				classroomReportByilder.append(classificationMaskMap.get(rs.getString("MAST_PROGRAM_ID")+"_"+rs.getString("CLASSIFICATION_MASK")) == null ? "" : classificationMaskMap.get(rs.getString("MAST_PROGRAM_ID")+"_"+rs.getString("CLASSIFICATION_MASK")));
				classroomReportByilder.append(",");
				classroomReportByilder.append(rs.getString("NAME"));
				classroomReportByilder.append(",");				
				if(includeClassroomId.equalsIgnoreCase("YES")){
					classroomReportByilder.append(rs.getString("ID"));
					classroomReportByilder.append(",");	
				}
				classroomReportByilder.append(ZillionUtil.parseDate(rs.getString("FORMATEDSTARTDATE") ,timeZone,MasterAvailabilityReportConstants.DATE_FORMAT_2,MasterAvailabilityReportConstants.DATE_FORMAT_4));
				classroomReportByilder.append(",");
				classroomReportByilder.append(ZillionUtil.parseDate(rs.getString("FORMATEDSTARTDATE") ,timeZone,MasterAvailabilityReportConstants.DATE_FORMAT_2,MasterAvailabilityReportConstants.DATE_FORMAT_5).toUpperCase());
				classroomReportByilder.append(",");
				classroomReportByilder.append(ZillionUtil.parseDate(rs.getString("FORMATEDSTARTDATE") ,timeZone,MasterAvailabilityReportConstants.DATE_FORMAT_2,MasterAvailabilityReportConstants.DATE_FORMAT_6));
				classroomReportByilder.append(",");
				classroomReportByilder.append(ZillionUtil.getWeekNumber(rs.getString("FORMATEDSTARTDATE") ,timeZone,MasterAvailabilityReportConstants.DATE_FORMAT_2));
				classroomReportByilder.append(",");				
				classroomReportByilder.append(providerMap.get(rs.getString("PROVIDER_ID")) == null ? "" : providerMap.get(rs.getString("PROVIDER_ID")).getName() == null ? "" : providerMap.get(rs.getString("PROVIDER_ID")).getName());
				classroomReportByilder.append(",");
				classroomReportByilder.append(rs.getString("ASSIGNED_SEATS") == null ? "" : rs.getString("ASSIGNED_SEATS"));
				classroomReportByilder.append(",");
				classroomReportByilder.append(rs.getString("AVAILABLE_SEATS")  == null ? "" :rs.getString("AVAILABLE_SEATS"));
				classroomReportByilder.append(",");
				classroomReportByilder.append(rs.getString("SCH_CLASSROOM_HRS_WEEKLY")  == null ? "" : rs.getString("SCH_CLASSROOM_HRS_WEEKLY"));
				classroomReportByilder.append(",");
				classroomReportByilder.append(rs.getString("SCH_INDIVIDUAL_HRS_WEEKLY")  == null ? "" : rs.getString("SCH_INDIVIDUAL_HRS_WEEKLY"));
				classroomReportByilder.append(",");
				classroomReportByilder.append(rs.getString("SCH_COMMITTED_HRS_WEEKLY"));
				classroomReportByilder.append("\n");
				
			}
		
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(null != rs){
				rs.close();
			}
			if (pStmt != null) 
	        { 
	        	pStmt.close(); 
	        }
			
		}
		classroomReportData.put(ZillionUtil.getDateAsString(stDateTime.toDate(), MasterAvailabilityReportConstants.DATE_FORMAT_1), classroomReportByilder);
		DateTime edDateTime = new DateTime();
		System.out.println("Time taken to constract classroom report data on " + stDateTime.toString( "yyyy-MM-dd'T'HH:mm:ss.SSS'ZZ'" ) + ": "+(edDateTime.getMillis()-stDateTime.getMillis()) +"ms");

	return classroomReportData;
}
	
/**
 * 	
 * @param query
 * @param connection
 * @param sessionMap
 * @throws SQLException
 */
	public static void getSessionTypes(String query,Connection connection,Map<String,String> sessionMap) throws SQLException{
		
		DateTime st = new DateTime();
		  Statement stmt = null;
		  ResultSet rs = null;
		try{  
			
			 stmt = connection.createStatement();
		     rs = stmt.executeQuery(query);
		     while(rs.next()){
		    	 sessionMap.put(rs.getString(1), rs.getString(3));
		     }
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(null != rs){
				rs.close();
			}
			if (stmt != null) 
	        { 
	        	stmt.close(); 
	        }
		}
		
		DateTime et = new DateTime();
		MasterAvailabilityReportHelper.timeTaken = MasterAvailabilityReportHelper.timeTaken +(et.getMillis()-st.getMillis());
		 System.out.println("Time taken to get sesion types :"+(et.getMillis()-st.getMillis()) +"ms");
	}
	
/**
 * 	
 * @param query
 * @param connection
 * @param sessionMap
 * @throws SQLException
 */
 public static void getMasterProgram(String query,Connection connection,Map<String,String> sessionMap) throws SQLException{
			
			DateTime st = new DateTime();
			  Statement stmt = null;
			  ResultSet rs = null;
			try{  
				
				 stmt = connection.createStatement();
			     rs = stmt.executeQuery(query);
			     while(rs.next()){
			    	 sessionMap.put(rs.getString(1), rs.getString(2));
			     }
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				if(null != rs){
					rs.close();
				}
				if (stmt != null) 
		        { 
		        	stmt.close(); 
		        }
			}
			
			DateTime et = new DateTime();
			MasterAvailabilityReportHelper.timeTaken = MasterAvailabilityReportHelper.timeTaken +(et.getMillis()-st.getMillis());
			 System.out.println("Time taken to get master program types :"+(et.getMillis()-st.getMillis()) +"ms");
}	
	
/**
 * 	
 * @param query
 * @param connection
 * @param classificationMaskMap
 * @throws SQLException
 */
public static void classificationMask(String query,Connection connection,Map<String,String> classificationMaskMap) throws SQLException{
	DateTime st = new DateTime();
			  Statement stmt = null;
			  ResultSet rs = null;
			try{  
				
				 stmt = connection.createStatement();
			     rs = stmt.executeQuery(query);
			     while(rs.next()){
			    	 classificationMaskMap.put(rs.getString("MAST_PROGRAM_ID")+"_"+rs.getString("CLASSIFICATION_MASK"), rs.getString("CLASSIFICATION_CODE"));
			     }
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				if(null != rs){
					rs.close();
				}
				if (stmt != null) 
		        { 
		        	stmt.close(); 
		        }
			}
			DateTime et = new DateTime();
			MasterAvailabilityReportHelper.timeTaken = MasterAvailabilityReportHelper.timeTaken +(et.getMillis()-st.getMillis());
			 System.out.println("Time taken to get classificationMask :"+(et.getMillis()-st.getMillis()) +"ms");
		}	
/**
 * 
 * @param query
 * @param connection
 * @param providerMap
 * @throws SQLException
 */
 public static void getProviders(String query,Connection connection,Map<String,Provider> providerMap) throws SQLException{

	  Statement stmt = null;
	  List<Provider> providerList = new ArrayList<Provider>();
	  Provider provider = null;
	  ResultSet rs = null;
			try{  
				
				 stmt = connection.createStatement();
			     rs = stmt.executeQuery(query);
			     while(rs.next()){
			    	 
			    	 System.out.println("Getting provider details "+ rs.getString("ID"));
			    	 DateTime st = new DateTime();
			    	 provider = new Provider();
			    	 provider.setId(rs.getString("ID"));
			    	 provider.setName(rs.getString("NAME"));
			    	 provider.setSchCommittedHrsWeekly(rs.getString("SCH_COMMITTED_HRS_WEEKLY"));
			    	 provider.setEmail(rs.getString("EMAIL"));
			    	 provider.setPhone(rs.getString("PHONE"));
			    	 provider.setIsApproved(rs.getBoolean("IS_APPROVED"));
			    	 provider.setIsApproved(rs.getBoolean("WILL_DELIVER"));
			    	 providerList.add(provider);
			    	 DateTime et = new DateTime();
			    	 MasterAvailabilityReportHelper.timeTaken = MasterAvailabilityReportHelper.timeTaken +(et.getMillis()-st.getMillis());
			    	 System.out.println("Time taken to get provider" + rs.getString("ID") +(et.getMillis()-st.getMillis()) +"ms");
			     }
			     
			   for(Provider pro:providerList)  {
				     providerMap.put(pro.getId(),pro);
				
			   }
			     
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				if(null != rs){
					rs.close();
				}
				if (stmt != null) 
		        { 
		        	stmt.close(); 
		        }
			}
 }
 /**
  * 
  * @param query
  * @param connection
  * @param classroomMap
  * @throws SQLException
  */
public static void getClassroomByProvider(String query,Connection connection,Map<String,String> classroomMap) throws SQLException{
	  PreparedStatement preparedStatement = null;
	  ResultSet rs = null;	
		
		for(String providerId : providerMap.keySet()){
			 DateTime st = new DateTime();
		try{  
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, providerId);
			rs = preparedStatement.executeQuery();
			while (rs.next()) {
				classroomMap.put(rs.getString(1), providerId);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(null != rs){
				rs.close();
			}
			if (preparedStatement != null) 
	        { 
			    preparedStatement.close(); 
	        }
			
		}
		
	 DateTime et = new DateTime();
   	 MasterAvailabilityReportHelper.timeTaken = MasterAvailabilityReportHelper.timeTaken +(et.getMillis()-st.getMillis());
   	 System.out.println("Time taken to get classroom for " + providerId+": " +(et.getMillis()-st.getMillis()) +"ms");
	
	}
}
/**
 * 
 * @param query
 * @param connection
 * @param classroomMap
 * @throws SQLException
 */
public static void getClassroomListByProvider(String query,Connection connection,Map<String,List<Classroom>> classroomListByProviderMap) throws SQLException{
	 
	  Classroom classroom = null;
	  List<Classroom> classroomList = null;
	  List<Classroom> RAClassroomList = null;
	  List<Classroom> HBMIClassroomList = null;
		for(String providerId : providerMap.keySet()){
			 PreparedStatement preparedStatement = null;
			  ResultSet rs = null;	
			 DateTime st = new DateTime();
		try{
			classroomList = new ArrayList<Classroom>();
			RAClassroomList = new ArrayList<Classroom>();
			HBMIClassroomList = new ArrayList<Classroom>();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, providerId);
			rs = preparedStatement.executeQuery();
			while (rs.next()) {
				classroom = new Classroom();
				classroom.setId(rs.getString("ID"));
				classroom.setName(rs.getString("NAME"));
				classroom.setStartDt(rs.getString("START_DT"));
				classroom.setEndDt(rs.getString("END_DT"));
				classroom.setStatus(rs.getString("STATUS"));
				classroom.setMastHealthProgramId(rs.getString("MAST_PROGRAM_ID"));
				classroomList.add(classroom);
				
				if(null != rs.getString("MAST_PROGRAM_ID") && rs.getString("MAST_PROGRAM_ID").equalsIgnoreCase("02")){
					RAClassroomList.add(classroom);
				}else if(null != rs.getString("MAST_PROGRAM_ID") && rs.getString("MAST_PROGRAM_ID").equalsIgnoreCase("03")){
					HBMIClassroomList.add(classroom);
				}
			}
			if(classroomList.size() > 0){
			classroomListByProviderMap.put(providerId, classroomList);
			raClassroomListByProviderMap.put(providerId, RAClassroomList);
			hbmiClassroomListByProviderMap.put(providerId, HBMIClassroomList);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(null != rs){
				rs.close();
			}
			
			if (preparedStatement != null) 
	        { 
				preparedStatement.close(); 
	        }
			
		}
		
		 DateTime et = new DateTime();
	   	 MasterAvailabilityReportHelper.timeTaken = MasterAvailabilityReportHelper.timeTaken +(et.getMillis()-st.getMillis());
	   	 System.out.println("Time taken to get classroomList by " + providerId+": " +(et.getMillis()-st.getMillis()) +"ms");
	
	}
}
	
/**
 * 
 * @param query
 * @param connection
 * @param classroomMap
 * @throws SQLException
 */
public static void getClassroomStartDateOfLast(String query,Connection connection,Map<String,Classroom> classroomStartDateOfLastMap) throws SQLException{
	  PreparedStatement preparedStatement = null;
	  ResultSet rs = null;	
	  Classroom classroom = null;
		for(String providerId : providerMap.keySet()){
		try{
		    preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, providerId);
			rs = preparedStatement.executeQuery();
			while (rs.next()) {
				classroom = new Classroom();
				classroom.setId(rs.getString("ID"));
				classroom.setStartDt(rs.getString("START_DT"));
			}
			classroomStartDateOfLastMap.put(providerId, classroom);
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(null != rs){
				rs.close();
			}
			if (preparedStatement != null) 
	        { 
			    preparedStatement.close(); 
	        }
			
		}
		
	  
	
	}
}
/**
 * 
 * @param reportDates
 * @param timeZone
 * @throws ParseException
 * @throws IOException
 */
public static void masterAvailabilityReportGeneration(List<String> reportDates,String timeZone,String includeId) throws ParseException, IOException{
	
	Events events = null;
	DateTime timeslot = null;
	DateTime eventEndDateTime = null;
	DateTime earliestTimeslot = null;
	DateTime latestTimeslot = null;
	Map<String,DateTime> minMaxDateTimeMap = null;
	List<String> dateListFromEvents = null;
	String eventCode=null;
	Map<String,Events> eventsByProvider = null;
	Map<String,String> providerEventMap = null;
	Map<String,Map<String,String>> timeslotData = new HashMap<String,Map<String,String>>();
	List<Events> allEvents = new ArrayList<Events>();
	List<String> header = new ArrayList<String>();
	StringBuilder exportData = new StringBuilder();
	Map<String,String> timeSlotMap = new HashMap<String,String>();
	Map<String,StringBuilder> exportDataMap = new HashMap<String,StringBuilder>();
	String time_Slot = null;
	String newEventCode = null;
	for(String date : reportDates){
		  exportData	= new StringBuilder();
		  if(includeId.equalsIgnoreCase("YES")){
			  exportData.append("Provider Id").append(",");
		  }
		  exportData.append(" Name ,").append("Email,Total Classes (A+U),Total ILI Classes (A+U),Total HBMI Classes (A+U),Start Date of the last assigned classroom,");
		  eventsByProvider = ReportAPIHelper.getAllProvidersEventsByDate(providerMap,date,timeZone);
		  
		  for(String provider : eventsByProvider.keySet()){
			
			  allEvents.add(eventsByProvider.get(provider));
		  }
		  
		  dateListFromEvents = ZillionUtil.dateListFromEvents(allEvents);
		  minMaxDateTimeMap = ZillionUtil.getMinMaxDateTime(dateListFromEvents,MasterAvailabilityReportConstants.DATR_FORMAT_7,date,timeZone);
		  earliestTimeslot = minMaxDateTimeMap.get(MasterAvailabilityReportConstants.MINDATE);
		  latestTimeslot = minMaxDateTimeMap.get(MasterAvailabilityReportConstants.MAXDATE);
		  
		  header = ZillionUtil.getMasterAvailabilityReportTimeSlotHeader(earliestTimeslot, latestTimeslot, timeZone);	
		  exportData.append(ZillionUtil.getStringFromList(header)).append("\n");
		  
		for(String providerId  : providerMap.keySet()){
			DateTime stDateTime = new DateTime();
			 providerEventMap = new HashMap<String,String>();
			events =eventsByProvider.get(providerId);
		if(null != events)	{
			for (Event event : events.getEvent()) {
				
				if(!event.getSessionStatus().equalsIgnoreCase("Canceled") 
				   && !event.getSessionStatus().equalsIgnoreCase("Unconfirmed") 
				   &&!event.getSessionStatus().equalsIgnoreCase("Unscheduled") && !event.getIsRecurrence()){
					
				timeslot = ZillionUtil.getDateFromString(event.getStartDtTime(),MasterAvailabilityReportConstants.DATR_FORMAT_7);
				eventEndDateTime =  ZillionUtil.getDateFromString(event.getEndDtTime(),MasterAvailabilityReportConstants.DATR_FORMAT_7);
				
				eventCode = getEventCode( event );
				
				
				while( ( timeslot.getMillis() >= earliestTimeslot.getMillis() || 
						eventEndDateTime.getMillis() >= earliestTimeslot.getMillis()  )
						&& timeslot.getMillis() <= latestTimeslot.getMillis() && 
						 timeslot.getMillis()< eventEndDateTime.getMillis()) {
					
					if(timeslot.getMillis() < earliestTimeslot.getMillis() && eventEndDateTime.getMillis() >= earliestTimeslot.getMillis()){
						
						time_Slot = ZillionUtil.parseDate(earliestTimeslot.toString() ,timeZone,MasterAvailabilityReportConstants.DATR_FORMAT_8,MasterAvailabilityReportConstants.DATE_FORMAT_6);
						newEventCode = getEventCodeByPriority(eventCode,providerEventMap,time_Slot);						
						providerEventMap.put(time_Slot, newEventCode );
					}else{
						
						time_Slot = ZillionUtil.parseDate(timeslot.toString() ,timeZone,MasterAvailabilityReportConstants.DATR_FORMAT_8,MasterAvailabilityReportConstants.DATE_FORMAT_6);
						newEventCode = getEventCodeByPriority(eventCode,providerEventMap,time_Slot);	
						providerEventMap.put(time_Slot, newEventCode );
					}
	    			
					timeslot = timeslot.plusMinutes( 5 );
					eventCode = eventCode.toLowerCase();
					}	
				}
		    }
		 }	
		   
		  if(null != providerEventMap && providerEventMap.size() > 0){
			timeslotData.put(providerId, providerEventMap);
		  }
		  
		  DateTime edDateTime = new DateTime();
		  System.out.println("Time taken to constract report data for" + providerId + "("+date+") : "+(edDateTime.getMillis()-stDateTime.getMillis()) +"ms");
		  MasterAvailabilityReportHelper.timeTaken = MasterAvailabilityReportHelper.timeTaken +(edDateTime.getMillis()-stDateTime.getMillis());
	   }
		
		
		for(String providerId : providerMap.keySet()){
			
			timeSlotMap = timeslotData.get(providerId);
			if(null != timeSlotMap){
				 if(includeId.equalsIgnoreCase("YES")){
					  exportData.append(providerId).append(",");
				  }
				exportData.append(providerMap.get(providerId) == null ? "" : providerMap.get(providerId).getName()).append(",");
				exportData.append(providerMap.get(providerId) == null ? "" : providerMap.get(providerId).getEmail()).append(",");
				exportData.append(classroomListByProviderMap.get(providerId) == null ? "" : classroomListByProviderMap.get(providerId).size()).append(",");
				exportData.append(raClassroomListByProviderMap.get(providerId) == null ? "" : raClassroomListByProviderMap.get(providerId).size()).append(",");
				exportData.append(hbmiClassroomListByProviderMap.get(providerId) == null ? "" : hbmiClassroomListByProviderMap.get(providerId).size()).append(",");
				String startDateOfLast = classroomStartDateOfLastMap.get(providerId) == null ? "" : classroomStartDateOfLastMap.get(providerId).getStartDt();
				exportData.append(ZillionUtil.parseDate(startDateOfLast,timeZone,MasterAvailabilityReportConstants.DATE_FORMAT_2,MasterAvailabilityReportConstants.DATE_FORMAT_4));
				for(String data : header){				
					
					exportData.append(",");
					exportData.append(timeSlotMap.get(data)==null ? "":timeSlotMap.get(data));
					
				}
				exportData.append("\n");
			}
			
		}
		exportDataMap.put(date, exportData);
	 }

	 System.out.println("CSV generation for master availability report started ...");
	CsvExportUtil.exportCsv(exportDataMap, DESTINA_STRING, MasterAvailabilityReportConstants.MASTERAVAILABILITY_REPORTNAME);
	System.out.println("Master availability CSV file exported successfully ...");
	
	System.out.println("Total Time taken to export Master Availability Report : "+MasterAvailabilityReportHelper.timeTaken+" ms");
	System.out.println("Average time to export Master Availability Report for each provider: " + (MasterAvailabilityReportHelper.timeTaken/providerMap.size())+" ms");
}

/**
 * 
 * @param event
 * @return
 */
public static String getEventCode(Event event){
	
	
	 if( event.getEventType().equalsIgnoreCase("Available"))
		 return "Y";
	 if( event.getEventType().equalsIgnoreCase("Unavailable"))
		 return "N";
	 if( event.getIsGroupSession() == false ) {   
		// return max 4 chars for individual sessions
		String stDisplay = event.getSessionTypeId() == null ? "" : event.getSessionTypeId().getName();
		return (stDisplay.length() <= 4 ) ? stDisplay.toUpperCase() : stDisplay.substring( 0,3 ).toUpperCase();
	   }

	 if( null != classroomMap.get( event.getClassroomId().getId() ) && !classroomMap.get( event.getClassroomId().getId() ).equalsIgnoreCase(event.getOwnerId() ))
		 return "G";

	 String masterProgram = stMasterProgramMap.get( event.getSessionTypeId().getId() );
	 String mpChar ="I";
	 if(null != masterProgram && masterProgram.equalsIgnoreCase("03")){
	   mpChar = "H";
	 }
	 
	 String approveChar = (event.getIsApproved()) ? "A" : "U";
	  return ("" + mpChar + approveChar).toUpperCase();
	 
	 
  }
/**
 *  
 * @param newEventCode
 * @param providerEventMap
 * @param timeSlot
 * @return
 */
public static String getEventCodeByPriority(String newEventCode,Map<String,String> providerEventMap,String timeSlot){
	
	
	if(null != providerEventMap.get(timeSlot)){		
		if(providerEventMap.get(timeSlot).equalsIgnoreCase("Y")){
			return newEventCode;
		}else{
			if(newEventCode.equalsIgnoreCase("Y") || newEventCode.equalsIgnoreCase("N")){				
				return providerEventMap.get(timeSlot);
			
			}else{
				return newEventCode;
			}
		}
	}else{
		return newEventCode;
	}
	
  }
}
