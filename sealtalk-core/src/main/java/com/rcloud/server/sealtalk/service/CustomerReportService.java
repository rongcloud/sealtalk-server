package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.dao.CustomerReportMapper;
import com.rcloud.server.sealtalk.entity.CustomerReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerReportService {

    @Autowired
    private CustomerReportMapper mapper;

    public void saveSelective(CustomerReport report) {
        mapper.insertSelective(report);
    }

}
