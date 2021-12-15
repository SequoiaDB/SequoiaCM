package com.sequoiacm.cloud.adminserver.controller;

import java.util.List;

import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsBreakpointFileRawData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.service.InternalStatisticsService;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsFileRawData;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;

@RestController
@RequestMapping("/internal/v1/statistics")
public class InternalStatisticsController {
    private static final Logger logger = LoggerFactory
            .getLogger(InternalStatisticsController.class);
    @Autowired
    private InternalStatisticsService service;

    @PostMapping(path = { "/raw_data/" + ScmStatisticsType.FILE_DOWNLOAD,
            "/raw_data/" + ScmStatisticsType.FILE_UPLOAD })
    public void reportFileRawData(@RequestBody List<ScmStatisticsFileRawData> rawDataList)
            throws StatisticsException {
        // 客户端启动的时候会发一起次空数据的上报以初始化自身的相关实例
        if (rawDataList == null || rawDataList.size() <= 0) {
            return;
        }
        logger.debug("received raw data: {}", rawDataList);
        service.reportFileRawData(rawDataList);
    }

    @PostMapping(path = "/raw_data/" + ScmStatisticsType.BREAKPOINT_FILE_UPLOAD)
    public void reportBreakpointFileRawData(
            @RequestBody List<ScmStatisticsBreakpointFileRawData> rawDataList)
            throws StatisticsException {
        if (rawDataList == null || rawDataList.size() <= 0) {
            return;
        }
        logger.debug("received raw data: {}", rawDataList);
        service.reportBreakpointFileRawData(rawDataList);
    }
}
