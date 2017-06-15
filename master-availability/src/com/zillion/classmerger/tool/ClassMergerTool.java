package com.zillion.classmerger.tool;

/**
 * @author Ganesan
 *
 */
public class ClassMergerTool {

	/**
	 * @param commandLineArgs
	 */
	public static void main(String[] commandLineArgs) {
		if(commandLineArgs.length >=5) {
			String username = commandLineArgs[0];
			String password = commandLineArgs[1];
			String sourceClassroomName = commandLineArgs[2];
			String targetClassroomName = commandLineArgs[3];
			String path = commandLineArgs[4];
			ClassMergerHelper.mergeClasses(sourceClassroomName, targetClassroomName, username, password, path);
		} 
		else 
		{
			System.out.println("Insufficient Parameters to merge the classes");
		}
		
		//String path = "/Users/ganesan/Documents/Development_RealAppeal/PROMETHEUS_REPORTTOOLS/resources/mergeclasstool_strawberry.properties";
		//String path = "/Users/ganesan/Downloads/ClassMergerTool/mergeclasstool_sorbet.properties";
		
		// Error ::: Insufficient Parameters to merge the classes
		// ClassMergerHelper.mergeClasses(null, null, null, null, null);
		
		// Error ::: Source & Target Classroom Should not be same
		// ClassMergerHelper.mergeClasses("HBMI-NPD-DEC-30-2015-WED-08:00-EST-12437699", "HBMI-NPD-DEC-30-2015-WED-08:00-EST-12437699", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);

		// Error ::: Source classroom does not exist
		// ClassMergerHelper.mergeClasses("HBMI-NPD-DEC-30-2015-WED-08:00-EST-12437698", "HBMI-NPD-DEC-30-2015-WED-08:00-EST-12437699", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);
		
		// Error ::: Target classroom does not exist
		// ClassMergerHelper.mergeClasses("HBMI-NPD-MAR-21-2017-TUE-08:00-EDT-58441023", "HBMI-NPD-DEC-30-2015-WED-08:00-EST-12437698", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);
		
		// Error ::: Source & Target Classroom Programs are differnet
		// ClassMergerHelper.mergeClasses("HBMI-NPD-MAR-21-2017-TUE-08:00-EDT-58441023", "RA-PD-APR-17-2017-MON-12:15-EDT-73546532", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);
		
		// Error ::: Source & Target Classroom Classifications are differnet
		// ClassMergerHelper.mergeClasses("RA-PD-APR-17-2017-MON-12:15-EDT-73546532", "RA-NPD-APR-17-2017-MON-18:00-EDT-88566564", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);
		
		// Error ::: Source Classroom Not Approved Or Start Date is in Future
		// ClassMergerHelper.mergeClasses("RA-NPD-APR-20-2017-THU-02:45-EDT-79581588", "RA-NPD-APR-20-2017-THU-09:30-EDT-17927119", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);
		
		// Error ::: Target Classroom Not Approved Or Start Date is in Future
		// ClassMergerHelper.mergeClasses("RA-NPD-APR-20-2017-THU-02:45-EDT-79581588", "RA-NPD-APR-26-2017-WED-13:30-EDT-13134702", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);

		// Error ::: Target Classroom Doesn't have enough space to accommodate members from source classroom
		// ClassMergerHelper.mergeClasses("RA-NPD-APR-19-2017-WED-12:00-EDT-63163555", "RA-NPD-APR-19-2017-WED-10:00-EDT-49975725", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);
		
		// Error ::: Trying to move the members from source to target classroom not meeting the interval range (+/- 2 intervals)
		// ClassMergerHelper.mergeClasses("RA-NPD-APR-19-2017-WED-07:00-EDT-18331751", "RA-NPD-APR-12-2017-WED-18:00-EDT-47160074", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);
		
		// Error ::: Classroom movement between Interval #0 and a future interval or from a future interval to Interval #0 is not allowed. Please enter an interval to interval combination that is valid.
		// ClassMergerHelper.mergeClasses("RA-NPD-APR-20-2017-THU-09:30-EDT-83018021", "RA-NPD-APR-17-2017-MON-07:00-EDT-83847039", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);
		// ClassMergerHelper.mergeClasses("RA-NPD-APR-17-2017-MON-07:00-EDT-83847039", "RA-NPD-APR-20-2017-THU-09:30-EDT-83018021", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);
		
		// Error ::: Trying to move the members from source to target classrooms are not meeting the classroom session count
		// ClassMergerHelper.mergeClasses("RA-PD-MAR-17-2017-FRI-16:00-EDT-89530740","RA-PD-MAR-30-2017-THU-10:45-EDT-50975113", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);
		
		// Success ::: Movement between classroom which in 0th interval and classroom session count 53.
		// ClassMergerHelper.mergeClasses("RA-NPD-APR-20-2017-THU-17:15-EDT-25914250","RA-NPD-APR-20-2017-THU-17:15-EDT-84246686", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);

	    // Success ::: Movement between classroom which in 0th interval and classroom session count 52.
        // ClassMergerHelper.mergeClasses("RA-NPD-APR-20-2017-THU-02:45-EDT-79581588","RA-NPD-APR-20-2017-THU-01:30-EDT-97922777", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);

		// Success ::: Movement between classroom which in >=1 interval
		// ClassMergerHelper.mergeClasses("RA-PD-MAR-30-2017-THU-19:00-EDT-56271312","RA-PD-MAR-30-2017-THU-10:45-EDT-50975113", "seededprogramadmin@healthfleet.com", "Healthfleet2015", path);

	}
}