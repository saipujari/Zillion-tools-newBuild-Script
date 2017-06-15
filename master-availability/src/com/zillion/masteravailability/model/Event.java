package com.zillion.masteravailability.model;


public class Event {

	private String id;
	private String startDtTime;
	private String coachName;
	private String sessionStatus;
	private String eventType;
	private Boolean isGroupSession;
	private String endDtTime;
	private Boolean isApproved;
	private SessionType sessionTypeId;
	private Classroom classroomId;
	private String ownerId;
	private Boolean isRecurrence;
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getStartDtTime() {
		return startDtTime;
	}
	public void setStartDtTime(String startDtTime) {
		this.startDtTime = startDtTime;
	}
	public String getCoachName() {
		return coachName;
	}
	public void setCoachName(String coachName) {
		this.coachName = coachName;
	}
	public String getSessionStatus() {
		return sessionStatus;
	}
	public void setSessionStatus(String sessionStatus) {
		this.sessionStatus = sessionStatus;
	}
	public String getEventType() {
		return eventType;
	}
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
	public Boolean getIsGroupSession() {
		return isGroupSession;
	}
	public void setIsGroupSession(Boolean isGroupSession) {
		this.isGroupSession = isGroupSession;
	}
	public String getEndDtTime() {
		return endDtTime;
	}
	public void setEndDtTime(String endDtTime) {
		this.endDtTime = endDtTime;
	}
	public Boolean getIsApproved() {
		return isApproved;
	}
	public void setIsApproved(Boolean isApproved) {
		this.isApproved = isApproved;
	}
	public SessionType getSessionTypeId() {
		return sessionTypeId;
	}
	public void setSessionTypeId(SessionType sessionTypeId) {
		this.sessionTypeId = sessionTypeId;
	}
	public Classroom getClassroomId() {
		return classroomId;
	}
	public void setClassroomId(Classroom classroomId) {
		this.classroomId = classroomId;
	}
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	public Boolean getIsRecurrence() {
		return isRecurrence;
	}
	public void setIsRecurrence(Boolean isRecurrence) {
		this.isRecurrence = isRecurrence;
	}

	
	
	
}
