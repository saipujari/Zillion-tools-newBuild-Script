package com.zillion.classmerger.model;

import java.io.Serializable;

public class AccountRequest implements Serializable {

    private static final long serialVersionUID = -2911955710320781764L;
    
    public String id;
    
    public AccountRequest(){
        
    }
    
    public AccountRequest(String id){
        this.id=id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
}