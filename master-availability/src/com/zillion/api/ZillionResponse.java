package com.zillion.api;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class ZillionResponse implements Serializable{
	private static final long serialVersionUID = 1L;
	private String responseStatus;
	private String responseMessage;
	private String responseCode;
	private String uniqueRequestId;
	private String responseData;
	private JSONObject responseDataObject;
	
	private JSONObject input;

	/**
	 * initializes a healthFleetResponse object from a jsonObject representing
	 * the response, for testing
	 * @param input
	 * @throws JSONException
	 */
	public ZillionResponse(JSONObject input) throws JSONException {
		this.input = input;
		this.responseStatus = input.get("ResponseStatus").toString();
		this.responseMessage = input.get("ResponseMessage").toString();
		this.responseCode = input.get("ResponseCode").toString();
		try{
			 this.uniqueRequestId = input.get("UniqueRequestID").toString();
			}catch(Exception e){
				this.uniqueRequestId ="";
			}
		this.responseData = input.get("ResponseData").toString();
		if((responseData.startsWith("{") || responseData.startsWith("[")) &&
		   (responseData.endsWith("}")   || responseData.endsWith("]"))){
			this.responseDataObject = ZillionUtil.parseData(input.get("ResponseData").toString());
		} else {
			this.responseDataObject = new JSONObject();
		}
	}

	/**
	 * @return the responseStatus
	 */
	public String getResponseStatus() {
		return responseStatus;
	}

	/**
	 * @param responseStatus the responseStatus to set
	 */
	public void setResponseStatus(String responseStatus) {
		this.responseStatus = responseStatus;
	}

	/**
	 * @return the responseMessage
	 */
	public String getResponseMessage() {
		return responseMessage;
	}

	/**
	 * @param responseMessage the responseMessage to set
	 */
	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}

	/**
	 * @return the responseCode
	 */
	public String getResponseCode() {
		return responseCode;
	}

	/**
	 * @param responseCode the responseCode to set
	 */
	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	/**
	 * @return the uniqueRequestId
	 */
	public String getUniqueRequestId() {
		return uniqueRequestId;
	}

	/**
	 * @param uniqueRequestId the uniqueRequestId to set
	 */
	public void setUniqueRequestId(String uniqueRequestId) {
		this.uniqueRequestId = uniqueRequestId;
	}

	/**
	 * @return the responseData
	 */
	public String getResponseData() {
		return responseData;
	}

	/**
	 * @param responseData the responseData to set
	 */
	public void setResponseData(String responseData) {
		this.responseData = responseData;
	}

	/**
	 * @return the responseDataObject
	 */
	public JSONObject getResponseDataObject() {
		return responseDataObject;
	}

	/**
	 * @param responseDataObject the responseDataObject to set
	 */
	public void setResponseDataObject(JSONObject responseDataObject) {
		this.responseDataObject = responseDataObject;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	public String toString() {
		return getInput().toString();
	}

	public JSONObject getInput() {
		return input;
	}
}