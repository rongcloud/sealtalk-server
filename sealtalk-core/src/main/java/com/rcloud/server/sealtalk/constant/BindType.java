package com.rcloud.server.sealtalk.constant;

public enum BindType {

    BOT(0),
    AI_USER(1);

    private final int type;

    BindType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
