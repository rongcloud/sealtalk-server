package com.rcloud.server.sealtalk.rongcloud.message;

import io.rong.messages.BaseMessage;
import io.rong.util.GsonUtil;

/**
 *
 */
public class DismissUltraGroupMessage extends BaseMessage {

    private static final String TYPE = "ST:UltraGrpNtf";

    private String operatorUserId;
    private String operatorUserNickName;
    private String operation;
    private String message;

    public String getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(String operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public String getOperatorUserNickName() {
        return operatorUserNickName;
    }

    public void setOperatorUserNickName(String operatorUserNickName) {
        this.operatorUserNickName = operatorUserNickName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return GsonUtil.toJson(this, DismissUltraGroupMessage.class);
    }
}
