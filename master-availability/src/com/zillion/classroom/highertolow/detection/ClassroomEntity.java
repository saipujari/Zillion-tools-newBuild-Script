package com.zillion.classroom.highertolow.detection;

import java.util.Date;


public class ClassroomEntity {

	public String oldClassroomId;
	public String classroomId;
	public String accountProgramId;
	public Date oldClassroomDate;
	public Date newClassroomDate;
	public String oldClassroomIntervalNumber;
	public String newClassroomIntervalNumber;
	public Date assignedEndDate;
	public Date assignedStartDate;
	
	public ClassroomEntity(){
		
	}

	/**
	 * @return the classroomId
	 */
	public String getClassroomId() {
		return classroomId;
	}

	/**
	 * @param classroomId the classroomId to set
	 */
	public void setClassroomId(String classroomId) {
		this.classroomId = classroomId;
	}

	/**
	 * @return the accountProgramId
	 */
	public String getAccountProgramId() {
		return accountProgramId;
	}

	/**
	 * @param accountProgramId the accountProgramId to set
	 */
	public void setAccountProgramId(String accountProgramId) {
		this.accountProgramId = accountProgramId;
	}

	/**
	 * @return the oldClassroomDate
	 */
	public Date getOldClassroomDate() {
		return oldClassroomDate;
	}

	/**
	 * @param oldClassroomDate the oldClassroomDate to set
	 */
	public void setOldClassroomDate(Date oldClassroomDate) {
		this.oldClassroomDate = oldClassroomDate;
	}

	/**
	 * @return the newClassroomDate
	 */
	public Date getNewClassroomDate() {
		return newClassroomDate;
	}

	/**
	 * @param newClassroomDate the newClassroomDate to set
	 */
	public void setNewClassroomDate(Date newClassroomDate) {
		this.newClassroomDate = newClassroomDate;
	}

	/**
	 * @return the oldClassroomId
	 */
	public String getOldClassroomId() {
		return oldClassroomId;
	}

	/**
	 * @param oldClassroomId the oldClassroomId to set
	 */
	public void setOldClassroomId(String oldClassroomId) {
		this.oldClassroomId = oldClassroomId;
	}

	/**
	 * @return the oldClassroomIntervalNumber
	 */
	public String getOldClassroomIntervalNumber() {
		return oldClassroomIntervalNumber;
	}

	/**
	 * @param oldClassroomIntervalNumber the oldClassroomIntervalNumber to set
	 */
	public void setOldClassroomIntervalNumber(String oldClassroomIntervalNumber) {
		this.oldClassroomIntervalNumber = oldClassroomIntervalNumber;
	}

	/**
	 * @return the newClassroomIntervalNumber
	 */
	public String getNewClassroomIntervalNumber() {
		return newClassroomIntervalNumber;
	}

	/**
	 * @param newClassroomIntervalNumber the newClassroomIntervalNumber to set
	 */
	public void setNewClassroomIntervalNumber(String newClassroomIntervalNumber) {
		this.newClassroomIntervalNumber = newClassroomIntervalNumber;
	}

	/**
	 * @return the assignedEndDate
	 */
	public Date getAssignedEndDate() {
		return assignedEndDate;
	}

	/**
	 * @param assignedEndDate the assignedEndDate to set
	 */
	public void setAssignedEndDate(Date assignedEndDate) {
		this.assignedEndDate = assignedEndDate;
	}

	/**
	 * @return the assignedStartDate
	 */
	public Date getAssignedStartDate() {
		return assignedStartDate;
	}

	/**
	 * @param assignedStartDate the assignedStartDate to set
	 */
	public void setAssignedStartDate(Date assignedStartDate) {
		this.assignedStartDate = assignedStartDate;
	}
	
}