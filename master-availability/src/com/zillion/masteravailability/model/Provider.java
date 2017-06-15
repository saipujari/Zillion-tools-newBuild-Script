package com.zillion.masteravailability.model;

public class Provider {

	private String id;
	private String name;
	private String schCommittedHrsWeekly;
	private String email;
	private String phone;
	private Boolean isApproved;
	private Boolean willDeliver;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSchCommittedHrsWeekly() {
		return schCommittedHrsWeekly;
	}
	public void setSchCommittedHrsWeekly(String schCommittedHrsWeekly) {
		this.schCommittedHrsWeekly = schCommittedHrsWeekly;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public Boolean getIsApproved() {
		return isApproved;
	}
	public void setIsApproved(Boolean isApproved) {
		this.isApproved = isApproved;
	}
	public Boolean getWillDeliver() {
		return willDeliver;
	}
	public void setWillDeliver(Boolean willDeliver) {
		this.willDeliver = willDeliver;
	}
	
	
	
}
