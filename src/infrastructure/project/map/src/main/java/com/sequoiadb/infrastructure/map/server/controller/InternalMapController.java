package com.sequoiadb.infrastructure.map.server.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.infrastructure.metasource.MetaCursor;
import com.sequoiadb.infrastructure.map.CommonDefine;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.server.ServerUtils;
import com.sequoiadb.infrastructure.map.server.config.CommonServerConfig;
import com.sequoiadb.infrastructure.map.server.service.IMapService;

@RestController
@RequestMapping("/internal/v1")
public class InternalMapController {
    private final IMapService mapService;

    @Autowired
    private CommonServerConfig serverConfig;

    @Autowired
    public InternalMapController(IMapService mapService) {
        this.mapService = mapService;
    }

    @PostMapping(value = "/map/{map_name}", params = "action=createMap")
    public BSONObject createMap(
            @RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_KEY_TYPE) String keyType,
            @RequestParam(CommonDefine.RestArg.MAP_VALUE_TYPE) String valueType,
            HttpServletRequest request) throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        ServerUtils.checkKeyType(keyType);
        return mapService.createMap(mapGroupName, mapName, keyType, valueType);
    }

    @GetMapping(value = "/map/{map_name}", params = "action=getMap")
    public BSONObject getMap(
            @RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName, HttpServletRequest request)
            throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        return mapService.getMap(mapGroupName, mapName);
    }

    @DeleteMapping(value = "/map/{map_name}", params = "action=deleteMap")
    public void deleteMap(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName, HttpServletRequest request)
            throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        mapService.deleteMap(mapGroupName, mapName);
    }

    @PostMapping(value = "/map/{map_name}", params = "action=put")
    public BSONObject put(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_ENTRY) BSONObject entry,
            HttpServletRequest request) throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        BSONObject checkedEntry = checkGetEntry(entry);
        return mapService.put(mapGroupName, mapName, checkedEntry);
    }

    @PostMapping(value = "/map/{map_name}", params = "action=putAll")
    public void putAll(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_ENTRY_LIST) String entrys,
            HttpServletRequest request) throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        List<BSONObject> entryList;
        try {
            entryList = (List<BSONObject>) JSON.parse(entrys);
            if (entryList.size() == 0) {
                return;
            }
        }
        catch (Exception e) {
            throw new ScmMapServerException(ScmMapError.INVALID_ARGUMENT,
                    "entrys type error: entryList=" + entrys);
        }
        List<BSONObject> checkedEntryList = new ArrayList<>();
        for (BSONObject entry : entryList) {
            checkedEntryList.add(checkGetEntry(entry));
        }
        mapService.putAll(mapGroupName, mapName, checkedEntryList);
    }

    @GetMapping(value = "/map/{map_name}", params = "action=count")
    public long count(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(name = CommonDefine.RestArg.MAP_FILTER, required = false) BSONObject filter,
            HttpServletRequest request) throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        return mapService.count(mapGroupName, mapName, filter);
    }

    @GetMapping(value = "/map/{map_name}", params = "action=get")
    public BSONObject get(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_KEY) BSONObject key, HttpServletRequest request)
            throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        BSONObject checkedKey = checkGetKey(key);
        return mapService.get(mapGroupName, mapName, checkedKey);
    }

    @GetMapping(value = "/map/{map_name}", params = "action=list")
    public void listEntry(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(name = CommonDefine.RestArg.MAP_FILTER, required = false) BSONObject condition,
            @RequestParam(name = CommonDefine.RestArg.MAP_ORDERBY, required = false) BSONObject orderby,
            @RequestParam(name = CommonDefine.RestArg.MAP_SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(name = CommonDefine.RestArg.MAP_LIMIT, required = false, defaultValue = "-1") long limit,
            HttpServletRequest request, HttpServletResponse response) throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        MetaCursor cursor = mapService.list(mapGroupName, mapName, condition, null, orderby, skip,
                limit);
        ServerUtils.putCursorToWriter(cursor, ServerUtils.getWriter(response),
                serverConfig.getListInstanceCheckInterval());
    }

    @GetMapping(value = "/map/{map_name}", params = "action=listKey")
    public void listKey(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(name = CommonDefine.RestArg.MAP_FILTER, required = false) BSONObject condition,
            @RequestParam(name = CommonDefine.RestArg.MAP_ORDERBY, required = false) BSONObject orderby,
            @RequestParam(name = CommonDefine.RestArg.MAP_SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(name = CommonDefine.RestArg.MAP_LIMIT, required = false, defaultValue = "-1") long limit,
            HttpServletRequest request, HttpServletResponse response) throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        BSONObject selector = new BasicBSONObject(CommonDefine.FieldName.KEY, null);
        MetaCursor cursor = mapService.list(mapGroupName, mapName, condition, selector, orderby,
                skip, limit);
        ServerUtils.putCursorToWriter(cursor, ServerUtils.getWriter(response),
                serverConfig.getListInstanceCheckInterval());
    }

    @DeleteMapping(value = "/map/{map_name}", params = "action=remove")
    public BSONObject remove(
            @RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_KEY) BSONObject key, HttpServletRequest request)
            throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        BSONObject checkedKey = checkGetKey(key);
        return mapService.remove(mapGroupName, mapName, checkedKey);
    }

    @DeleteMapping(value = "/map/{map_name}", params = "action=removeAll")
    public boolean removeAll(
            @RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String mapGroupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(name = CommonDefine.RestArg.MAP_FILTER, required = false) BSONObject filter,
            HttpServletRequest request) throws ScmMapServerException {
        ServerUtils.checkMapName(mapName);
        return mapService.removeAll(mapGroupName, mapName, filter);
    }

    private BSONObject checkGetEntry(BSONObject entry) throws ScmMapServerException {
        if (entry != null) {
            if (entry.containsField(CommonDefine.FieldName.KEY)
                    && entry.containsField(CommonDefine.FieldName.VALUE)) {
                Object key = entry.get(CommonDefine.FieldName.KEY);
                Object value = entry.get(CommonDefine.FieldName.VALUE);
                BSONObject bson = new BasicBSONObject();
                bson.put(CommonDefine.FieldName.KEY, key);
                bson.put(CommonDefine.FieldName.VALUE, value);
                return bson;
            }
        }
        throw new ScmMapServerException(ScmMapError.INVALID_ARGUMENT,
                "missing required: entry=" + entry);

    }

    private BSONObject checkGetKey(BSONObject keyBson) throws ScmMapServerException {
        if (keyBson != null && keyBson.containsField(CommonDefine.FieldName.KEY)) {
            Object key = keyBson.get(CommonDefine.FieldName.KEY);
            BSONObject bson = new BasicBSONObject();
            bson.put(CommonDefine.FieldName.KEY, key);
            return bson;
        }
        throw new ScmMapServerException(ScmMapError.INVALID_ARGUMENT,
                "missing required: key=" + keyBson);

    }

    private BSONObject checkGetValue(BSONObject valueBson) throws ScmMapServerException {
        if (valueBson != null && valueBson.containsField(CommonDefine.FieldName.VALUE)) {
            Object value = valueBson.get(CommonDefine.FieldName.VALUE);
            BSONObject bson = new BasicBSONObject();
            bson.put(CommonDefine.FieldName.VALUE, value);
            return bson;
        }
        throw new ScmMapServerException(ScmMapError.INVALID_ARGUMENT,
                "missing required : value=" + valueBson);
    }
}
