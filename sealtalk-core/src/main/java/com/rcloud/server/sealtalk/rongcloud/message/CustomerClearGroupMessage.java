package com.rcloud.server.sealtalk.rongcloud.message;

import io.rong.messages.BaseMessage;
import io.rong.util.GsonUtil;

public class CustomerClearGroupMessage extends BaseMessage {
    private transient static final String TYPE = "ST:MsgClear";

    private String operatorUserId;
    private String operation;
    private String clearTime;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return GsonUtil.toJson(this, CustomerClearGroupMessage.class);
    }

    public String getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(String operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getClearTime() {
        return clearTime;
    }

    public void setClearTime(String clearTime) {
        this.clearTime = clearTime;
    }
}
