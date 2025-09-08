package com.rcloud.server.sealtalk.constant;

public enum  MessageType {
    GROUP_NOTIFICATION("ST:GrpNtf"),
    CON_NOTIFICATION("ST:ConNtf");




    private String objectName;

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    MessageType(String objectName) {

        this.objectName = objectName;
    }

}
