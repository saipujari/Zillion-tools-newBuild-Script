package com.zillion.api;

import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import com.zillion.masteravailability.model.Event;
import com.zillion.masteravailability.model.Events;

public class ZillionUtil {
	
 public ZillionUtil(){}
 
/**
 * 	
 * @param input
 * @return
 */
	public static ZillionResponse parseResponse(StringBuffer input){
		JSONObject jsonobj = null;
		try {
			@SuppressWarnings("unused")
            String str = "";
			if(input.toString().startsWith("[")){
				str = input.substring(1, input.length() - 1);
			} else {
				try {
					jsonobj = new JSONObject(input.toString());
				} catch (JSONException ex) {
				}
			}
		} catch (Exception e) {
		  e.printStackTrace();
		}
		ZillionResponse response = null;
		try {
			response = new ZillionResponse(jsonobj);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return response;
	}
/**
 * 	
 * @param response
 * @return
 * @throws JSONException
 */
public static JSONObject parseData(ZillionResponse response) throws JSONException{
	JSONObject jsonobj = new JSONObject(response.getResponseData());
	return jsonobj;

}
/**
 * 	
 * @param response
 * @return
 */
public static JSONObject parseData(String response){
	JSONObject jsonobj = null;
	if(response.startsWith("[") && response.endsWith("]")){
		response = response.substring(1, response.length() - 1);
	}
	try {
		if(response.startsWith("[") && response.endsWith("]")){
			response.substring(1, response.length() - 1);
		}
		try { jsonobj = new JSONObject(response); } catch (JSONException e) {}
	} catch (Exception e) {
		e.printStackTrace();
	}
	return jsonobj;
}
/**
 * 
 * @param startdate
 * @param enddate
 * @return
 * @throws ParseException 
 */
public static List<String> getDaysBetweenDates(String startdate, String enddate) throws ParseException
 {
	 SimpleDateFormat formatter = new SimpleDateFormat(MasterAvailabilityReportConstants.DATE_FORMAT_1);
     List<String> dateList = new ArrayList<String>();
     Calendar calendar = new GregorianCalendar();
     calendar.setTime(formatter.parse(startdate));
     Date endDate = formatter.parse(enddate);
     while (calendar.getTime().before(endDate) ||calendar.getTime().equals(endDate) )
     {
         dateList.add(formatter.format(calendar.getTime()));
         calendar.add(Calendar.DATE, 1);
     }
     return dateList;
 }
/**
 * 
 * @param startdate
 * @param enddate
 * @param timeZone
 * @return
 * @throws ParseException 
 */
 public static Date getDateInUTC(String date ,String timeZone,String dateFormat) throws ParseException{
	  
	 DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(dateFormat).withZone(DateTimeZone.forID(timeZone));
	 DateTime dateTime = dateTimeFormatter.parseDateTime(date);
	 Date dateInUTC = getDateInTimeZone(dateTime.toDate(),DateTimeZone.UTC.toTimeZone());
	 return dateInUTC;
	
 }
 /**
  * 
  * @param currentDate
  * @param timezone
  * @return
  */
 public static Date getDateInTimeZone(Date currentDate, TimeZone timezone) {
     Calendar calendar = new GregorianCalendar(timezone);
     calendar.setTimeInMillis(currentDate.getTime());
     Calendar currentCalendar = Calendar.getInstance();
     currentCalendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
     currentCalendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
     currentCalendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
     currentCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
     currentCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
     currentCalendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND));
     currentCalendar.set(Calendar.MILLISECOND, calendar.get(Calendar.MILLISECOND));
     return currentCalendar.getTime();
 }
 /**
  * 
  * @param endDate
  * @return
 * @throws ParseException 
  */
 public static String setEndDateTime(String endDate) throws ParseException{
	 SimpleDateFormat formatter = new SimpleDateFormat(MasterAvailabilityReportConstants.DATE_FORMAT_1);
	 SimpleDateFormat returnDateFormat = new SimpleDateFormat(MasterAvailabilityReportConstants.DATE_FORMAT_2);
	 Calendar cal = Calendar.getInstance();  
     cal.setTime(formatter.parse(endDate));  
     cal.set(Calendar.HOUR_OF_DAY, 23);  
     cal.set(Calendar.MINUTE, 59);  
     cal.set(Calendar.SECOND, 59);  
     cal.set(Calendar.MILLISECOND, 59);      
     return returnDateFormat.format(cal.getTime()).toString(); 
	 
 }
 
 /**
  * 
  * @param startDate
  * @return
 * @throws ParseException 
  */
 public static String setStartDateTime(String startDate) throws ParseException{
	 SimpleDateFormat formatter = new SimpleDateFormat(MasterAvailabilityReportConstants.DATE_FORMAT_1);
	 SimpleDateFormat returnDateFormat = new SimpleDateFormat(MasterAvailabilityReportConstants.DATE_FORMAT_2);
	 Calendar cal = Calendar.getInstance();  
     cal.setTime(formatter.parse(startDate));  
     cal.set(Calendar.HOUR_OF_DAY,0);  
     cal.set(Calendar.MINUTE, 0);  
     cal.set(Calendar.SECOND, 0);  
     cal.set(Calendar.MILLISECOND, 0);      
     return returnDateFormat.format(cal.getTime()).toString(); 
	 
 }
 
 /**
  * 
  * @param startdate
  * @param enddate
  * @param timeZone
  * @return
  * @throws ParseException 
  */
  public static String parseDate(String date ,String timeZone,String inputFormat,String outputFormat) throws ParseException{
 	  
	  if(null != date && date.trim().length() > 0){
		try{	  
			  DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(inputFormat).withZone(DateTimeZone.UTC);
			  DateTime dateTime = dateTimeFormatter.parseDateTime(date);	  
			  Date dateInSpecifiedTimeZone = getDateInTimeZone(dateTime.toDate(),DateTimeZone.forID(timeZone).toTimeZone());	  
			  SimpleDateFormat formatter = new SimpleDateFormat(outputFormat);
		      return formatter.format(dateInSpecifiedTimeZone).toString();
		  }catch(Exception e){
			  e.printStackTrace();
		  }
	  }
	  return "";
  }
  
 
/**
 *   
 * @param classroomStartDateStr
 * @param timeZone
 * @param inputFormat
 * @return
 */
  public static String getWeekNumber(String classroomStartDateStr,String timeZone,String inputFormat){
	  if(null != classroomStartDateStr && classroomStartDateStr.trim().length() > 0){
		  
		  DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(inputFormat);
	      DateTime classroomStartDateInUTC =dateTimeFormatter.parseDateTime(classroomStartDateStr).withZoneRetainFields(DateTimeZone.UTC);
          DateTime classroomStartDateInSpecifiedTimeZone = new DateTime(classroomStartDateInUTC).withZone(DateTimeZone.forID(timeZone));
          DateTime currentDateInSpecifiedTimeZone =new DateTime().withZone(DateTimeZone.forID(timeZone));
          if(classroomStartDateInSpecifiedTimeZone.isAfter(currentDateInSpecifiedTimeZone) ){
        	  return "0";
          }else{
        	  int weekNumber = 0;
        	  while(classroomStartDateInSpecifiedTimeZone.isBefore(currentDateInSpecifiedTimeZone)){
        		  classroomStartDateInSpecifiedTimeZone = classroomStartDateInSpecifiedTimeZone.plusDays(7);
        		  weekNumber ++;
        	   	  }
        	  
         	       return String.valueOf(weekNumber);        	  
          }
	  }
	  return "";
  }
  /**
   * 
   * @param inputDate
   * @param pattern
   * @return
   */
  public static String getDateAsString(Date inputDate, String pattern) {
      String formattedDate = null;
      if (inputDate != null) {
          SimpleDateFormat sdf = new SimpleDateFormat(pattern);
          try {
              formattedDate = sdf.format(inputDate);
          } catch (Exception e) {
             e.printStackTrace();
          }
          return formattedDate;
      } 
      else
          return null;
  } 
 /**
  *  
  * @param dates
  * @param inputFormat
  * @return
 * @throws ParseException 
  */
 public static Map<String,DateTime> getMinMaxDateTime(List<String> dates,String inputFormat,String eventDate,String timeZone) throws ParseException{
	 
	 DateTime dateTime = null;
	 Map<String,DateTime> minMaxDateTime = new HashMap<String,DateTime>();
	 SortedSet<DateTime> dateTimeSet = new TreeSet<DateTime>();
	 DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(inputFormat);
	 DateTime minDateTime= null;
	 DateTime maxDateTime = null;
	 for(String date : dates){
		 dateTime = dateTimeFormatter.parseDateTime(date);
		 dateTimeSet.add(dateTime);
	 }
	 
	 DateTime startDateTime =  new DateTime(ZillionUtil.getDateInUTC(ZillionUtil.setStartDateTime(eventDate) ,timeZone,MasterAvailabilityReportConstants.DATE_FORMAT_2));
	 DateTime endDateTime = new DateTime(ZillionUtil.getDateInUTC(ZillionUtil.setEndDateTime(eventDate),timeZone,MasterAvailabilityReportConstants.DATE_FORMAT_2));
	 
	 DateTime earliestTimeslot = startDateTime;
	 DateTime latestTimeslot = endDateTime;
	 
	if(dateTimeSet.size()>0){
		minDateTime = dateTimeSet.first();
		maxDateTime = dateTimeSet.last();
	 
		earliestTimeslot = ( minDateTime.getMillis() > startDateTime.getMillis() ) ? minDateTime : earliestTimeslot;
		latestTimeslot = ( maxDateTime.getMillis() < endDateTime.getMillis() ) ? maxDateTime : latestTimeslot;
	}
		 
	 minMaxDateTime.put(MasterAvailabilityReportConstants.MINDATE, earliestTimeslot);
	 minMaxDateTime.put(MasterAvailabilityReportConstants.MAXDATE,latestTimeslot);
	 
	return minMaxDateTime;
	 
 }
/**
 *   
 * @param events
 * @return
 */
public static List<String> dateListFromEvents(List<Events> eventList){
	List<String> dateList = new ArrayList<String>();
	
for(Events events : eventList){
	for(Event event : events.getEvent()){
	
		if(!event.getSessionStatus().equalsIgnoreCase("Canceled") 
		   && !event.getSessionStatus().equalsIgnoreCase("Unconfirmed") 
		   &&!event.getSessionStatus().equalsIgnoreCase("Unscheduled") && !event.getIsRecurrence()){
			if(null != event.getStartDtTime()){
			 dateList.add(event.getStartDtTime());
			}
			if(null != event.getEndDtTime()){
				 dateList.add(event.getEndDtTime());
	       }
		}
	}
 }
	return dateList;
}
/**
 * 
 * @param date
 * @param inputFormat
 * @return
 */
public static DateTime getDateFromString(String date,String inputFormat){
	 DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(inputFormat).withZoneUTC();
	 return dateTimeFormatter.parseDateTime(date);
}
/**
 * 
 * @param earliestTimeslot
 * @param latestTimeslot
 * @param timeZone
 * @return
 * @throws ParseException
 */
public static List<String> getMasterAvailabilityReportTimeSlotHeader(DateTime earliestTimeslot, DateTime latestTimeslot,String timeZone) throws ParseException{
	DateTime timeslot = earliestTimeslot;
	List<String> header = new ArrayList<String>();
	while( timeslot.getMillis() <= latestTimeslot.getMillis()) {
		
		header.add(ZillionUtil.parseDate(timeslot.toString() ,timeZone,MasterAvailabilityReportConstants.DATR_FORMAT_8,MasterAvailabilityReportConstants.DATE_FORMAT_6));
		timeslot = timeslot.plusMinutes( 5 );
		
		
		}	
	return header;
	
 }

/**
 * 
 * @param inputList
 * @return
 */
public static String getStringFromList(List<String> inputList){
	StringBuilder outputData = new StringBuilder();
	for(String data : inputList){
		outputData.append(data);
		outputData.append(",");
	}
	return outputData.toString().replaceAll(",$", "");
	
}
/**
 * 
 * @param path
 * @return
 */
public static Properties getProperties(String path){
	
	Properties props = new Properties();
	if(null == path || path.trim().length() == 0)
		return null;
	try{
		 props.load(new FileInputStream(path));
	}catch(Exception e){
		System.out.println("Resources not able to load");
		e.printStackTrace();
	}
	
    return props;
 }
/**
 * 
 * @param startDate
 * @return
 */
 public static DateTime getStartDateofWeek(DateTime startDate){	 
	 DateTime weekStart = startDate.withDayOfWeek( DateTimeConstants.MONDAY ).withTimeAtStartOfDay();
     return weekStart;
	
 }
 /**
  * 
  * @param startDate
  * @return
  */
 public static DateTime getEndDateofWeek(DateTime startDate){	 
	 DateTime weekEnd = startDate.withDayOfWeek(DateTimeConstants.SUNDAY).withTimeAtStartOfDay();
     return weekEnd;
	
 } 
 /**
  * 
  * @param date
  * @return
  */
 public static boolean isDateInCurrentWeek(Date date) {
	  Calendar c = Calendar.getInstance();
	  c.setFirstDayOfWeek(Calendar.MONDAY);

	  c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
	  c.set(Calendar.HOUR_OF_DAY, 0);
	  c.set(Calendar.MINUTE, 0);
	  c.set(Calendar.SECOND, 0);
	  c.set(Calendar.MILLISECOND, 0);
	  Date monday = c.getTime();
	  Date nextMonday= new Date(monday.getTime()+7*24*60*60*1000);
	   boolean isThisWeek = date.after(monday) && date.before(nextMonday);
	   return isThisWeek;
	}
 
}

