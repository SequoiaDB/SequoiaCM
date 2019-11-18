package com.sequoiacm.cloud.adminserver.controller;

import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.cloud.adminserver.common.RestCommonDefine;
import com.sequoiacm.cloud.adminserver.common.RestUtils;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.service.StatisticsService;

@RestController
@RequestMapping("/api/v1")
public class StatisticsController {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    @Autowired
    private StatisticsService statisticsService;

    @PostMapping("/statistics/refresh")
    public void refresh(
            @RequestParam(RestCommonDefine.RestArg.STATISTICS_TYPE) Integer statisticsType,
            @RequestParam(RestCommonDefine.RestArg.WORKSPACE_NAME) String workspaceName)
            throws StatisticsException {
        statisticsService.refresh(statisticsType, workspaceName);
        logger.debug("refresh statistics success: type={},wsName={}", statisticsType,
                workspaceName);
    }

    @GetMapping("/statistics/traffic/file")
    public void listFileTraffic(
            @RequestParam(value = RestCommonDefine.RestArg.QUERY_FILTER, required = false) BSONObject filter,
            HttpServletResponse response) throws StatisticsException {
        logger.debug("list file traffic,{}:{}", RestCommonDefine.RestArg.QUERY_FILTER, filter);
        response.reset();
        response.setHeader(RestCommonDefine.CONTENT_TYPE, RestCommonDefine.APPLICATION_JSON_UTF8);
        MetaCursor cursor = statisticsService.getTrafficList(filter);
        RestUtils.putCursorToWriter(cursor, RestUtils.getWriter(response));
    }

    @GetMapping("/statistics/delta/file")
    public void listFileDelta(
            @RequestParam(value = RestCommonDefine.RestArg.QUERY_FILTER, required = false) BSONObject filter,
            HttpServletResponse response) throws StatisticsException {
        logger.debug("list file delta,{}:{}", RestCommonDefine.RestArg.QUERY_FILTER, filter);
        response.reset();
        response.setHeader(RestCommonDefine.CONTENT_TYPE, RestCommonDefine.APPLICATION_JSON_UTF8);
        MetaCursor cursor = statisticsService.getFileDeltaList(filter);
        RestUtils.putCursorToWriter(cursor, RestUtils.getWriter(response));
    }
}
