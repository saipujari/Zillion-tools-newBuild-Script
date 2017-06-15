Pre-requisites :
============

Code will be used to generated the report week by week.
Will comment the other lines.

masteravailabilitytool
├── bin
├── config
│   ├── masteravilabilityresource_choc.properties
│   └── masteravilabilityresource_pistachio.properties
├── jars
│   ├── masteravilabilityresource_choc.properties
│   ├── masteravilabilityresource_pistachio.properties
│   └── ReportTool.jar
├── lib
│   ├── jackson-annotations-2.6.4.jar
│   ├── jackson-core-2.6.3.jar
│   ├── jackson-databind-2.6.3.jar
│   ├── joda-time-2.3.jar
│   ├── json-20151123.jar
│   └── ojdbc7.jar
├── ojdbc7.jar
├── Readme.txt
├── resources
│   └── masteravilabilityresource.properties
└── src
    └── com
        └── zillion
            ├── api
            │   ├── CsvExportUtil.java
            │   ├── MasterAvailabilityReportConstants.java
            │   ├── MasterAvailabilityReportHelper.java
            │   ├── ReportAPIHelper.java
            │   ├── ZillionRequest.java
            │   ├── ZillionResponse.java
            │   └── ZillionUtil.java
            ├── availability
            │   ├── model
            │   │   ├── CoachInfo.java
            │   │   ├── TimeBlock.java
            │   │   └── TimeSegment.java
            │   └── report
            │       ├── AvailabilityLoader.java
            │       └── AvailabilityReport.java
            ├── db
            │   └── ReportDbHelper.java
            ├── masteravailability
            │   ├── model
            │   │   ├── Classroom.java
            │   │   ├── Event.java
            │   │   ├── Events.java
            │   │   ├── Provider.java
            │   │   └── SessionType.java
            │   └── report
            │       ├── ClassroomReport.java
            │       └── MasterAvailabilityReport.java
            ├── package-info.java
            └── ReportTool.java
            

Steps to run
============

1. Checkout the below jar file to the box : 

https://scm.healthfleet.com/svn/reporttools/branches/masteravailabilitytool/jars/ReportTool.jar

2. Command format
java -jar ReportTool.jar <REPORT-TYPE> <PARAMETERS>
<REPORT-TYPE> 			is the type of report to generate: MASTER-AVAILABILITY-REPORT, CLASSROOM-REPORT, or CAPACITY-REPORT
<PARAMETERS> 			vary depending on the report generated.

Parameter explanation
for CAPACITY-REPORT: 
<SESSION-TYPE-ID> <IGNORE-COACH-IDS> <START-DATE> <NUM-WEEKS> <TIMEZONE> <SESSION-DURATION-MINS> <RESOURCE-PATH> <INCLUDE-ID>
SESSION-TYPE-ID: 		ID of session type to genearte the report for.  Should be 13 for Customziation Session
IGNORE-COACH-IDS: 		comma delimmited list of coach ids to exclude from the report.  For now, only 07
SESSION-DURATION-MINS:	total time to allocate for a coach for each session - use 30 for customization sessions
START-DATE:				date when report needs to run.  Format is MM-DD-YYYY
NUM-WEEKS:				number of weeks to include in the report. Should be 5 - ie: current week plus 4 more
TIMEZONE:				the timezone that is applied to the start date
RESOURCE-PATH:			the path to where the .properties file is located
INCLUDE-ID              YES or NO (If YES means report include provider id other wise provider id not included in report)
 

for MASTER-AVAILABILITY-REPORT:
<USERNAME> <PASSWORD> <SESSION-TYPE-IDS> <START-DATE> <END-DATE> <TIMEZONE> <RESOURCE-PATH> <INCLUDE-ID>
USERNAME:				username/password is used to call APIs needed to get report data
PASSWORD:				username/password is used to call APIs needed to get report data
SESSION-TYPE-IDS:		Comma delimited list of session type IDs.  Run the report for all providers approved to deliver these session type ids.
START-DATE:				date when report needs to run.  Format is MM-DD-YYYY
END-DATE:				date when report needs to end.  Format is MM-DD-YYYY
TIMEZONE:				the timezone that is applied to the start date
RESOURCE-PATH:			the path to where the .properties file is located
INCLUDE-ID              YES or NO (If YES means report include provider id other wise provider id not included in report)

for CLASSROOMREPORT:

<USERNAME> <PASSWORD> <SESSION-TYPE-IDS> <START-DATE> <END-DATE> <TIMEZONE> <RESOURCE-PATH> <INCLUDE-ID>
USERNAME:				username/password is used to call APIs needed to get report data
PASSWORD:				username/password is used to call APIs needed to get report data
SESSION-TYPE-IDS:		Comma delimited list of session type IDs.  Run the report for all providers approved to deliver these session type ids.
START-DATE:				date when report needs to run.  Format is MM-DD-YYYY
END-DATE:				date when report needs to end.  Format is MM-DD-YYYY
TIMEZONE:				the timezone that is applied to the start date
RESOURCE-PATH:			the path to where the .properties file is located
INCLUDE-ID              YES or NO (If YES means classroom report include class room ids other wise classroom id not included in report)


3. Example commands to generate the corresponding report :

CAPACITY-REPORT :
export JAVA_HOME=/opt/jdk8/
set startDate='date +"%m-%d-%y"'
$JAVA_HOME/bin/java -jar ReportTool.jar CAPACITY-REPORT 13 07 20 $startDate 2 Asia/Kolkata /data/report/masteravilabilityresource.properties YES

OUTPUT FILE FORMAT : CapacityReport-MM-DD-YYYY.csv

MASTER AVAILABILITY REPORT : 
export JAVA_HOME=/opt/jdk8/
$JAVA_HOME/bin/java  -jar ReportTool.jar MASTER-AVAILABILITY-REPORT seededprogramadmin@healthfleet.com Healthfleet2015 01,02,05,13  12-01-2015 12-02-2015 Asia/Kolkata /data/report/masteravilabilityresource.properties YES

OUTPUT FILE FORMAT : MasterAvailability-MM-DD-YYYY.csv

CLASSROOM REPORT : 
export JAVA_HOME=/opt/jdk8/
$JAVA_HOME/bin/java  -jar ReportTool.jar CLASSROOM-REPORT seededprogramadmin@healthfleet.com Healthfleet2015 01,02,05,13  12-01-2015 12-02-2015 Asia/Kolkata /data/report/masteravilabilityresource.properties YES

OUTPUT FILE FORMAT : ClassroomReport-MM-DD-YYYY.csv
