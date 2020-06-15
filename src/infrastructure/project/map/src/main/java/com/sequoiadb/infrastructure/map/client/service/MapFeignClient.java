package com.sequoiadb.infrastructure.map.client.service;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiadb.infrastructure.map.CommonDefine;
import com.sequoiadb.infrastructure.map.ScmMapServerException;

import feign.Response;

public interface MapFeignClient {
    @PostMapping(value = "/internal/v1/map/{map_name}?action=createMap")
    public BSONObject createMap(
            @RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_KEY_TYPE) String keyType,
            @RequestParam(CommonDefine.RestArg.MAP_VALUE_TYPE) String valueType)
            throws ScmMapServerException;

    @GetMapping(value = "/internal/v1/map/{map_name}?action=getMap")
    public BSONObject getMap(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName)
            throws ScmMapServerException;

    @DeleteMapping(value = "/internal/v1/map/{map_name}?action=deleteMap")
    public void deleteMap(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName)
            throws ScmMapServerException;

    @GetMapping(value = "/internal/v1/map/{map_name}?action=count")
    public long count(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_FILTER) BSONObject filter)
            throws ScmMapServerException;

    @PostMapping(value = "/internal/v1/map/{map_name}?action=put")
    public BSONObject putMapEntry(
            @RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_ENTRY) BSONObject entry)
            throws ScmMapServerException;

    @PostMapping(value = "/internal/v1/map/{map_name}?action=putAll")
    public void putEntryList(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_ENTRY_LIST) String entrys)
            throws ScmMapServerException;

    @GetMapping(value = "/internal/v1/map/{map_name}?action=get")
    public BSONObject getMapValue(
            @RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_KEY) BSONObject key)
            throws ScmMapServerException;

    @GetMapping(value = "/internal/v1/map/{map_name}?action=list")
    public Response listMapEntry(
            @RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(name = CommonDefine.RestArg.MAP_FILTER, required = false) BSONObject condition,
            @RequestParam(name = CommonDefine.RestArg.MAP_ORDERBY, required = false) BSONObject orderby,
            @RequestParam(name = CommonDefine.RestArg.MAP_SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(name = CommonDefine.RestArg.MAP_LIMIT, required = false, defaultValue = "-1") long limit)
            throws ScmMapServerException;

    @GetMapping(value = "/internal/v1/map/{map_name}?action=listKey")
    public Response listMapKey(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(name = CommonDefine.RestArg.MAP_FILTER, required = false) BSONObject condition,
            @RequestParam(name = CommonDefine.RestArg.MAP_ORDERBY, required = false) BSONObject orderby,
            @RequestParam(name = CommonDefine.RestArg.MAP_SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(name = CommonDefine.RestArg.MAP_LIMIT, required = false, defaultValue = "-1") long limit)
            throws ScmMapServerException;

    // @GetMapping(value = "/internal/v1/map/{map_name}?action=listKey")
    // public BasicBSONList
    // listMapKey(@PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
    // @RequestParam(name = CommonDefine.RestArg.MAP_FILTER, required = false)
    // BSONObject condition,
    // @RequestParam(name = CommonDefine.RestArg.MAP_ORDERBY, required = false)
    // BSONObject orderby,
    // @RequestParam(name = CommonDefine.RestArg.MAP_SKIP, required = false,
    // defaultValue = "0") long skip,
    // @RequestParam(name = CommonDefine.RestArg.MAP_LIMIT, required = false,
    // defaultValue = "-1") long limit)
    // throws ScmMapServerException;

    @DeleteMapping(value = "/internal/v1/map/{map_name}?action=remove")
    public BSONObject remove(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_KEY) BSONObject key)
            throws ScmMapServerException;

    @DeleteMapping(value = "/internal/v1/map/{map_name}?action=removeAll")
    public boolean removeAll(@RequestHeader(CommonDefine.RestArg.MAP_GROUP_NAME) String groupName,
            @PathVariable(CommonDefine.RestArg.MAP_NAME) String mapName,
            @RequestParam(CommonDefine.RestArg.MAP_FILTER) BSONObject filter)
            throws ScmMapServerException;

}
