package com.sequoiacm.contentserver.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.service.ISystemService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.metasource.MetaCursor;

@RestController
@RequestMapping("/api/v1")
public class SystemController {
    @Autowired
    private ISystemService systemService;

    @PostMapping("/reload-bizconf")
    public ResponseEntity reloadBizconf(
            @RequestParam(CommonDefine.RestArg.BIZ_RELOAD_SCOPE) Integer scope,
            @RequestParam(value = CommonDefine.RestArg.BIZ_RELOAD_ID, required = false) Integer id,
            @RequestParam(value = CommonDefine.RestArg.BIZ_RELOAD_METADATA_ONLY, required = false, defaultValue = "false") Boolean isMetadataOnly)
            throws ScmServerException {
        if(scope == CommonDefine.NodeScope.SCM_NODESCOPE_ALL) {
            return ResponseEntity.ok(systemService.reloadAllNodeBizConf(isMetadataOnly));
        }

        if(id == null) {
            throw new ScmInvalidArgumentException("invalid arg, id is null:scope=" + scope + ",id=" + id);
        }

        if(scope == CommonDefine.NodeScope.SCM_NODESCOPE_CENTER) {
            return ResponseEntity.ok(systemService.reloadSiteBizConf(id, isMetadataOnly));
        }

        if(scope == CommonDefine.NodeScope.SCM_NODESCOPE_NODE) {
            return ResponseEntity.ok(systemService.reloadNodeBizConf(id, isMetadataOnly));
        }

        throw new ScmInvalidArgumentException("invalid arg, unknown scope:scope=" + scope);
    }

    @GetMapping("/conf-properties")
    public ResponseEntity getConfProperties(@RequestParam(CommonDefine.RestArg.GET_PROP_KEYS) String[] keys)
            throws ScmServerException {
        BSONObject confs = systemService.getConfs(keys);
        Map<String, BSONObject> result = new HashMap<>(1);
        result.put(CommonDefine.RestArg.GET_PROP_RESP_CONF, confs);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/conf-properties/{key:.+}")
    public ResponseEntity getConfProperty(@PathVariable(CommonDefine.RestArg.GET_PROP_KEY) String key)
            throws ScmServerException {
        String[] keys = new String[]{key};
        BSONObject confs = systemService.getConfs(keys);
        return ResponseEntity.ok(confs);
    }

    @GetMapping("/nodes")
    public void getNodeList(@RequestParam(value = "filter", required = false) BSONObject filter,
                            HttpServletResponse response)
            throws ScmServerException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = systemService.getNodeList(filter);
        ServiceUtils.putCursorToWriter(cursor, ServiceUtils.getWriter(response));
    }

}
