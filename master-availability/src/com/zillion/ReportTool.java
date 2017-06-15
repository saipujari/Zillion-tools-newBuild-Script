/**
 * 
 */
package com.zillion;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.zillion.api.MasterAvailabilityReportConstants;
import com.zillion.api.MasterAvailabilityReportHelper;
import com.zillion.availability.report.AvailabilityLoader;
import com.zillion.migration.MigrationConstants;
import com.zillion.migration.MigrationHelper;

/**
 * @author vijayakumar.s
 *
 */
public class ReportTool {

	/**
	 * @param commandLineArgs
	 */
	@SuppressWarnings("resource")
    public static void main(String[] commandLineArgs) {
		
		if(commandLineArgs.length ==0) {
			System.out.println("------------------------------------------------------------------");
			System.out.println("-------Reporting Tool---------------------------------------------");
			System.out.println("-------Please Select the report to be generated-------------------");
			System.out.println("-------1. CAPACITY-REPORT-----------------------------------------");
			System.out.println("-------2. MASTER-AVAILABILITY-REPORT------------------------------");
			System.out.println("-------3. CLASSROOM-REPORT----------------------------------------");
			System.out.println("------------------------------------------------------------------");
			System.out.println("--Please Enter the Serial Number of the report to be generated----");
			Scanner reader = new Scanner(System.in);
			String reportOption = reader.nextLine();
			if("1".equalsIgnoreCase(reportOption)) {
				
			} else if("2".equalsIgnoreCase(reportOption)) {
				
			} else if("3".equalsIgnoreCase(reportOption)) {
				
			}
		} else {
			if("CAPACITY-REPORT".equalsIgnoreCase(commandLineArgs[0])) {
				if(commandLineArgs.length >=8) {
				Map<String,String> inputParams = new HashMap<String,String>();
				inputParams.put(MasterAvailabilityReportConstants.SESSIONTYPEID, commandLineArgs[1]);
				inputParams.put(MasterAvailabilityReportConstants.COACHIDS_TO_IGNORE, commandLineArgs[2]);
				inputParams.put(MasterAvailabilityReportConstants.SESSION_DURATION, commandLineArgs[3]);
				inputParams.put(MasterAvailabilityReportConstants.STARTDATE, commandLineArgs[4]);
				inputParams.put(MasterAvailabilityReportConstants.NOOFWEEKS, commandLineArgs[5]);
				inputParams.put(MasterAvailabilityReportConstants.TIMEZONE, commandLineArgs[6]);
				inputParams.put(MasterAvailabilityReportConstants.RESOURCEBUNDLEPATH, commandLineArgs[7]);
				inputParams.put(MasterAvailabilityReportConstants.INCLUDEID, commandLineArgs[8]);
				AvailabilityLoader.generateCapacityReport(inputParams);
				}else{
					System.out.println("Insufficient Parameters to generate the report");
				}
			} else if("MASTER-AVAILABILITY-REPORT".equalsIgnoreCase(commandLineArgs[0])) {
				Map<String,String> inputParams = new HashMap<String,String>();
				if(commandLineArgs.length >=9) {
					inputParams.put(MasterAvailabilityReportConstants.USERNAME, commandLineArgs[1]);
					inputParams.put(MasterAvailabilityReportConstants.PASSWORD, commandLineArgs[2]);
					inputParams.put(MasterAvailabilityReportConstants.SESSIONTYPEIDS, commandLineArgs[3]);
					inputParams.put(MasterAvailabilityReportConstants.STARTDATE, commandLineArgs[4]);
					inputParams.put(MasterAvailabilityReportConstants.ENDDATE, commandLineArgs[5]);
					inputParams.put(MasterAvailabilityReportConstants.TIMEZONE, commandLineArgs[6]);
					inputParams.put(MasterAvailabilityReportConstants.RESOURCEBUNDLEPATH, commandLineArgs[7]);
					inputParams.put(MasterAvailabilityReportConstants.INCLUDEID, commandLineArgs[8]);
					try {
						MasterAvailabilityReportHelper.generateMasterAvilabilityReprot(inputParams);
					} catch(Exception e){
						e.printStackTrace();
					}
				} else {
					System.out.println("Insufficient Parameters to generate the report");
				}
				
			} else if("CLASSROOM-REPORT".equalsIgnoreCase(commandLineArgs[0])) {
				Map<String,String> inputParams = new HashMap<String,String>();
				if(commandLineArgs.length >=9) {
					inputParams.put(MasterAvailabilityReportConstants.USERNAME, commandLineArgs[1]);
					inputParams.put(MasterAvailabilityReportConstants.PASSWORD, commandLineArgs[2]);
					inputParams.put(MasterAvailabilityReportConstants.SESSIONTYPEIDS, commandLineArgs[3]);
					inputParams.put(MasterAvailabilityReportConstants.STARTDATE, commandLineArgs[4]);
					inputParams.put(MasterAvailabilityReportConstants.ENDDATE, commandLineArgs[5]);
					inputParams.put(MasterAvailabilityReportConstants.TIMEZONE, commandLineArgs[6]);
					inputParams.put(MasterAvailabilityReportConstants.RESOURCEBUNDLEPATH, commandLineArgs[7]);
					inputParams.put(MasterAvailabilityReportConstants.INCLUDEID, commandLineArgs[8]);
					try {
						MasterAvailabilityReportHelper.generateClassroomReport(inputParams);
					} catch(Exception e){
						e.printStackTrace();
					}
				} else {
					System.out.println("Insufficient Parameters to generate the report");
				}
			} else if("GROUPPE-MIGRATION".equalsIgnoreCase(commandLineArgs[0])) {			    
			    if (commandLineArgs.length >= 5) {
		            Map<String, String> inputParams = new HashMap<String, String>();
		            inputParams.put(MigrationConstants.ONBOARDING_STATUS, commandLineArgs[1]);
		            inputParams.put(MigrationConstants.COUNT, commandLineArgs[2]);
                    inputParams.put(MigrationConstants.RESOURCEBUNDLEPATH, commandLineArgs[3]);
		            inputParams.put(MigrationConstants.MEMBERS_IDS, commandLineArgs[4]);
		            try {
		                MigrationHelper.migrateMembers(inputParams);
		            } catch (Exception e) {
		                e.printStackTrace();
		            }
		        } else {
		            System.out.println("Insufficient Parameters to start Group PE migration");
		        }
			}
		}

	}

}
