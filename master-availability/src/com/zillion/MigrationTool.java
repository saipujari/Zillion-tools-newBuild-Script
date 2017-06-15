/**
 * 
 */
package com.zillion;

import java.util.HashMap;
import java.util.Map;

import com.zillion.api.MasterAvailabilityReportConstants;
import com.zillion.migration.MigrationConstants;
import com.zillion.migration.MigrationHelper;

/**
 * @author arunkumar.d
 * 
 */
public class MigrationTool {

    public static void main(String[] commandLineArgs) {

        String onboardingStatus ="MEMBER ONBOARDING";
        Map<String, String> inputParams = new HashMap<String, String>();
        //inputParams.put(MigrationConstants.COUNT, "1");
         inputParams.put(MigrationConstants.MEMBERS_IDS, "425F179AE3AF6605E0530100007F38DE");
        inputParams.put(MigrationConstants.ONBOARDING_STATUS, onboardingStatus);
        inputParams.put(MasterAvailabilityReportConstants.RESOURCEBUNDLEPATH,
            "E:\\Zillion\\GroupPE\\migration\\masteravailabilitytool\\resources\\masteravilabilityresource.properties");
        try {
            MigrationHelper.migrateMembers(inputParams);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
