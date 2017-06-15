/**
 * 
 */
package com.zillion.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author arunkumar.d
 *
 */
public class MigrationHelper {

    static Connection connection = null;
    final static Logger logger = Logger.getLogger(MigrationHelper.class);

    /**
     * 
     * @param inputParams
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static void migrateMembers(Map<String, String> inputParams) throws Exception {
        Long startTime = System.currentTimeMillis();
        List<String> accountIdsToProess = null;
        List<Object> memberDetails = null;
        Map<String, String> newMasterProgramMap = null;
        Map<String,String> organizationMap = null;
        
        // Load Query Properties
        MigrationUtil.loadMigrationQueries(inputParams);        
        
        // Validate input parameters
        MigrationUtil.validateInput(inputParams);
        
        try {
            // Getting database connection
            connection = MigrationUtil.getDatabaseConnection(inputParams);
            
            // Retrieving eligible members to process
            memberDetails      = getEligibleMembersToMigrate(inputParams);
            accountIdsToProess = (List<String>) memberDetails.get(0);
            organizationMap             = (Map<String, String>) memberDetails.get(1);
            
            // Retrieving new onboarding program details
            String newProgram = MigrationUtil.getResource("MAST_NEW_PROGRAM_ID");
            newMasterProgramMap = getProgramDetails(newProgram);
        } catch (Exception e) {
            throw e;
        } finally {
            connection.close();
        }

        // Processing members one by one, If any exception means skip that particular member and start processing the next member
        int index =  1;
        for (String accountId : accountIdsToProess) {
            
            logger.debug("------------------------------ Start processing member :"+index+"--------------------------------------");
            logger.debug("Start processing account Id : " + accountId);

            // one connection for one account id, After processing account id the connection will closed automatically
            connection = MigrationUtil.getDatabaseConnection(inputParams);
            
            // Setting auto commit is false for roll back transaction if any exception
            connection.setAutoCommit(false);

            try {
                // ACCOUNT_PROGRAM Retrieving account program details for group pe eligible members
                String masterProgram = MigrationUtil.getResource("MAST_OLD_PROGRAM_ID");;
                Map<String, String> accountProgramMap = getAccountProgram(accountId,masterProgram);
                
                //Get summary account todate  value
                
                Map<String, String> summaryAccountTodate = getSummaryAccountTodate(accountId);
                
                // Update old account program as in active
                updateAccountProgram(accountId, accountProgramMap);
                
                // Create new account program
                createNewAccountProgram(accountId, accountProgramMap, newMasterProgramMap);
                
                // Delete non on boarding program based on account id's
                deleteNonOnboardingProgram(accountId);
                
                // Delete non on boarding account program membership based on account id's
                deleteNonOnboardingAccountPrmMembership(accountId);

                // ACCOUNT_PROGRAM_MEMBERSHIP
                // Retrieving account program membership
                Map<String, String> actProgramMembershipmap = getAccountProgramMembership(accountId);
                
                // Update the account program membership
                updateAccountProgramMembership(accountId, actProgramMembershipmap);
                
                // Create new account program membership
                createNewAccountProgramMembership(accountId, actProgramMembershipmap, newMasterProgramMap,organizationMap);
                
                // update account provider assigned 
                updateAccountProviderAssigned(accountId, accountProgramMap);
                
                // Create entry in Account Program Interval Activity
                createAccountProgramActivity(accountId,accountProgramMap,summaryAccountTodate);

                // Update summary account todate
                updateSummaryAccountTodate(accountId, newMasterProgramMap, accountProgramMap);

                connection.commit();
                logger.debug("Accont Id :" + accountId + " has been  processed");
            } catch (Exception e) {
                connection.rollback();
                e.printStackTrace();
                logger.error("Exception while Processing accountId : " + accountId);
            } finally {
                connection.close();
            }
            logger.debug("------------------------------ End Processing --------------------------------------");
            index++;
            
        }
        Long endTime = System.currentTimeMillis();
        logger.debug("Total time taken for processing: " + (endTime - startTime));
    }

    /**
     * 
     * @return List<String>
     * @throws Exception
     *  This method find the eligible members for group pe
     */
    private static List<Object> getEligibleMembersToMigrate(Map<String, String> inputParams) throws Exception {
        PreparedStatement preparedStatement = null;
        ResultSet resultset = null;
        String[] memberIdArray = null;
        String[] onboardingStatusArray = null;
        Map<String,String> organizationIdMap = new HashMap<String,String>();
        List<Object> memberDetails =  new ArrayList<Object>();
        try {
            List<String> accountIdToProcess = new ArrayList<String>();
            if (inputParams.get(MigrationConstants.ONBOARDING_STATUS) == null || inputParams.get(MigrationConstants.ONBOARDING_STATUS).trim().length() == 0) {
                throw new Exception("Input (onboardingStatus): Onboarding status is null, Please check your input params");
            }
            onboardingStatusArray = inputParams.get(MigrationConstants.ONBOARDING_STATUS).split(MigrationConstants.COMMA);
            if (MigrationUtil.isValid(inputParams.get(MigrationConstants.COUNT))) {
                String count = inputParams.get(MigrationConstants.COUNT);
                String[]  membersToSkip = null;                
                if(MigrationUtil.isValid(inputParams.get(MigrationConstants.MEMBERS_IDS_TO_SKIP))) {
                    membersToSkip = inputParams.get(MigrationConstants.MEMBERS_IDS_TO_SKIP).split(MigrationConstants.COMMA);
                }
                preparedStatement = MigrationUtil.getEligibleMembersWithCount(count,onboardingStatusArray,membersToSkip);
            } 
            else if (MigrationUtil.isValid(inputParams.get(MigrationConstants.MEMBERS_IDS))) {
                memberIdArray = inputParams.get(MigrationConstants.MEMBERS_IDS).split(MigrationConstants.COMMA);
                preparedStatement = MigrationUtil.getSpecifiedMembers(memberIdArray, onboardingStatusArray);
            } 
            else {
                throw new Exception("Invalid Input: Please pass input count or memberIds or memberIdsToSkip");
            }
            resultset = preparedStatement.executeQuery();
            while (resultset.next()) {
                if (isValidAccountToProcess(resultset.getString("ACCOUNT_ID"))) {
                    accountIdToProcess.add(resultset.getString("ACCOUNT_ID"));
                    organizationIdMap.put(resultset.getString("ACCOUNT_ID"), resultset.getString("ORGID"));
                    logger.debug("Account id taken for processing  :  " + resultset.getString("ACCOUNT_ID"));
                }
            }

            if (accountIdToProcess.size() == 0) {
                throw new Exception("Members not avilable to process..");
            }
            
            // List of account ids
            memberDetails.add(accountIdToProcess);
            
            // Member organizations ids
            memberDetails.add(organizationIdMap);
            
            return memberDetails;
        } catch (Exception e) {
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, resultset);
        }
    }
/**
 * 
 * @param accountId
 * @return
 * @throws Exception
 */
    private static Boolean isValidAccountToProcess(String  accountId) throws Exception {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        String status = null; 
        Boolean isValidAccount = true;
        String sessionStatusQuery = MigrationUtil.getResource("CALENDAR_EVENT");
        try {
            // Retrieving account program based on account id and master program
            preparedStatement = connection.prepareStatement(sessionStatusQuery);
            preparedStatement.setString(1, accountId);
            logger.info("Get session status : "+sessionStatusQuery);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                status = rs.getString("STATUS");
            }
            if(null != status && (status.equalsIgnoreCase("Scheduled") || status.equalsIgnoreCase("Rescheduled"))) {
                isValidAccount = false;
                logger.debug("Skiping account Id : " + accountId +" Customization Session status "+ status);
            }
        } catch (Exception e) {
            logger.error("Exception while getting validAccountId, Skiping account Id : " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, rs);
        }
        return isValidAccount;
    }
    
    /**
     * 
     * @param accountIdList
     * @return
     * @throws Exception
     * Retrieving account program based on account id and master program
     */
    private static Map<String, String> getAccountProgram(String accountId,String masterProgramId) throws Exception {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        String accountProgramId = null, programEndDate = null, programEndDateTime = null, startDate = null, startDateTime = null;
        Map<String, String> accountProgramMap = new HashMap<String, String>();
        String accountProgramQuery = MigrationUtil.getResource("GET_ACCOUNT_PROGRAM");
        try {
            // Retrieving account program based on account id and master program
            preparedStatement = connection.prepareStatement(accountProgramQuery);
            preparedStatement.setString(1, accountId);
            preparedStatement.setString(2, masterProgramId);
            logger.info("Get account program : "+accountProgramQuery);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                accountProgramId = rs.getString("ID");
                programEndDate = rs.getString("END_DT");
                programEndDateTime = rs.getString("END_DT_TIME");
                startDate = rs.getString("START_DT");
                startDateTime = rs.getString("START_DT_TIME");
            }
            accountProgramMap.put("ACCOUNT_PROGRAM_ID", accountProgramId);
            accountProgramMap.put("END_DATE", programEndDate);
            accountProgramMap.put("END_DATE_TIME", programEndDateTime);
            accountProgramMap.put("START_DT", startDate);
            accountProgramMap.put("START_DT_TIME", startDateTime);
            logger.info("Old account program id : "+accountProgramId);
        } catch (Exception e) {
            logger.error("Exception while getting account program, Skiping account Id : " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, rs);
        }
        return accountProgramMap;
    }

    /**
     * 
     * @param accountIdList
     * @throws Exception
     */
    private static void updateAccountProgram(String accountId, Map<String, String> accountProgramDetailsMap) throws Exception {
        PreparedStatement preparedStatement = null;
        String accountProgramId = null;
        String updateAccountProgramQuery = MigrationUtil.getResource("UPDATE_ACCOUNT_PROGRAM");
        try {
            // Get account program id : accountProgramDetails map having old customization details against account id
            accountProgramId = accountProgramDetailsMap.get("ACCOUNT_PROGRAM_ID");
            if (null != accountProgramId && accountProgramId.trim().length() > 0) {
                preparedStatement = connection.prepareStatement(updateAccountProgramQuery);
                logger.info("Update account program query : "+updateAccountProgramQuery);
                preparedStatement.setString(1, accountProgramId);
                preparedStatement.executeUpdate();
            } 
            else {
                throw new Exception("Valid account program details not found for (account id) : " + accountId);
            }
        } catch (Exception e) {
            logger.error("Exception while updateAccountProgram, Skiping account Id : " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, null);
        }
    }

    /**
     * 
     * @param accountIdList
     * @throws Exception
     */
    private static void deleteNonOnboardingProgram(String accountId) throws Exception {
        String deleteNonOnboardingProgram = MigrationUtil.getResource("DELETE_NON_ONBOARDING_ACT_PRO");
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(deleteNonOnboardingProgram);
            logger.info("Delete NonOnboarding Program query : "+deleteNonOnboardingProgram);
            preparedStatement.setString(1, accountId);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            logger.error("Exception while delete non onboarding program, Skipping account id: " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, null);
        }
    }
 /**
  * 
  * @param accountId
  * @throws Exception
  */
    private static void deleteNonOnboardingAccountPrmMembership(String accountId) throws Exception {
        String deleteNonOnboardingProgram = MigrationUtil.getResource("DELETE_ACCOUNT_PROGRAM_MEMBERSHIP");
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(deleteNonOnboardingProgram);
            logger.info("Delete NonOnboarding Program query : "+deleteNonOnboardingProgram);
            preparedStatement.setString(1, accountId);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            logger.error("Exception while delete deleteNonOnboardingAccountPrmMembership, Skipping account id: " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, null);
        }
    }
    /**
     * 
     * @return
     * @throws Exception
     * Get new program details
     */
    private static Map<String, String> getProgramDetails(String programId) throws Exception {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Map<String, String> newProgramMap = new HashMap<String, String>();        
        String newProgramQuery = MigrationUtil.getResource("GET_PROGRAM");
        try {
            preparedStatement = connection.prepareStatement(newProgramQuery);
            logger.info("Get program details query : "+newProgramQuery);
            preparedStatement.setString(1, programId);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                newProgramMap.put("ID", rs.getString("ID"));
                newProgramMap.put("TERM_DURATION_UNITS", rs.getString("TERM_DURATION_UNITS"));
                newProgramMap.put("TERM_DURATION_LENGTH", rs.getString("TERM_DURATION_LENGTH"));
                newProgramMap.put("NAME", rs.getString("NAME"));
            }
        } catch (Exception e) {
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, rs);
        }
        return newProgramMap;
    }

    /**
     * 
     * @param accountIdList
     * @param programDetails
     * @throws Exception
     */
    private static void createNewAccountProgram(String accountId, Map<String, String> accountProgramMap,
        Map<String, String> newCustProgram) throws Exception {
        PreparedStatement preparedStatement = null;
        Map<String, Date> combinedData = null;
        String newAccountProgram = MigrationUtil.getResource("CREATE_NEW_ACCOUNT_PROGRAM");
        logger.info("Create new account program : "+newAccountProgram);
        try {
            combinedData = MigrationUtil.getCombinedData(accountId, accountProgramMap, newCustProgram);
            // Creating new account program
            preparedStatement = connection.prepareStatement(newAccountProgram);
            preparedStatement.setString(1, accountId);
            preparedStatement.setString(2, newCustProgram.get("ID"));
            preparedStatement.setString(3, null); // LAST_COMPLETED_EVENT_ID
            preparedStatement.setInt(4, 0); // DISCOUNT_AMOUNT
            preparedStatement.setDate(5, new java.sql.Date(combinedData.get("END_DT").getTime())); // END_DT :startdate+ duration
            preparedStatement.setString(6, "0"); // RATING
            preparedStatement.setDate(7, new java.sql.Date(combinedData.get("START_DT").getTime())); // START_DT : startdate
            preparedStatement.setDate(8, new java.sql.Date(combinedData.get("END_DT_TIME").getTime())); // END_DT_TIME:startdatetime+duration
            preparedStatement.setString(9, "In Progress"); // Status
            preparedStatement.setDate(10, new java.sql.Date(combinedData.get("START_DT_TIME").getTime())); // START_DT_TIME :startdate
            preparedStatement.setString(11, null); // LAST_COMPLETED_SESSION_DT_TIME
            preparedStatement.setString(12, null); // CANCEL_EFFECTIVE_DT
            preparedStatement.setString(13, null); // CANCEL_EFFECTIVE_DT_TIME
            preparedStatement.setString(14, null); // CANCELLATION_REASON
            preparedStatement.setString(15, "1"); // IS_ACTIVE
            preparedStatement.setString(16, "0"); // IS_EXPIRED
            preparedStatement.setString(17, "1"); // APPROVED
            preparedStatement.setString(18, "0"); // COMPLIMENTARY
            preparedStatement.setString(19, "0"); // PM_SCHEDULE_EDITED
            preparedStatement.setString(20, newCustProgram.get("NAME")); // NAME
            preparedStatement.setDate(21, new java.sql.Date(new Date().getTime())); // CREATED_DT
            preparedStatement.setString(22, "9"); // CREATED_BY_ID
            preparedStatement.setDate(23, new java.sql.Date(new Date().getTime())); // LAST_MODIFIED_DT
            preparedStatement.setString(24, null); // LAST_MODIFIED_BY_ID
            preparedStatement.setString(25, "0"); // IS_DELETED
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            logger.error("Exception while createNewAccountProgram, Skipping account Id : " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, null);
        }
    }

    /**
     * 
     * @param accountId
     * @return
     * @throws Exception
     */
    private static Map<String, String> getAccountProgramMembership(String accountId) throws Exception {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Map<String, String> actProMembershipMap = new HashMap<String, String>();
        try {
            String oldProgram = MigrationUtil.getResource("MAST_OLD_PROGRAM_ID");
            String actProMemQuery = MigrationUtil.getResource("GET_ACCOUNT_PRO_MEMBERSHIP");
            logger.info("Get account program membership query : "+actProMemQuery);
            preparedStatement = connection.prepareStatement(actProMemQuery);
            preparedStatement.setString(1, accountId);
            preparedStatement.setString(2, oldProgram);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                actProMembershipMap.put("ID", rs.getString("ID"));
                actProMembershipMap.put("START_DT", rs.getString("START_DT"));
                actProMembershipMap.put("START_DT_TIME", rs.getString("START_DT_TIME"));
                actProMembershipMap.put("REG_CODE", rs.getString("REG_CODE"));
                actProMembershipMap.put("ONBOARD_STARTED_ON", rs.getString("ONBOARD_STARTED_ON"));
                actProMembershipMap.put("ONBOARD_HISTORY", rs.getString("ONBOARD_HISTORY"));                
            }
        } catch (Exception e) {
            logger.error("Exception while  getAccountProgramMembership, skipping account id" + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, rs);
        }
        return actProMembershipMap;
    }

    /**
     * 
     * @param accountIdList
     * @throws Exception
     */
    private static void updateAccountProgramMembership(String accountId,
        Map<String, String> accountProgramMembeshipDetails) throws Exception {
        PreparedStatement preparedStatement = null;
        String accountProgramMebershipId = null;
        String updateAccountProgramQuery = MigrationUtil.getResource("UPDATE_ACCOUNT_PRO_MEMBERSHIP");
        logger.info("Update account program membership query: "+updateAccountProgramQuery);
        try {
            // Get account program id : accountProgramDetails map having old customization details against account id
            accountProgramMebershipId = accountProgramMembeshipDetails.get("ID");
            if (null != accountProgramMebershipId && accountProgramMebershipId.trim().length() > 0) {
                preparedStatement = connection.prepareStatement(updateAccountProgramQuery);
                preparedStatement.setString(1, accountProgramMebershipId);
                preparedStatement.executeUpdate();
            } else {
                throw new Exception("Valid account program membership details not found for (account id) : " + accountId);
            }
        } catch (Exception e) {
            logger.error("Exception while updateAccountProgramMembership, skipping account id " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, null);
        }
    }

    /**
     * 
     * @param accountId
     * @param accountProgramMap
     * @param newCustProgram
     * @throws Exception
     */
    private static void createNewAccountProgramMembership(String accountId, Map<String, String> accountProgramMap,
        Map<String, String> newCustProgram,Map<String,String> orgMap) throws Exception {
        Map<String, Date> combinedData = null;
        PreparedStatement preparedStatement = null;
        String accountProgramMebershipQuery = MigrationUtil.getResource("CREATE_ACCOUNT_PROGRAM_MEMBERSHIP");
        logger.info("Create new account program membership query : "+accountProgramMebershipQuery);
        try {
            String masterProgramId = MigrationUtil.getResource("MAST_NEW_PROGRAM_ID");
            String onboardingHistory = MigrationUtil.getOnboardingHistory(accountProgramMap.get("ONBOARD_HISTORY"));
            Map<String, String> pcubeMap = getPcubeAndRegCode(accountId, orgMap);
            combinedData = MigrationUtil.getCombinedData(accountId, accountProgramMap, newCustProgram);
            Date onboard_started_on =  MigrationUtil.dateFromString( accountProgramMap.get("ONBOARD_STARTED_ON"), "yyyy-MM-dd");
            preparedStatement = connection.prepareStatement(accountProgramMebershipQuery);
            preparedStatement.setString(1, accountId); // ACCOUNT_ID - accountid
            preparedStatement.setString(2, null); // PRIMARY_DISCOUNT_ID
            preparedStatement.setString(3, null); // SECONDARY_DISCOUNT_ID
            preparedStatement.setString(4, pcubeMap.get("PCUBE_ID")); // PCUBE_ID- pcube id from new masterprogram
            preparedStatement.setString(5, pcubeMap.get("MAST_ASMT_QUEST_ID")); // MAST_ASMT_QUEST_ID
            preparedStatement.setString(6, masterProgramId); // MAST_PROGRAM_ID- masterprogram id
            preparedStatement.setString(7, pcubeMap.get("REG_CODE_ORDER_ID")); // REG_CODE_ORDER_ID
            preparedStatement.setString(8, "Active"); // ACTIVE - 1
            preparedStatement.setDate(9, new java.sql.Date(combinedData.get("END_DT").getTime())); // END_DT-startdt+duration
            preparedStatement.setString(10, null); // MBRSHIP_PACKAGE
            preparedStatement.setString(11, null); // MBRSHIP_TYPE
            preparedStatement.setDate(12, new java.sql.Date(onboard_started_on.getTime())); // ONBOARD_STARTED_ON
            preparedStatement.setString(13, null); // PROMOTION_CODE
            preparedStatement.setString(14, accountProgramMap.get("REG_CODE")); // REG_CODE -reg code from new master program
            preparedStatement.setDate(15, new java.sql.Date(combinedData.get("START_DT").getTime())); // START_DT-startdt
            preparedStatement.setString(16, "0"); // TOTAL_BENEFIT_REMAINING 0
            preparedStatement.setDate(17, new java.sql.Date(combinedData.get("END_DT_TIME").getTime())); // END_DT_TIME -startdttime+duration
            preparedStatement.setString(18, null); // MEMBER_TIME_ZONE_KEY -null
            preparedStatement.setDate(19, new java.sql.Date(combinedData.get("START_DT_TIME").getTime())); // START_DT_TIME-startdatetime
            preparedStatement.setString(20, "0"); // IS_EXPIRED - 0
            preparedStatement.setString(21, "1"); // IS_ACTIVE - 1
            preparedStatement.setString(22, onboardingHistory); // ONBOARD_HISTORY
            preparedStatement.setString(23, pcubeMap.get("ONBOARDING_STEP_ID")); // ACCOUNT_ONBOARDING_STEP_ID- ?
            preparedStatement.setString(24, null); // USER_ONBOARDING_STEP_ID
            preparedStatement.setString(25, null); // NAME - null
            preparedStatement.setDate(26, new java.sql.Date(new Date().getTime())); // CREATED_DT-currentdate
            preparedStatement.setString(27, null); // CREATED_BY_ID
            preparedStatement.setDate(28, new java.sql.Date(new Date().getTime())); // LAST_MODIFIED_DT -currentdate
            preparedStatement.setString(29, null); // LAST_MODIFIED_BY_ID - null
            preparedStatement.setString(30, "0"); // IS_DELETED - 0
            preparedStatement.setString(31, null); // ALERT_SETTING_ID -null
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            logger.error("Exception while createNewAccountProgramMembership, Skipping account Id : " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, null);
        }
    }

    /**
     * 
     * @param accountId
     * @param accountProgramDetailsMap
     * @throws Exception
     */
    private static void updateAccountProviderAssigned(String accountId, Map<String, String> accountProgramDetailsMap) throws Exception {
        PreparedStatement preparedStatement = null;
        String accountProgramId = null;
        String oldMasterProgramId = MigrationUtil.getResource("MAST_OLD_PROGRAM_ID");
        String updateAccountProgramQuery = MigrationUtil.getResource("DELETE_ACCOUNT_PROVIDER_ASSIGNED");
        try {
            // Get account program id : accountProgramDetails map having old customization details against account id
            accountProgramId = accountProgramDetailsMap.get("ACCOUNT_PROGRAM_ID");
            if (null != accountProgramId && accountProgramId.trim().length() > 0) {
                logger.info("Update account provider assigned query : "+updateAccountProgramQuery);
                preparedStatement = connection.prepareStatement(updateAccountProgramQuery);
                preparedStatement.setString(1, accountProgramId);
                preparedStatement.setString(2, oldMasterProgramId);
                preparedStatement.executeUpdate();
            } else {
                throw new Exception("Valid account program details not found for (account id) : " + accountId);
            }
        } catch (Exception e) {
            logger.error("Exception while updateAccountProviderAssigned, Skiping account Id : " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, null);
        }
    }

    /**
     * 
     * @param accountId
     * @param newCustProgramMap
     * @param accountProgramMap
     * @throws Exception
     */
    private static void updateSummaryAccountTodate(String accountId, Map<String, String> newCustProgramMap, Map<String, String> accountProgramMap) throws Exception {
        PreparedStatement preparedStatement = null;
        String accountProgramId = null;
        String updateAccountProgramQuery = MigrationUtil.getResource("UPDATE_SUMMARY_ACCOUNT_TODATE");
        try {
            Date programStDate = MigrationUtil.dateFromString(accountProgramMap.get("START_DT"), "yyyy-MM-dd HH:mm:ss.S");
            // Get account program id : accountProgramDetails map having old customization details against account id
            String masterProgram = MigrationUtil.getResource("MAST_NEW_PROGRAM_ID");;
            Map<String, String> newAccountProgramMap = getAccountProgram(accountId,masterProgram);
            accountProgramId = newAccountProgramMap.get("ACCOUNT_PROGRAM_ID");
            if (MigrationUtil.isValid(accountProgramId)) {
                logger.info("Update summery account to date query : "+updateAccountProgramQuery);
                preparedStatement = connection.prepareStatement(updateAccountProgramQuery);
                preparedStatement.setString(1, newCustProgramMap.get("ID"));
                preparedStatement.setString(2, newCustProgramMap.get("NAME"));
                preparedStatement.setString(3, null);
                preparedStatement.setString(4, null);
                preparedStatement.setString(5, accountProgramId);
                preparedStatement.setDate(6, new java.sql.Date(programStDate.getTime()));
                preparedStatement.setString(7, accountId);
                preparedStatement.executeUpdate();
            } else {
                throw new Exception("Valid account program details not found for (account id) : " + accountId);
            }
        } catch (Exception e) {
            logger.error("Exception while updateSummaryAccountTodate, Skiping account Id : " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, null);
        }
    }

    /**
     * 
     * @param accountId
     * @param accountProgramMap
     * @throws Exception
     */
    private static void createAccountProgramActivity(String accountId, Map<String, String> accountProgramMap,Map<String,String> summaryAccountTodate) throws Exception {
        PreparedStatement preparedStatement = null;
        String accountProgramMebershipQuery = MigrationUtil.getResource("CREATE_ACCOUNT_PROGRAM_ACTIVITY");
        try {
            logger.info("Create account program activity query : "+accountProgramMebershipQuery);
            String accountProgramId = accountProgramMap.get("ACCOUNT_PROGRAM_ID");
            String onboardingStatus = summaryAccountTodate.get("ONBOARDING_STATUS");
            String programIntervalNumber = summaryAccountTodate.get("PROGRAM_INTERVAL_NUMBER");
            String accountPrmStartDate = summaryAccountTodate.get("ACCOUNT_PROGRAM_START_DT");
            String activityData = "{\"ACCOUNT_ID\"=\"" + accountId + "\",\"ACCOUNT_PROGRAM_ID\":\"" + accountProgramId+ "\",\"ONBOARDING_STATUS\":\"" + onboardingStatus+ "\",\"PROGRAM_INTERVAL_NUMBER\":\"" + programIntervalNumber+ "\",\"ACCOUNT_PROGRAM_START_DT\":\"" + accountPrmStartDate+ "\"}";
            preparedStatement = connection.prepareStatement(accountProgramMebershipQuery);
            preparedStatement.setString(1, null); // PROGRAM_INTERVAL_ID - null
            preparedStatement.setString(2, accountProgramId); // ACCOUNT_PROGRAM_ID-accountProgramId
            preparedStatement.setString(3, null); // DESCRIPTION - null
            preparedStatement.setString(4, null); // PARENT_ID - null
            preparedStatement.setDate(5, new java.sql.Date(new Date().getTime())); // ACTIVITY_DT -sysdate
            preparedStatement.setString(6, "Migration"); // ACTIVITY_TYPE -Migration
            preparedStatement.setString(7, "Customization Migration"); // ACTIVITY_NAME-Customization Migration
            preparedStatement.setString(8, activityData); // ACTIVITY_DATA -{"ACCOUNT_ID"="","ACCOUNT_PROGRAM_ID"=""}
            preparedStatement.setString(9, null); // CONTENT_TYPE_ID- null
            preparedStatement.setString(10, null); // NAME - null
            preparedStatement.setDate(11, new java.sql.Date(new Date().getTime())); // CREATED_DT -sysdate
            preparedStatement.setString(12, "09"); // CREATED_BY_ID - 09
            preparedStatement.setDate(13, new java.sql.Date(new Date().getTime())); // LAST_MODIFIED_DT- sysdate
            preparedStatement.setString(14, null);// LAST_MODIFIED_BY_ID - null
            preparedStatement.setString(15, "0"); // IS_DELETED - 0
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            logger.error("Exception while createAccountProgramActivity, Skipping account Id : " + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, null);
        }
    }

    /**
     * 
     * @param accountId
     * @param orgMap
     * @return
     * @throws Exception
     */
    private static Map<String, String> getPcubeAndRegCode(String accountId, Map<String, String> orgMap) throws Exception {
        String orgId = orgMap.get(accountId);
        String regCodeOrderId = null;
        Map<String, String> pcubeDetails = new HashMap<String, String>();
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Map<String,String> pcubeId = getPcubeId(orgId);
        if (pcubeId.size() == 0) {
           pcubeId = getPcubeId(MigrationUtil.getResource("ORG_ID"));
        }
        
        pcubeDetails.put("PCUBE_ID", pcubeId.get("PCUBEID"));
        pcubeDetails.put("MAST_ASMT_QUEST_ID", pcubeId.get("MAST_ASMT_QUEST_ID"));
        String pcubeOrderQuery  = MigrationUtil.getResource("GET_PCUBE_REG_CODE_ORDER");
        try {
            logger.info("Get Pcube And RegCode query : "+pcubeOrderQuery);
            preparedStatement = connection.prepareStatement(pcubeOrderQuery);
            preparedStatement.setString(1, pcubeId.get("PCUBEID"));
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                regCodeOrderId = rs.getString("ID");
            }
            pcubeDetails.put("REG_CODE_ORDER_ID", regCodeOrderId);
            pcubeDetails.put("ONBOARDING_STEP_ID", getOnboadingStepId());
        } catch (Exception e) {
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, rs);
        }
        return pcubeDetails;
    }

    /**
     * 
     * @param orgId
     * @return
     * @throws Exception
     */
    private static Map<String,String> getPcubeId(String orgId) throws Exception {
        Map<String,String> pcubeDetails = new HashMap<String,String>();
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        String pcubeQuery = MigrationUtil.getResource("GET_PCUBE_DETAILS");
        String mastProgramId = MigrationUtil.getResource("MAST_NEW_PROGRAM_ID");
        try {
            logger.info("Get Pcube Id query : "+pcubeQuery);
            preparedStatement = connection.prepareStatement(pcubeQuery);
            preparedStatement.setString(1, mastProgramId);
            preparedStatement.setString(2, orgId);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                pcubeDetails.put("PCUBEID",  rs.getString("ID"));
                pcubeDetails.put("MAST_ASMT_QUEST_ID",rs.getString("MAST_ASMT_QUEST_ID"));
            }
        } catch (Exception e) {
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, rs);
        }
        return pcubeDetails;
    }
    
    /**
     * 
     * @return
     * @throws Exception
     */
    private static String getOnboadingStepId() throws Exception {
        String onboardingStepId = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        String onboardingStepIdQuery = MigrationUtil.getResource("GET_ONBOARDING_STEP_ID");
        try {
            logger.info("Get onboarding step id  query:"+onboardingStepIdQuery);
            preparedStatement = connection.prepareStatement(onboardingStepIdQuery);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                onboardingStepId = rs.getString("ID");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, rs);
        }
        return onboardingStepId;
    }
    
    /**
     * 
     * @param accountId
     * @return
     * @throws Exception
     */
    private static Map<String, String> getSummaryAccountTodate(String accountId) throws Exception {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Map<String, String> summaryAccountTodateMap = new HashMap<String, String>();
        try {
            String oldProgram = MigrationUtil.getResource("MAST_OLD_PROGRAM_ID");
            String actProMemQuery = MigrationUtil.getResource("GET_SUMMARY_ACCOUNT_TODATE");
            logger.info("Get account program membership query : "+actProMemQuery);
            preparedStatement = connection.prepareStatement(actProMemQuery);
            preparedStatement.setString(1, accountId);
            preparedStatement.setString(2, oldProgram);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                summaryAccountTodateMap.put("ONBOARDING_STATUS", rs.getString("ONBOARDING_STATUS"));
                summaryAccountTodateMap.put("PROGRAM_INTERVAL_NUMBER", rs.getString("PROGRAM_INTERVAL_NUMBER"));
                summaryAccountTodateMap.put("ACCOUNT_PROGRAM_START_DT", rs.getString("ACCOUNT_PROGRAM_START_DT"));
                            
            }
        } catch (Exception e) {
            logger.error("Exception while  getSummaryAccountTodate, skipping account id" + accountId);
            throw e;
        } finally {
            MigrationUtil.closeStatementAndResultset(preparedStatement, rs);
        }
        return summaryAccountTodateMap;
    }

}