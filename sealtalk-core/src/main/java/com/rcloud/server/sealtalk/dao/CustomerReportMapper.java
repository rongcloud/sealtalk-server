package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.entity.CustomerReport;

public interface CustomerReportMapper {
    int insertSelective(CustomerReport record);
}
