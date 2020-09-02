package com.sequoiacm.contentserver.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public void siteList(@RequestParam(value = "filter", required = false) BSONObject filter,
                         HttpServletResponse response)
            throws ScmServerException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = siteService.getSiteList(filter);
        ServiceUtils.putCursorToWriter(cursor, ServiceUtils.getWriter(response));
    }
}
