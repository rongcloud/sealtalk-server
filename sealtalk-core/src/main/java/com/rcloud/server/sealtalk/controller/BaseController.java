package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.interceptor.ServerApiParamHolder;
import com.rcloud.server.sealtalk.model.RequestUriInfo;
import com.rcloud.server.sealtalk.model.ServerApiParams;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseController {

    @Autowired
    protected SealtalkConfig sealtalkConfig;

    protected Integer getCurrentUserId() {
        ServerApiParams serverApiParams = ServerApiParamHolder.get();
        if (serverApiParams != null) {
            return serverApiParams.getCurrentUserId();
        } else {
            return null;
        }
    }

    protected String getIp() {
        ServerApiParams serverApiParams = ServerApiParamHolder.get();
        if (serverApiParams != null) {
            RequestUriInfo requestUriInfo = serverApiParams.getRequestUriInfo();
            if(requestUriInfo!=null){
                return requestUriInfo.getIp();
            }
            return "";
        } else {
            return "";
        }
    }

    protected String getUri() {
        ServerApiParams serverApiParams = ServerApiParamHolder.get();
        if (serverApiParams != null) {
            RequestUriInfo requestUriInfo = serverApiParams.getRequestUriInfo();
            if(requestUriInfo!=null){
                return requestUriInfo.getUri();
            }
            return "";
        } else {
            return "";
        }
    }

    protected ServerApiParams getServerApiParams() {
        return ServerApiParamHolder.get();
    }

}
