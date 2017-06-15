package com.zillion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MigrateMNDWeight {
	static Connection mysqlConnection = null;
	static Connection oracleConnection = null;

	public static void main(String[] args) {
		System.out.println("Execution Starts");
		if(validateInput(args)) {
			mysqlConnection = getMysqlDBConnection(args[0]);
			oracleConnection = getOracleDBConnection(args[0]);
			
			try {
				Statement oracleStmt = oracleConnection.createStatement();
				Statement oracleStmt1 = oracleConnection.createStatement();
				Statement mysqlStmt = mysqlConnection.createStatement();
				Statement mysqlStmt1 = mysqlConnection.createStatement();
				
				String validicOrgId = "";
				String validicToken = "";
				
				//Fetching Validic settings 
				String sql = "SELECT PROP_KEY, PROP_VALUE FROM ORG_SETTING WHERE SUBSYSTEM='TRACKER' AND ORGANIZATION_ID='0301'";
				ResultSet rs = oracleStmt.executeQuery(sql);
				while(rs.next()) {
					if(rs.getString("PROP_KEY").equals("TRACKER_VALIDIC_ORG_ID")) {
						validicOrgId = rs.getString("PROP_VALUE");
					} else if(rs.getString("PROP_KEY").equals("TRACKER_ORGANIZATION_ACCESS_TOKEN")) {
						validicToken = rs.getString("PROP_VALUE");
					}
				}
				if (validicOrgId.equals("") || validicToken.equals("")) {
					throw new RuntimeException("Validic entries are missing in Org Setting Table");
				}
				
				//Fetching Orbera members
				sql = "SELECT ACCOUNT_ID, MAP_SYSTEM_ACCOUNT_CODE, aim.CREATED_DT, aim.IS_DELETED " +
						"FROM ACCOUNT_IDENTITY_MAP aim, ACCOUNT a, ORGANIZATION o " +
						"WHERE MAP_SYSTEM_NAME='MYNETDIARY' " +
						"AND aim.ACCOUNT_ID=a.ID AND a.ORGANIZATION_ID=o.ID " +
						"AND o.PARENT_ORGANIZATION_ID='0301'";
				String sql1 = "";
				rs = oracleStmt.executeQuery(sql);
				String strAccount = "";
				String accountId = null;
			    while(rs.next()) {
			    	strAccount = rs.getString("MAP_SYSTEM_ACCOUNT_CODE");
			    	accountId = rs.getString("ACCOUNT_ID");
			    	ObjectMapper mapper = new ObjectMapper();
		            JsonNode jsonAccount = mapper.readTree(strAccount);
		            System.out.println("ACCOUNT: " + accountId + ", UserID: " + jsonAccount.get("userId").asText());
		            ResultSet rs1;
		            if(!rs.getBoolean("IS_DELETED")) {
		            	//Creating Validic Account for each members
		            	String response = createValidicAccount(validicOrgId, validicToken);
		            	System.out.println("Response: " + response);
		            	JsonNode accountDetails = mapper.readTree(response);
			            sql = "INSERT INTO ACCOUNT_IDENTITY_MAP (ACCOUNT_ID, MAP_SYSTEM_NAME, MAP_SYSTEM_ACCOUNT_CODE, NAME, CREATED_DT) VALUES " +
				    			"('" + accountId + "', 'VALIDIC', '" + accountDetails.get("user").toString() + "', 'VALIDIC', " +
				    			"TO_TIMESTAMP('" + rs.getString("CREATED_DT") + "', 'YYYY-MM-DD HH24:MI:SS.FF6'))";
			            rs1 = oracleStmt1.executeQuery(sql);
			            
			            //Deactivating existing MND account
			            sql = "UPDATE ACCOUNT_IDENTITY_MAP SET IS_DELETED = 1 " +
				    			"WHERE ACCOUNT_ID='" + accountId + "' AND MAP_SYSTEM_NAME='MYNETDIARY'";
			            rs1 = oracleStmt1.executeQuery(sql);
		            }

		            //Fetching Measurement from MND
					sql = "SELECT measurementId, measurementDate, actualValue " +
							"FROM measuremententry WHERE userId='" + 
							jsonAccount.get("userId").asText()  + "' AND " +
							"measurementId = 40 ORDER BY measurementDate, measurementId";
					rs1 = mysqlStmt.executeQuery(sql);
				    while(rs1.next()) {
				    	
				    	//Verifying whether the entry was already created
				    	sql = "SELECT ACCOUNT_ID FROM ACCOUNT_TRK_MEASUREMENT " +
				    			"WHERE SOURCE='MND' AND ACCOUNT_ID='" + accountId + "' " +
				    			"AND TO_CHAR(DATE_LOGGED, 'yyyy-mm-dd')='" + rs1.getString("measurementDate") + "' " +
				    			"AND VALUE=" + convertGramsToPounds(rs1.getDouble("actualValue"));
				    	ResultSet rs2 = oracleStmt1.executeQuery(sql);
				    	if(rs2.next()) {
				    		System.out.println("Record already exist");
				    	} else {
				    		//Inserting weight
				    		double actualValue = rs1.getDouble("actualValue");
				    		String measurementDate = rs1.getString("measurementDate");
					    	sql = "INSERT INTO ACCOUNT_TRK_MEASUREMENT (MEASUREMENT_TYPE, DATE_LOGGED, SOURCE, SOURCE_NAME, LAST_UPDATED, " +
					    			"VALIDATED, IS_PRIMARY, IS_DELETED, ACCOUNT_ID, VALUE, MEASUREMENT_ID) VALUES (";
					    	sql += "'WEIGHT',";
					    	sql += "TO_DATE('" + measurementDate + "', 'YYYY-MM-DD'),";
					    	sql += "'MND',";
					    	sql += "'MND',";
					    	sql += "TO_DATE('" + measurementDate + "', 'YYYY-MM-DD'),";
					    	sql += "0,";
					    	sql += "1,";
					    	sql += "0,";
					    	sql += "'" + accountId + "',";
					    	sql += convertGramsToPounds(actualValue) + ",";
					    	sql += "(SELECT ID FROM TRACKER_MEASUREMENT WHERE MEASUREMENT_NAME='WEIGHT')";
					    	sql += ")";
					    	
					    	//Inserting BMI
					    	sql1 = "SELECT measurementId, measurementDate, actualValue " +
									"FROM measuremententry WHERE userId='" + 
									jsonAccount.get("userId").asText()  + "' AND " +
									"measurementId = 43 ORDER BY measurementDate, measurementId";
					    	ResultSet rs3 = mysqlStmt1.executeQuery(sql1);
					    	if(rs3.next()) {
					    		sql1 = "INSERT INTO ACCOUNT_TRK_MEASUREMENT (MEASUREMENT_TYPE, DATE_LOGGED, SOURCE, SOURCE_NAME, LAST_UPDATED, " +
						    			"VALIDATED, IS_PRIMARY, IS_DELETED, ACCOUNT_ID, VALUE, MEASUREMENT_ID) VALUES (";
					    		sql1 += "'WEIGHT',";
					    		sql1 += "TO_DATE('" + measurementDate + "', 'YYYY-MM-DD'),";
					    		sql1 += "'MND',";
					    		sql1 += "'MND',";
					    		sql1 += "TO_DATE('" + measurementDate + "', 'YYYY-MM-DD'),";
					    		sql1 += "0,";
					    		sql1 += "1,";
					    		sql1 += "0,";
					    		sql1 += "'" + accountId + "',";
						    	long inches = mmToFeetInches(rs3.getInt("actualValue"));
					    		double bmi = (convertGramsToPounds(actualValue) / (inches*inches)) * 703;
					    		sql1 += bmi + ",";
					    		sql1 += "(SELECT ID FROM TRACKER_MEASUREMENT WHERE MEASUREMENT_NAME='BMI')";
					    		sql1 += ")";
					    	}
					    	oracleStmt1.executeQuery(sql);
					    	oracleStmt1.executeQuery(sql1);
				    	}
				    }
			    }
			} catch (Exception e) {
				e.printStackTrace();
			}

			
			
		}
		System.out.println("Execution Ends");
	}
	
	private static String createValidicAccount(String validicOrgId, String validicToken) throws IOException {
		
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost postRequest = new HttpPost("https://api.validic.com/v1/organizations/" + validicOrgId + "/users.json");

		StringEntity input = new StringEntity("{\"user\": {\"uid\": \"" + UUID.randomUUID().toString() + "\"},\"access_token\": \"" + validicToken + "\"}");
		input.setContentType("application/json");
		postRequest.setEntity(input);

		HttpResponse response = httpClient.execute(postRequest);

		if (response.getStatusLine().getStatusCode() != 201) {
			throw new RuntimeException("Failed : HTTP error code : "
				+ response.getStatusLine().getStatusCode());
		}
		
		BufferedReader br = new BufferedReader(
                        new InputStreamReader((response.getEntity().getContent())));

		String output;
		String responseData = "";
		while ((output = br.readLine()) != null) {
			responseData += output;
		}

		httpClient.getConnectionManager().shutdown();
		return responseData;
	}
	
	private static double convertGramsToPounds(double weightInGrams) {
		return Math.round(weightInGrams * 0.00220462);
	}
	
	public static long mmToFeetInches(int mm) {
		double MMS_IN_INCH = 25.4;
		double MMS_IN_FOOT = MMS_IN_INCH * 12;
		long feet = Math.round(Math.floor(mm / MMS_IN_FOOT));
		long inches = Math.round((mm % MMS_IN_FOOT) / MMS_IN_INCH);
		return (feet * 12) + inches;
	}
	
	private static boolean validateInput(String[] args) {
		boolean val = true;
		if(args.length != 1) {
			val = false;
			System.out.println("Need Environment Name eg: CHOCO / CHOCO / TEST / UAT / PRD");
		}
		return val;
	}
	
	private static Connection getMysqlDBConnection(String env) {
		Connection dbConnection = null;
		try {
			ResourceBundle DB_PROP = ResourceBundle.getBundle("env", Locale.ENGLISH);
			String DB_DRIVER = "com.mysql.jdbc.Driver";
			String DB_HOST = "jdbc:mysql://" + DB_PROP.getString(env +"_MND_DB_HOST") + "/" + DB_PROP.getString(env +"_MND_DB_NAME") ;
			String DB_USER = DB_PROP.getString(env +"_MND_DB_USER");
			String DB_PASSWORD = DB_PROP.getString(env +"_MND_DB_PASSWORD");
			Class.forName(DB_DRIVER);
			dbConnection = DriverManager.getConnection(DB_HOST, DB_USER,DB_PASSWORD);
			return dbConnection;
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		return dbConnection;
	}
	
	private static Connection getOracleDBConnection(String env) {
		Connection dbConnection = null;		
		String DB_DRIVER = "oracle.jdbc.driver.OracleDriver";
		ResourceBundle DB_PROP = ResourceBundle.getBundle("env", Locale.ENGLISH);
		String DB_CONNECTION = DB_PROP.getString(env +"_ZIL_CONNECTION");
		String DB_USER = DB_PROP.getString(env +"_ZIL_DB_USER");
		String DB_PASSWORD = DB_PROP.getString(env +"_ZIL_DB_PASSWORD");
		try {
			Class.forName(DB_DRIVER);
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
		try {
			dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER,DB_PASSWORD);
			return dbConnection;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return dbConnection;
	}
}
