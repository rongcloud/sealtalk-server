package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.dto.ReportParam;
import com.rcloud.server.sealtalk.entity.CustomerReport;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.service.CustomerReportService;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import com.rcloud.server.sealtalk.util.ValidateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
@Slf4j
public class CustomerController extends BaseController{


    @Autowired
    private CustomerReportService customerReportService;


    /**
     * 举报功能
     */
    @RequestMapping("/report")
    public APIResult<Object> report( @RequestBody ReportParam param) throws Exception {

        ValidateUtils.notBlank(param.getTargetId(), "targetId");
        ValidateUtils.notBlank(param.getLevelF(), "levelF");
        ValidateUtils.notNull(param.getChannelType(), "channelType");
        ValidateUtils.notNull(param.getPics(), "pics");


        String userId = getServerApiParams().getCurrentUserIdStr();
        CustomerReport report = new CustomerReport();
        report.setReportId(userId);
        report.setTargetId(param.getTargetId());
        report.setChannelType(param.getChannelType());
        report.setReportLevelFirst(param.getLevelF());
        report.setReportLevelSecond(param.getLevelS());
        report.setPic(JacksonUtil.toJson(param.getPics()));
        report.setContent(param.getContent());
        customerReportService.saveSelective(report);
        return APIResultWrap.ok();
    }


}
