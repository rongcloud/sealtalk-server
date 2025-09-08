package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class PingController {

    @GetMapping(value = "/ping")
    public APIResult<Object> ping() {
        return APIResultWrap.ok();
    }



}
