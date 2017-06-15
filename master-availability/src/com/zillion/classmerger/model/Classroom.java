package com.zillion.classmerger.model;

import java.util.Date;

/**
 *
 * @author Ganesan
 */
public class Classroom {
    
    private String id;
    
    private String name;
    
    private Date startDt;

    private Date endDt;
    
    private Boolean isApproved;
    
    private String status;

    private Long classificationMask;
    
    private Integer availableSeats;
    
    private Integer assignedSeats;
    
    private String mastHealthProgramId;
    
    private String assignedCoachId;
    
    public Classroom() {
    	
    }

    public Classroom(String id) {
        this.id = id;
    }

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

	public Date getStartDt() {
		return startDt;
	}

	public void setStartDt(Date startDt) {
		this.startDt = startDt;
	}

	public Date getEndDt() {
		return endDt;
	}

	public void setEndDt(Date endDt) {
		this.endDt = endDt;
	}

	public Boolean getIsApproved() {
		return isApproved;
	}

	public void setIsApproved(Boolean isApproved) {
		this.isApproved = isApproved;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getClassificationMask() {
		return classificationMask;
	}

	public void setClassificationMask(Long classificationMask) {
		this.classificationMask = classificationMask;
	}

	public Integer getAvailableSeats() {
		return availableSeats;
	}

	public void setAvailableSeats(Integer availableSeats) {
		this.availableSeats = availableSeats;
	}

	public Integer getAssignedSeats() {
		return assignedSeats;
	}

	public void setAssignedSeats(Integer assignedSeats) {
		this.assignedSeats = assignedSeats;
	}

	public String getMastHealthProgramId() {
		return mastHealthProgramId;
	}

	public void setMastHealthProgramId(String mastHealthProgramId) {
		this.mastHealthProgramId = mastHealthProgramId;
	}

	public String getAssignedCoachId() {
		return assignedCoachId;
	}

	public void setAssignedCoachId(String assignedCoachId) {
		this.assignedCoachId = assignedCoachId;
	}

}