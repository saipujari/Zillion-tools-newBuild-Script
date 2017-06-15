package com.zillion.masteravailability.report;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.zillion.api.MasterAvailabilityReportConstants;
import com.zillion.api.MasterAvailabilityReportHelper;

public class ClassroomReport {
	
	public static void main(String[] args) {
		
    	@SuppressWarnings("resource")
		Scanner reader = new Scanner(System.in);  
		System.out.println("Enter UserName: ");
		String userName = reader.nextLine();
		System.out.println("Enter Password: ");
		String password = reader.nextLine();
		System.out.println("Enter Session Types IDs: ");
		String sessionTypeIds = reader.nextLine();
		System.out.println("Enter Report Start Date (MM-DD-YYYY) : ");
		String startDate = reader.nextLine();
		System.out.println("Enter Report End Date (MM-DD-YYYY) : ");
		String endDate = reader.nextLine();
		System.out.println("Enter Report Timezone: ");
		String timeZone = reader.nextLine();
		System.out.println("Enter Properties file path: ");
		String resourcePath = reader.nextLine();
		
		if(null!= userName && null != password && null != sessionTypeIds 
				&& null != startDate && null != endDate && null != timeZone){
			
			Map<String,String> inputParams = new HashMap<String,String>();
			inputParams.put(MasterAvailabilityReportConstants.USERNAME, userName);
			inputParams.put(MasterAvailabilityReportConstants.PASSWORD, password);
			inputParams.put(MasterAvailabilityReportConstants.SESSIONTYPEIDS, sessionTypeIds);
			inputParams.put(MasterAvailabilityReportConstants.STARTDATE, startDate);
			inputParams.put(MasterAvailabilityReportConstants.ENDDATE, endDate);
			inputParams.put(MasterAvailabilityReportConstants.TIMEZONE, timeZone);
			inputParams.put(MasterAvailabilityReportConstants.RESOURCEBUNDLEPATH, resourcePath);
			
			try {
				MasterAvailabilityReportHelper.generateClassroomReport(inputParams);
			} catch(Exception e){
				e.printStackTrace();
			}
		}else{
			System.out.println("Please enter valid username/password");
		}
		
		
	}

}
