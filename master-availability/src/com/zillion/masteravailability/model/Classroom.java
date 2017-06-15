package com.zillion.masteravailability.model;


public class Classroom {

    private String id;    
    private String name;    
    private String startDt;
    private String endDt;    
    private String status;    
    private String mastHealthProgramId;
    private Boolean isApproved;
    
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
	public String getStartDt() {
		return startDt;
	}
	public void setStartDt(String startDt) {
		this.startDt = startDt;
	}
	public String getEndDt() {
		return endDt;
	}
	public void setEndDt(String endDt) {
		this.endDt = endDt;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getMastHealthProgramId() {
		return mastHealthProgramId;
	}
	public void setMastHealthProgramId(String mastHealthProgramId) {
		this.mastHealthProgramId = mastHealthProgramId;
	}
	public Boolean getIsApproved() {
		return isApproved;
	}
	public void setIsApproved(Boolean isApproved) {
		this.isApproved = isApproved;
	}
}
