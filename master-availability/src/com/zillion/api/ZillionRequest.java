package com.zillion.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class ZillionRequest {

	private static String uniqueRequestID;
	private static String organizationID;
	private static String applicationName;
	private static String authorizedUserName;
	@SuppressWarnings("unused")
    private static String authorizedUserPassword;
	private static String authorizedUserID;
	private static String authorizedSignatureToken;
	private static String restMethod;
	private static String requestURL;
	private static String requestBody;
	private static Properties testProperties;
	private boolean showResponse = true;
	@SuppressWarnings("unused")
    private static String baseURL;
	private static String frontEndDomain;
	
	/**
	 * public constructor, sets values to "NOT-SET" 
	 */
	public ZillionRequest(){
		super();
		testProperties = MasterAvailabilityReportHelper.masterAvilabilityResource;
		baseURL = testProperties.getProperty("baseURL");
		frontEndDomain = testProperties.getProperty("frontEndDomain");
		setUniqueRequestID("NOT_SET");
		setOrganizationID("NOT_SET");
		setApplicationName("NOT_SET");
		setAuthorizedUserName("NOT_SET");
		setAuthorizedUserPassword("NOT_SET");
		setAuthorizedUserID("NOT_SET");
		setAuthorizedSignatureToken("NOT_SET");
		setRestMethod("NOT_SET");
		setRequestURL("NOT_SET");
		setRequestBody("NOT_SET");		
		
	}
	
	/**
	 * Sends the given request, formats the response into a HealthFleetResponse
	 */
	public ZillionResponse send() throws IOException{

        if(getUniqueRequestID().equals("NOT_SET") 		||
		   getOrganizationID().equals("NOT_SET") 		||
		   getApplicationName().equals("NOT_SET") 		||
		   getAuthorizedUserName().equals("NOT_SET") 	||
           getAuthorizedUserID().equals("NOT_SET")  	||
		   getRestMethod().equals("NOT_SET") 			||
		   getRequestURL().equals("NOT_SET") 			||
		   getAuthorizedSignatureToken().equals("NOT_SET")){
			throw new IOException("Must set all fields prior to sending.");
		} else {
			URL obj = new URL(getRequestURL());
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod(getRestMethod());
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("uniqueRequestID", getUniqueRequestID());
			con.setRequestProperty("organizationID", getOrganizationID());
			con.setRequestProperty("applicationName", getApplicationName());
			con.setRequestProperty("authorizedUserName", getAuthorizedUserName());
			con.setRequestProperty("authorizedUserID", getAuthorizedUserID());
			con.setRequestProperty("authorizedSignatureToken", getAuthorizedSignatureToken());
			con.setRequestProperty("X-FORWARDED-FOR", frontEndDomain);
			DateTime now = new DateTime(DateTimeZone.UTC); 
			con.setRequestProperty("TimeinUTC", now.toString());
			con.setDoOutput(true);
		
			if(getRestMethod().equals("PUT") || getRestMethod().equals("POST")){
				if(getRequestBody().equals("NOT_SET")){
					throw new IOException("Must set body field prior to sending a PUT request.");
				} else {	
					OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
					wr.write(getRequestBody());
					wr.flush();
				}
			}
			
		
			
			StringBuffer response;
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();		
			ZillionResponse hfResponse = ZillionUtil.parseResponse(response);			
			return hfResponse;
		}
	}
	

    /**
     * Generates the headers for a portal request,
     */
	public void generateRequest(String orgId,String applicationName,String authorizedUserName,String authorizedUserID,String authorizedSignatureToken){
		generateUniqueRequestID();
		setOrganizationID(orgId);
		setApplicationName(applicationName);
		setAuthorizedUserName(authorizedUserName);
		setAuthorizedUserID(authorizedUserID);
		setAuthorizedSignatureToken(authorizedSignatureToken);
	}
	/**
	 * generates a random number up to 50000 and sets it to the uniques request id
	 */
	public void generateUniqueRequestID(){
		this.setUniqueRequestID(Integer.toString((int)(50000 + Math.random()*50000)));
	}

	/**
	 * @return the uniqueRequestID
	 */
	public  String getUniqueRequestID() {
		return uniqueRequestID;
	}

	/**
	 * @param uniqueRequestID the uniqueRequestID to set
	 */
	public void setUniqueRequestID(String uniqueRequestID) {
		ZillionRequest.uniqueRequestID = uniqueRequestID;
	}

	/**
	 * @return the organizationID
	 */
	public String getOrganizationID() {
		return organizationID;
	}

	/**
	 * @param organizationID the organizationID to set
	 */
	public void setOrganizationID(String organizationID) {
		ZillionRequest.organizationID = organizationID;
	}

	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName(String applicationName) {
		ZillionRequest.applicationName = applicationName;
	}

	/**
	 * @return the authorizedUserName
	 */
	public String getAuthorizedUserName() {
		return authorizedUserName;
	}

	/**
	 * @param authorizedUserName the authorizedUserName to set
	 */
	public void setAuthorizedUserName(String authorizedUserName) {
		ZillionRequest.authorizedUserName = authorizedUserName;
	}
	
	/**
	 * @param authorizedUserPassword the authorizedUserPassword to set
	 */
	public void setAuthorizedUserPassword(String authorizedUserPassword) {
		ZillionRequest.authorizedUserPassword = authorizedUserPassword;
	}

	/**
	 * @return the authorizedUserID
	 */
	public String getAuthorizedUserID() {
		return authorizedUserID;
	}

	/**
	 * @param authorizedUserID the authorizedUserID to set
	 */
	public void setAuthorizedUserID(String authorizedUserID) {
		ZillionRequest.authorizedUserID = authorizedUserID;
	}

	/**
	 * @return the authorizedSignatureToken
	 */
	public String getAuthorizedSignatureToken() {
		return authorizedSignatureToken;
	}

	/**
	 * @param authorizedSignatureToken the authorizedSignatureToken to set
	 */
	public void setAuthorizedSignatureToken(String authorizedSignatureToken) {
		ZillionRequest.authorizedSignatureToken = authorizedSignatureToken;
	}

	/**
	 * @return the restMethod
	 */
	public String getRestMethod() {
		return restMethod;
	}

	/**
	 * @param restMethod the restMethod to set
	 */
	public void setRestMethod(String restMethod) {
		ZillionRequest.restMethod = restMethod;
	}

	/**
	 * @return the requestURL
	 */
	public String getRequestURL() {
		return requestURL;
	}

	/**
	 * @param requestURL the requestURL to set
	 */
	public void setRequestURL(String requestURL) {
		ZillionRequest.requestURL = requestURL;
	}

	/**
	 * @return the requestBody
	 */
	public String getRequestBody() {
		return requestBody;
	}

	/**
	 * @param requestBody the requestBody to set
	 */
	public void setRequestBody(String requestBody) {
		ZillionRequest.requestBody = requestBody;
	}

	/**
	 * @return the showResponse
	 */
	public boolean isShowResponse() {
		return showResponse;
	}

	/**
	 * @param showResponse the showResponse to set
	 */
	public void setShowResponse(boolean showResponse) {
		this.showResponse = showResponse;
	}
	
	
	
}
