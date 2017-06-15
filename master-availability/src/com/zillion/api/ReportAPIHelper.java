package com.zillion.api;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zillion.masteravailability.model.Events;
import com.zillion.masteravailability.model.Provider;

public class ReportAPIHelper {
	
	public static String organizationId = "02";
	public static Properties masterAvilabilityResource = MasterAvailabilityReportHelper.masterAvilabilityResource;
	public static final String         baseURL         = masterAvilabilityResource.getProperty("baseURL");
	public static String SIGNATURE_TOKEN = null;
	public static String USERID = null;
	public static String USERNAME = null;
	public static String DOMAIN = masterAvilabilityResource.getProperty("frontEndDomain");
	
	static {
	    disableSslVerification();
	}
	
	/**
	 *  	
	 * @param providerId
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static Events getProviderEvents(String providerId,String date,String timeZone) throws ParseException, IOException{
		DateTime eventAPIStart = new DateTime();
		Events events = null;
		Date startDate = ZillionUtil.getDateInUTC(ZillionUtil.setStartDateTime(date) ,timeZone,MasterAvailabilityReportConstants.DATE_FORMAT_2);
		Date endDate = ZillionUtil.getDateInUTC(ZillionUtil.setEndDateTime(date),timeZone,MasterAvailabilityReportConstants.DATE_FORMAT_2);
		System.out.println(baseURL + " " + startDate  + " " + endDate);
		String rangeStart = ZillionUtil.getDateAsString(startDate, "yyyy-MM-dd'T'HH:mm:ss.SSS") + "Z";
		String rangeEnd = ZillionUtil.getDateAsString(endDate, "yyyy-MM-dd'T'HH:mm:ss.SSS") + "Z";
		String url = baseURL + "/provider/v1/provider/"+ providerId + "/events?"  + "eventTypes=Available,Unavailable,Session" + "&rangeStart=" + rangeStart + "&rangeEnd=" + rangeEnd;
		ZillionRequest request = new ZillionRequest();
		request.generateRequest(organizationId,"PROGRAM ADMIN PORTAL",USERNAME,USERID,SIGNATURE_TOKEN);
		request.setRestMethod("GET");
		request.setRequestURL(url);
	    request.setAuthorizedSignatureToken(SIGNATURE_TOKEN);
	    request.setAuthorizedUserID(USERID);
		ZillionResponse zillionResponse = request.send();
		System.out.println("Api Response : "+zillionResponse.toString());
		if(zillionResponse.getResponseStatus().equals("OK") && zillionResponse.getResponseCode().equals("0")){
			if(null != zillionResponse.getResponseData() && zillionResponse.getResponseData().length() > 0){
				String response = "{\"event\":"+zillionResponse.getResponseData()+"}";
				//System.out.println("Events for "+response);
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
				events =  mapper.readValue(response, Events.class);				 
			}
		}
		DateTime eventAPIEnd = new DateTime();
		MasterAvailabilityReportHelper.timeTaken = MasterAvailabilityReportHelper.timeTaken +(eventAPIEnd.getMillis()-eventAPIStart.getMillis());
		System.out.println("Time taken to get events for" + providerId + " : "+(eventAPIEnd.getMillis()-eventAPIStart.getMillis()) +"ms");
		return events;
	}
	
	/**
	 *  	
	 * @param providerMap
	 * @param date
	 * @param timeZone
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	public static Map<String,Events> getAllProvidersEventsByDate(Map<String,Provider> providerMap,String date,String timeZone) throws ParseException, IOException{
		Map<String,Events> allProvidersEvents = new HashMap<String,Events>();
		Events events = null;
		for(String providerId : providerMap.keySet()){
			System.out.println("Getting events for : "+providerMap.get(providerId).getName() +"-"+providerMap.get(providerId).getEmail() + " Date "+date);
			events = getProviderEvents(providerId,date,timeZone);
			if(null != events && events.getEvent().size() > 0){
				allProvidersEvents.put(providerId, events);
			}
		}
		return allProvidersEvents;
	}
	
	 /**
	   * 
	   * @param userName
	   * @param password
	   * @return
	   * @throws Exception 
	   */
	public static String doLogin(String userName, String password) throws Exception{
		System.out.println("Login Started for : "+ userName);

	    String msg="01"+organizationId+"LOGIN"+userName+password;
		String token = generateSignatureToken("LOGIN", msg);
	    String encryptPassword = EncryptPassword(password);
		ZillionResponse zillionResponse = authenticateUser(userName, encryptPassword, token);
		if(zillionResponse.getResponseStatus().equals("OK") && zillionResponse.getResponseCode().equals("0")){
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(zillionResponse.getResponseData());
			JsonNode userId = node.get("userId");
		      JsonNode userRolesList = node.get("userRolesList");
		      JsonNode firstUserRole = userRolesList.get(0);
		      JsonNode applicationsList = firstUserRole.get("applicationsList");
    	      JsonNode application = applicationsList.get(0);
		      String message = "01" + "02" + application.asText() + firstUserRole.get("roleName").asText() + userId.asText();
		      token = generateSignatureToken(application.asText(), message);
		      ZillionResponse zillionResponse1 = authenticateApplication( application.asText(), userId.asText(), token );
		      if(zillionResponse1.getResponseStatus().equals("OK") && zillionResponse1.getResponseCode().equals("0")){
		    	  System.out.println(userName + "Logged in Successfully ");
		    	  SIGNATURE_TOKEN = token;
		    	  USERID = node.get("userId").asText();
		    	  USERNAME = userName;
		      }
		      else{
		    	  throw new Exception("Authentication Failed");
		      }
		}
		else{
			System.out.println(zillionResponse.getResponseCode() + " " + zillionResponse.getResponseData());
			  throw new Exception("Authentication Failed");
		}
		return zillionResponse.toString();
	}
	  
	
	public static String generateSignatureToken(String applicationName, String message){

        try {
  	
            String url = "https://"+DOMAIN+"/api/generateSignatureToken";
            ZillionRequest request = new ZillionRequest();
            request.setRequestURL(url);
            
            request.setRestMethod("POST");

            request.setApplicationName("");
            request.setAuthorizedSignatureToken("");
            request.setAuthorizedUserID("");
            request.setAuthorizedUserName("");
            request.setOrganizationID("");
            request.setUniqueRequestID("01");
            request.setRequestBody("{\"organizationId\":\""+organizationId+"\",\"applicationName\":\""+applicationName+"\", \"message\":\""+message+"\"}");
            ZillionResponse hfResponse = request.send();
            return hfResponse.getResponseData().toString();
         

        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
	/**
	 * 	
	 * @param username
	 * @param password
	 * @param token
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	 public static ZillionResponse authenticateUser(String username, String password, String token) throws IOException, JSONException {
		String url = baseURL + "/auth/v2/authenticateuser";
		ZillionRequest request = new ZillionRequest();
		request.setUniqueRequestID("01");
		request.setOrganizationID(organizationId);
		request.setApplicationName("LOGIN");
		request.setAuthorizedUserID("");
		request.setAuthorizedUserName("");
		request.setAuthorizedSignatureToken(token);
		request.setRestMethod("POST");
		request.setRequestURL(url);
		String date = new DateTime(DateTimeZone.UTC).toString();
		request.setRequestBody("{\"domain\":\""+DOMAIN+"\",\"username\":\""+username+"\",\"password\":\""+password+"\",\"timeOfRequestInUTC\":\"" + date + "\"}");
		return request.send();
	 }
	 /**
	  * 
	  * @param application
	  * @param userId
	  * @param token
	  * @return
	  * @throws IOException
	  * @throws JSONException
	  */
	 public static ZillionResponse authenticateApplication(String application, String userId, String token) throws IOException, JSONException {
	     String url1 = baseURL + "/auth/v2/authenticateapplication";
	     ZillionRequest request = new ZillionRequest();
	     request.setUniqueRequestID("01");
	     request.setOrganizationID(organizationId);
	     request.setApplicationName(application);
	     request.setAuthorizedUserID(userId);
	     request.setAuthorizedUserName("");
	     request.setAuthorizedSignatureToken(token);
	     request.setRestMethod("POST");
	     request.setRequestURL(url1);
	     String date = new DateTime(DateTimeZone.UTC).toString();
	     request.setRequestBody("{\"timeOfRequestInUTC\":\"" + date + "\"}");
	     return request.send();
	 }
	 
	 public static void generateMoveMembersRequest(String targetClassroomId, String requestBody) throws IOException{
		 String url = baseURL + "/provider/v1/classroom/" + targetClassroomId + "/members/move";
		 ZillionRequest request = new ZillionRequest();
		 request.generateRequest(organizationId,"PROGRAM ADMIN PORTAL",USERNAME,USERID,SIGNATURE_TOKEN);
		 request.setRestMethod("POST");
		 request.setRequestURL(url);
		 request.setAuthorizedSignatureToken(SIGNATURE_TOKEN);
		 request.setAuthorizedUserID(USERID);
		 request.setRequestBody(requestBody);
		 ZillionResponse zillionResponse = request.send();
		 System.out.println("Api Response : "+zillionResponse.toString());
		 if(zillionResponse.getResponseStatus().equals("OK") && zillionResponse.getResponseCode().equals("0")){
			 System.out.println("Merge Class Operation Succesfully Completed");
		 }
		 else{
			 System.out.println("Merge Class Operation Failed");
		 }
	 }
	 
	 
	 public static String EncryptPassword(String password){
			String passwordMasterKey = "vJ2n0V02rK5ougcd6e4rigjr1Hf7p2u0mDZXfJ05SWLpfoKZL1tARbnobr4oEEXjzVYoDb67VbnOrEn1g7XfZW47tvJJJroH2N2qUR0W8rAwyXN6uNZHfewXTZ3xYEZnzKiKQRPw8kUtQyuVjdbSOCz0ASR7rag62GFdhdr9jrlQf7ut8s3tnyh5FQUmgvbh5ZH8OxRMoB7Fzwh0ivTcly10L94z2fGKzlLWpMH/XmGefNxHnkA=";


		    byte[] keys = passwordMasterKey.getBytes();
		    byte[] msg = password.getBytes();
		    int ml = msg.length;
		    int kl = keys.length;
		    byte[] newMsg = new byte[ml];

		    for(int i = 0; i< ml; i++){
		        newMsg[i] = (byte) (msg[i]^keys[i%kl]);
		    }

		    return  DatatypeConverter.printBase64Binary(newMsg);
   }
	 
	 
	 private static void disableSslVerification() {
		    try
		    {
		        // Create a trust manager that does not validate certificate chains
		        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
		            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		                return null;
		            }
		            public void checkClientTrusted(X509Certificate[] certs, String authType) {
		            }
		            public void checkServerTrusted(X509Certificate[] certs, String authType) {
		            }
		        }
		        };

		        // Install the all-trusting trust manager
		        SSLContext sc = SSLContext.getInstance("SSL");
		        sc.init(null, trustAllCerts, new java.security.SecureRandom());
		        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		        // Create all-trusting host name verifier
		        HostnameVerifier allHostsValid = new HostnameVerifier() {
		            public boolean verify(String hostname, SSLSession session) {
		                return true;
		            }
		        };

		        // Install the all-trusting host verifier
		        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		    } catch (NoSuchAlgorithmException e) {
		        e.printStackTrace();
		    } catch (KeyManagementException e) {
		        e.printStackTrace();
		    }
		}
		 
}