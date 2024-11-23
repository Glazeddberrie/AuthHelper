package com.glazeddberrie.authhelper.Model;

public class Password {
    private String serviceName;
    private String userAssignedName;
    public Password(){
        // Required empty constructor for Firebase
    }
    public Password(String serviceName, String userAssignedName) {
        this.serviceName = serviceName;
        this.userAssignedName = userAssignedName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getUserAssignedName() {
        return userAssignedName;
    }
}
