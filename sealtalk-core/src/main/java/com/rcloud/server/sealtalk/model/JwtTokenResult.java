package com.rcloud.server.sealtalk.model;

import io.rong.models.Result;

/**
 * @author Jianlu.Yu
 */
public class JwtTokenResult extends Result {

    private String token;


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return null;
    }
}
