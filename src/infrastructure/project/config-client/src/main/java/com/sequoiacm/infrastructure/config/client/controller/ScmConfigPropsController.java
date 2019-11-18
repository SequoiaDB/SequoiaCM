package com.sequoiacm.infrastructure.config.client.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.infrastructure.config.client.service.ScmConfigPropService;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@RestController("innerConfPropsController")
@RequestMapping("/internal/v1")
public class ScmConfigPropsController {

    @Autowired
    private ScmConfigPropService service;

    @PutMapping("/config-props")
    public void updateConfigProps(
            @RequestParam(ScmRestArgDefine.CONF_PROPS_UPDATE_PROPERTIES) String updatePropsStr,
            @RequestParam(ScmRestArgDefine.CONF_PROPS_DELETE_PROPERTIES) String deletePropsStr,
            @RequestParam(ScmRestArgDefine.CONF_PROPS_ACCEPT_UNKNOWN_PROPS) boolean acceptUnknownProps)
            throws ScmConfigException {
        HashMap<String, String> updatePropsMap = new HashMap<>();
        BSONObject updatePropsBson = (BSONObject) JSON.parse(updatePropsStr);
        for (String key : updatePropsBson.keySet()) {
            updatePropsMap.put(key, updatePropsBson.get(key).toString());
        }


        List<String> deletePropsList = new ArrayList<>();
        BasicBSONList deletePropsBson = (BasicBSONList) JSON.parse(deletePropsStr);
        for (Object deleteProp : deletePropsBson) {
            deletePropsList.add(deleteProp.toString());
        }

        service.updateConfigProps(updatePropsMap, deletePropsList, acceptUnknownProps);
    }
}
