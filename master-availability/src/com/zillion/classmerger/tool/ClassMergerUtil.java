package com.zillion.classmerger.tool;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.Years;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClassMergerUtil {

	public static boolean isNull(String checkString) {
        if (null == checkString || checkString.trim().length() == 0 || checkString.trim().equalsIgnoreCase("null")) {
            return true;
        }
        return false;
    }

    public static boolean isNotNull(String checkString) {
        if (null == checkString || checkString.trim().length() == 0 || checkString.trim().equalsIgnoreCase("null")) {
            return false;
        }
        return true;
    }
    
    public static boolean isInvalidObject(Object object) {
        if (null == object) {
            return true;
        }
        return false;
    }
    
    public static boolean isValid(Object object) {
        if (null != object && object.toString().length() > 0 && !object.toString().equalsIgnoreCase("null")) {
            return true;
        }
        return false;
    }
    
    public static boolean isListNonEmpty(List<?> dataList) {
        if (null != dataList && !(dataList.isEmpty())) {
            return true;
        }
        return false;
    }
    
    public static boolean isListEmpty(List<?> dataList) {
        if (null == dataList || dataList.isEmpty()) {
            return true;
        }
        return false;
    }
    
	public static int getDifferenceBetweenTwoDates(Date startDate, Date endDate, String timezoneKey, String offsetType){
		DateTimeZone timezone = DateTimeZone.forID(timezoneKey);
		DateTime startDateTime = new DateTime(new DateTime(startDate),timezone);
		DateTime endDateTime = new DateTime(new DateTime(endDate),timezone);
		if(offsetType.equalsIgnoreCase(ClassMergerConstants.YEARS_FREQUENCY) || offsetType.equalsIgnoreCase(ClassMergerConstants.YEARLY_FREQUENCY))
			return Years.yearsBetween(startDateTime, endDateTime).getYears();
		else if(offsetType.equalsIgnoreCase(ClassMergerConstants.MONTHS_FREQUENCY) || offsetType.equalsIgnoreCase(ClassMergerConstants.MONTHLY_FREQUENCY))
			return Months.monthsBetween(startDateTime, endDateTime).getMonths();
		else if(offsetType.equalsIgnoreCase(ClassMergerConstants.WEEKS_FREQUENCY) || offsetType.equalsIgnoreCase(ClassMergerConstants.WEEKLY_FREQUENCY))
			return Weeks.weeksBetween(startDateTime, endDateTime).getWeeks();
		else if(offsetType.equalsIgnoreCase(ClassMergerConstants.DAYS_FREQUENCY) || offsetType.equalsIgnoreCase(ClassMergerConstants.DAILY_FREQUENCY))
			return Days.daysBetween(startDateTime, endDateTime).getDays();
		else if(offsetType.equalsIgnoreCase(ClassMergerConstants.HOURS_FREQUENCY) || offsetType.equalsIgnoreCase(ClassMergerConstants.HOURLY_FREQUENCY))
			return Hours.hoursBetween(startDateTime, endDateTime).getHours();
		else if(offsetType.equalsIgnoreCase(ClassMergerConstants.MINUTES_FREQUENCY) || offsetType.equalsIgnoreCase(ClassMergerConstants.MINUTELY_FREQUENCY))
			return Minutes.minutesBetween(startDateTime, endDateTime).getMinutes();
		return Days.daysBetween(startDateTime, endDateTime).getDays();
	}

    /**
     * 
     * @param pojo
     * @return
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static String convertPojoToString(Object pojo)  throws JsonGenerationException, JsonMappingException, IOException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
            String json = mapper.writeValueAsString(pojo);
            return json;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }
}