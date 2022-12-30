package com.sequoiacm.contentserver.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.service.ISiteService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.metasource.MetaCursor;

@RestController
@RequestMapping("/api/v1")
public class SiteController {

    private final ISiteService siteService;

    private static final Logger logger = LoggerFactory.getLogger(SiteController.class);

    @Autowired
    public SiteController(ISiteService siteService) {
        this.siteService = siteService;
    }

    @RequestMapping("/sites/{site_name}")
    public ResponseEntity site(@PathVariable("site_name") String siteName,
            HttpServletRequest request) throws ScmServerException {
        BSONObject site = siteService.getSite(siteName);
        Map<String, Object> result = new HashMap<>(1);
        result.put("site", site);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sites")
    public void siteList(
            @RequestParam(value = CommonDefine.RestArg.SITE_FILTER, required = false) BSONObject filter,
            @RequestParam(value = CommonDefine.RestArg.SITE_SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.SITE_LIMIT, required = false, defaultValue = "-1") long limit,
            HttpServletResponse response) throws ScmServerException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = siteService.getSiteList(filter, skip, limit);
        ServiceUtils.putCursorToWriter(cursor, ServiceUtils.getWriter(response));
    }

    @RequestMapping(value = "/sites", method = RequestMethod.HEAD)
    public ResponseEntity<String> countSite(
            @RequestParam(value = CommonDefine.RestArg.SITE_FILTER, required = false) BSONObject filter,
            HttpServletResponse response) throws ScmServerException {
        long count = siteService.countSite(filter);
        response.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
        return ResponseEntity.ok("");
    }

    @GetMapping(value = "/sites", params = "action=" + CommonDefine.RestArg.ACTION_GET_SITE_STRATEGY)
    public ResponseEntity siteStrategy() {
        StrategyType strategyType = ScmStrategyMgr.getInstance().strategyType();
        Map<String, Object> result = new HashMap<>(1);
        result.put(CommonDefine.RestArg.SITE_STRATEGY, strategyType.getName());
        return ResponseEntity.ok(result);
    }
}
