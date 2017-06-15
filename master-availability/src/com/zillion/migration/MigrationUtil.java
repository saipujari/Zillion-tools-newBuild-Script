/**
 * 
 */
package com.zillion.migration;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.zillion.api.MasterAvailabilityReportHelper;
import com.zillion.db.ReportDbHelper;

/**
 * @author arunkumar.d
 *
 */
public class MigrationUtil {

    private static ResourceBundle migrationResource = null;
    public static Connection connection = null;
    final static Logger logger = Logger.getLogger(MigrationUtil.class);
    /**
     * 
     * @param inputParams
     * @throws Exception
     * This method used to load properties based on input file path
     */
    public static void loadMigrationQueries(Map<String, String> inputParams) throws Exception {
        migrationResource =  ResourceBundle.getBundle("migrationqueries", Locale.ENGLISH);
      
    }

    /**
     * 
     * @param inputParams
     * @return
     * @throws Exception
     */
    public static Connection getDatabaseConnection(Map<String, String> inputParams) throws Exception {
        MasterAvailabilityReportHelper.setProprties(inputParams);
        connection = ReportDbHelper.getDatabaseConnection();
        if (null == connection) {
            throw new Exception("Invalid Database connection, Please check connection properties");
        }
        return connection;
    }

    /**
     * 
     * @param preparedStatement
     * @param rs
     */
    public static void closeStatementAndResultset(PreparedStatement preparedStatement, ResultSet rs) {
        if (null != preparedStatement) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                logger.error("Warning : Exception while closing PreparedStatement");
                e.printStackTrace();
            }
        }
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("Warning : Exception while closing ResultSet");
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * @param resourceKey
     * @return
     * @throws Exception
     *  Get query from resources based on query key
     */
    public static String getResource(String resourceKey) throws Exception {
        String query = migrationResource.getString(resourceKey);
        if (query == null || query.trim().length() == 0) {
            throw new Exception("Invalid " + resourceKey + " properties key, Please check properties file");
        }
        return query;
    }

    /**
     * 
     * 
     * @param onboardingStatus
     * @return
     * @throws Exception
     */
    public static String getDynamicCondition(String[] onboardingStatus) throws Exception {
        StringBuilder query = new StringBuilder();
        for (int index = 0; index < onboardingStatus.length; index++) {
            query.append("?,");
        }
        query.deleteCharAt(query.length() - 1);
        return query.toString();
    }

    /**
     * 
     * @param onboardingStatus
     * @return
     * @throws Exception
     */
    public static String getDynamicQuery(String[] onboardingStatus, String queryKey) throws Exception {
        String dynamicCondition = getDynamicCondition(onboardingStatus);
        String onboardingQuery = getResource(queryKey).replaceAll("<DYNAMICCONDITION>", dynamicCondition);
        return onboardingQuery;
    }

    /**
     * 
     * @param count
     * @param onboardingStatus
     * @return
     * @throws Exception
     * This method return PreparedStatement based on boarding status,count and member ids
     * to skip from input parameters 
     */
    public static PreparedStatement getEligibleMembersWithCount(String count, String[] onboardingStatus, String[] membersToSkip) throws Exception {
        PreparedStatement preparedStatement = null;
        StringBuilder query = new StringBuilder();
        String onboardingQuery  = getDynamicQuery(onboardingStatus, "ONBOARDING_STAUS_QUERY");
        String orderBy          = getResource("ORDER_BY");
        String baseQuery        = getResource("BASE_QUERY");
        String rowNumQuery      = getResource("ROWNUM_CONDTION");
        String masterProgram    = MigrationUtil.getResource("MAST_OLD_PROGRAM_ID");
        // Start building the query
        query.append("SELECT ACCOUNT_ID,ORGID,SESSIONTYPE FROM ( ");
        //Adding onboarding status in that query 
        query.append(baseQuery).append(MigrationConstants.AND).append(onboardingQuery).append(MigrationConstants.SPACE);
        //This condition used to skip any particular members, If not means does not appending the members to skip query  
        if (MigrationUtil.isValid(membersToSkip)) {
            String accountIdNotIn = getDynamicQuery(membersToSkip, "ACCOUNT_ID_NOT_IN");
            query.append(MigrationConstants.AND).append(accountIdNotIn).append(MigrationConstants.SPACE);
        }
        // Adding order by to make sure does not missing any records while applying count
        query.append(orderBy);
        // Adding count in the query 
        query.append(" )").append(MigrationConstants.WHERE).append(rowNumQuery);
        preparedStatement = connection.prepareStatement(query.toString());
        logger.info("Eligible Members With Count : "+query.toString());
        preparedStatement.setString(1, masterProgram);
        int index = 0;
        int settingIndex = 2;
        //Dynamically setting onboarding status
        for (; index < onboardingStatus.length; index++) {
            preparedStatement.setString(settingIndex, onboardingStatus[index].trim());
            settingIndex++;
        }
        if (MigrationUtil.isValid(membersToSkip)) {
            for (index = 0; index < membersToSkip.length; index++) {
                preparedStatement.setString(settingIndex, membersToSkip[index]);
                settingIndex++;
                logger.debug("Skipping account Id : " + membersToSkip[index].trim());
            }
        }
        preparedStatement.setString(settingIndex, count);
        return preparedStatement;
    }

    /**
     * 
     * @param members
     * @param onboardingStatus
     * @return
     * @throws Exception
     *  This method return PreparedStatement based on boarding status and member ids from input parameters 
     */
    public static PreparedStatement getSpecifiedMembers(String[] members, String[] onboardingStatus) throws Exception {
        PreparedStatement preparedStatement = null;
        StringBuilder query = new StringBuilder();
        String onboardingQuery = getDynamicQuery(onboardingStatus, "ONBOARDING_STAUS_QUERY");
        String baseQuery = getResource("BASE_QUERY");
        String masterProgram = MigrationUtil.getResource("MAST_OLD_PROGRAM_ID");
        String accountIdQuery = getDynamicQuery(members, "ACCOUNT_ID_QUERY");
        query.append(baseQuery).append(MigrationConstants.AND).append(onboardingQuery).append(MigrationConstants.AND);
        query.append(accountIdQuery);
        logger.info("Get Specified Members query : "+query.toString());
        preparedStatement = connection.prepareStatement(query.toString());
        preparedStatement.setString(1, masterProgram);
        int index = 0;
        int settingIndex = 2;
        for (; index < onboardingStatus.length; index++) {
            preparedStatement.setString(settingIndex, onboardingStatus[index].trim());
            settingIndex++;
        }
        for (index = 0; index < members.length; index++) {
            preparedStatement.setString(settingIndex, members[index].trim());
            settingIndex++;
        }
        return preparedStatement;
    }

    /**
     * 
     * @param onboardingStatus
     * @return
     * @throws Exception
     * This method return PreparedStatement based on onboarding status and member ids to  
     * skip input parameters   
     */
    public static PreparedStatement getMembersByOnboardingStatus(String[] onboardingStatus, String[] membersToSkip) throws Exception {
        PreparedStatement preparedStatement = null;
        StringBuilder query = new StringBuilder();
        String onboardingQuery = getDynamicQuery(onboardingStatus, "ONBOARDING_STAUS_QUERY");
        String orderBy = getResource("ORDER_BY");
        String baseQuery = getResource("BASE_QUERY");
        String masterProgram = MigrationUtil.getResource("MAST_OLD_PROGRAM_ID");
        // Dynamic query building based on onboarding status
        query.append(baseQuery).append(MigrationConstants.AND).append(onboardingQuery).append(MigrationConstants.SPACE);
        if (null != membersToSkip) {
            String accountIdNotIn = getDynamicQuery(membersToSkip, "ACCOUNT_ID_NOT_IN");
            query.append(MigrationConstants.AND).append(accountIdNotIn).append(MigrationConstants.SPACE);
        }
        query.append(orderBy);
        preparedStatement = connection.prepareStatement(query.toString());
        logger.info("Get Members By Onboarding Status query : "+query.toString());
        preparedStatement.setString(1, masterProgram);
        int index = 0;
        int settingIndex = 2;
        for (; index < onboardingStatus.length; index++) {
            preparedStatement.setString(settingIndex, onboardingStatus[index].trim());
            settingIndex++;
        }
        if (null != membersToSkip) {
            for (index = 0; index < membersToSkip.length; index++) {
                preparedStatement.setString(settingIndex, membersToSkip[index].trim());
                logger.debug("Skipping account Id : " + membersToSkip[index].trim());
            }
        }
        return preparedStatement;
    }

    /**
     * 
     * @param date
     * @param days
     * @return
     * This method used to 
     */
    public static Date addDaysIntoDate(Date date, int days) {
        DateTime dateTime = new DateTime(date);
        dateTime = dateTime.plusDays(days);
        return dateTime.toDate();
    }

    /**
     * 
     * @param accoundId
     * @param accountProgramMap
     * @param newCustProgram
     * @return
     * This method used to calculate end date and end date time based on start date, start date time
     * and duration
     */
    public static Map<String, Date> getCombinedData(String accoundId, Map<String, String> accountProgramMap, Map<String, String> newCustProgram) throws Exception {
        String startDateSt = null, startDateTimeSt = null;
        Map<String, Date> combineData = new HashMap<String, Date>();
        try {
            if (newCustProgram.size() == 0) {
                throw new Exception("New customization program  not avilable");
            }
            startDateSt = accountProgramMap.get("START_DT");
            startDateTimeSt = accountProgramMap.get("START_DT_TIME");
            String units = newCustProgram.get("TERM_DURATION_UNITS");
            Integer durationLength = Integer.parseInt(newCustProgram.get("TERM_DURATION_LENGTH"));
            if (units.equalsIgnoreCase("Days")) {
                durationLength = durationLength * 1;
            } 
            else if (units.equalsIgnoreCase("Weeks")) {
                durationLength = durationLength * 7;
            } 
            else if (units.equalsIgnoreCase("Years")) {
                durationLength = durationLength * 365;
            }
            if (null == startDateSt || null == startDateTimeSt) {
                throw new Exception( "ACCOUNT_PROGRAM or ACCOUNT_PROGRAM_MEMBERSHIP Start date or start date time is invalid for account Id : " + accoundId);
            }
            DateFormat startDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
            DateFormat startDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date startDate = startDateFormat.parse(startDateSt);
            Date startDateTime = startDateTimeFormat.parse(startDateTimeSt);
            Date endDate = MigrationUtil.addDaysIntoDate(startDate, durationLength);
            Date endDateTime = MigrationUtil.addDaysIntoDate(startDateTime, durationLength);
            if(endDate.before(new Date()) || endDate.equals(new Date())) {
                startDate     = new Date();
                startDateTime = new Date();
                endDate       = MigrationUtil.addDaysIntoDate(startDate, durationLength);
                endDateTime   =  MigrationUtil.addDaysIntoDate(startDateTime, durationLength);
            }
            combineData.put("START_DT", startDate);
            combineData.put("START_DT_TIME", startDateTime);
            combineData.put("END_DT", endDate);
            combineData.put("END_DT_TIME", endDateTime);
        } catch (Exception e) {
            logger.error("Warning : Invalid date's for accountId : " + accoundId);
            throw e;
        }
        return combineData;
    }

    /**
     * 
     * @param date
     * @param format
     * @return
     * @throws Exception
     * This method used to convert date from starting and input format
     */
    public static Date dateFromString(String date, String format) throws Exception {
        if (!MigrationUtil.isValid(date) || !MigrationUtil.isValid(format)) {
            throw new Exception("Invalid date : unable to  parse Date : " + date + " Format :" + format);
        }
        DateFormat startDateFormat = new SimpleDateFormat(format);
        return startDateFormat.parse(date);
    }

    /***
     * 
     * @param inputParams
     * @throws Exception
     * This method used to validate input parameters
     */
    public static void validateInput(Map<String, String> inputParams) throws Exception {
       if (inputParams.get(MigrationConstants.ONBOARDING_STATUS) == null || inputParams.get(MigrationConstants.ONBOARDING_STATUS).trim().length() == 0) {
            throw new Exception("Input (onboardingStatus): Onboarding status is null, Please check your input params");
       }
       else if((inputParams.get(MigrationConstants.MEMBERS_IDS) == null || inputParams.get(MigrationConstants.MEMBERS_IDS).trim().length() == 0) &&
            (inputParams.get(MigrationConstants.COUNT) == null || inputParams.get(MigrationConstants.COUNT).trim().length() == 0)) {
            throw new Exception("Input parameters : member ids or count is mandatory");
       }
       String []  onboardingStatusArray = inputParams.get(MigrationConstants.ONBOARDING_STATUS).split(MigrationConstants.COMMA);
       String  onboardingStatus =  MigrationUtil.getResource("ONBOARDING_STATUS");
       for (String status : onboardingStatusArray) {
           if(!onboardingStatus.contains(status)) {
               throw new Exception("Input parameters : Invalid onboarding status : "+status); 
           }
       }
    }
    
    /**
     * 
     * @param onboardingHistory
     * @return
     * @throws Exception
     * This method used to get member onboarding history
     * based on old program onboarding history  
     */
    public static String getOnboardingHistory(String onboardingHistory) throws Exception {
        String newHistory = null;
        if (MigrationUtil.isValid(onboardingHistory)) {
            int endIndex = onboardingHistory.length();
            if (onboardingHistory.contains(";Age")) {
                endIndex = onboardingHistory.indexOf(";Age");
            }
            newHistory = onboardingHistory.substring(0, endIndex);
        }
        return newHistory;
    } 

    /**
     * 
     * @param object
     * @return
     */
    public static boolean isValid(Object object) {
        if (null != object && object.toString().length() > 0 && !object.toString().equalsIgnoreCase("null")) {
            return true;
        }
        return false;
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
            logger.error("Unable to load Resources");
            e.printStackTrace();
        }
        return props;
     }
    
}