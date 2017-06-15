package com.zillion.classmerger.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MoveMembersRequest implements Serializable {

    private static final long serialVersionUID = 1650083838426353257L;

    private List<AccountRequest> membersList = new ArrayList<AccountRequest>();
    
    private Boolean isOverride = false;
    
    public MoveMembersRequest(){
        super();
    }

    /**
     * @return the membersList
     */
    public List<AccountRequest> getMembersList() {
        return membersList;
    }

    /**
     * @param membersList the membersList to set
     */
    public void setMembersList(List<AccountRequest> membersList) {
        this.membersList = membersList;
    }

	/**
	 * @return the isOverride
	 */
	public Boolean getIsOverride() {
		return isOverride;
	}

	/**
	 * @param isOverride the isOverride to set
	 */
	public void setIsOverride(Boolean isOverride) {
		this.isOverride = isOverride;
	}
    
}