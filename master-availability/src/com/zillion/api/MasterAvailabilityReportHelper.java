package com.zillion.api;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.zillion.db.ReportDbHelper;

public class MasterAvailabilityReportHelper {
	
	//public static final String PROP_FILE_NAME = "masteravilabilityresource"; 
	
	public static Properties masterAvilabilityResource = null;
 	public static Long  timeTaken = 0L ;
 	public static String USERNAME = null;
 	public static String PASSWORD = null;
 	public static String SESSIONTYPEIDS = null;
 	public static String STARTDATE = null;
 	public static String ENDDATE = null;
 	public static String TIMEZONE= null;
 	public static String INCLUDEID = null;
/**
 *  	
 * @param inputParams
 * 
 * @throws Exception 
 */
 public static void generateReportData(Map<String,String> inputParams) throws Exception{
	 
	    USERNAME = inputParams.get(MasterAvailabilityReportConstants.USERNAME);
	    PASSWORD = inputParams.get(MasterAvailabilityReportConstants.PASSWORD);
	    SESSIONTYPEIDS = inputParams.get(MasterAvailabilityReportConstants.SESSIONTYPEIDS);
	    STARTDATE = inputParams.get(MasterAvailabilityReportConstants.STARTDATE);
	    ENDDATE = inputParams.get(MasterAvailabilityReportConstants.ENDDATE);
	    TIMEZONE = inputParams.get(MasterAvailabilityReportConstants.TIMEZONE);
	    INCLUDEID=inputParams.get(MasterAvailabilityReportConstants.INCLUDEID);
	    System.out.println("Loading Properties File");
	   setProprties(inputParams);
	    ReportDbHelper.getReportDatas(SESSIONTYPEIDS);
	    
 }
 
 public static void setProprties(Map<String,String> inputParams){
	 
	 masterAvilabilityResource = ZillionUtil.getProperties(inputParams.get(MasterAvailabilityReportConstants.RESOURCEBUNDLEPATH));
 }
 
 /**
  * 
  * @throws Exception
  */
 public static void generateClassroomReport(Map<String,String> inputParams) throws Exception{	  
	 
	 generateReportData(inputParams);	 
	 List<String> dateList = null;
	 Map<String,StringBuilder> classroomReportData = null;
	 System.out.println("Constract datelist from strat date to end date");
	 dateList = ZillionUtil.getDaysBetweenDates(STARTDATE,ENDDATE);
	 System.out.println("Started to generate classroom report....");
	 classroomReportData = ReportDbHelper.classroomReportGeneration(dateList,TIMEZONE,SESSIONTYPEIDS,INCLUDEID);
	 System.out.println("CSV generation for classroom report started ...");
	 CsvExportUtil.exportCsv(classroomReportData,masterAvilabilityResource.getProperty("path"),MasterAvailabilityReportConstants.CLASSROOM_REPORTNAME);
	 System.out.println("Classroom report CSV file exported successfully ...");
	 if(null !=  ReportDbHelper.connection){
	  ReportDbHelper.connection.close();
	 }
 }
/**
 * 
 * @param inputParams
 * @throws Exception
 */
 public static void generateMasterAvilabilityReprot(Map<String,String> inputParams) throws Exception{
	 List<String> dateList = null;
	 generateReportData(inputParams);
	 ReportDbHelper.getMasterAvilabilityReportData();
	 System.out.println("Constract datelist from strat date to end date");
	 dateList = ZillionUtil.getDaysBetweenDates(STARTDATE,ENDDATE);
	 
	 ReportAPIHelper.doLogin(USERNAME, PASSWORD);
	 ReportDbHelper.masterAvailabilityReportGeneration(dateList,TIMEZONE,INCLUDEID);
	 if(null !=  ReportDbHelper.connection){
	  ReportDbHelper.connection.close();
	 }
  }

 
}
