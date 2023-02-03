package com.sequoiacm.contentserver.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.model.DataTableDeleteOption;
import com.sequoiacm.datasource.DatalocationFactory;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.DatasourceReaderDao;
import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataReader;
import com.sequoiacm.contentserver.service.IDatasourceService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;

@RestController
@RequestMapping("/internal/v1")
public class DatasourceController {

    private static final Logger logger = LoggerFactory.getLogger(DatasourceController.class);
    private final IDatasourceService datasourceService;

    @Autowired
    public DatasourceController(IDatasourceService service) {
        datasourceService = service;
    }

    @DeleteMapping("/datasource/{data_id}")
    public void deleteData(@PathVariable("data_id") String dataId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            @RequestParam(value = CommonDefine.RestArg.DATASOURCE_SITE_LIST_WS_VERSION, required = false, defaultValue = "1") Integer wsVersion,
            @RequestParam(value = CommonDefine.RestArg.DATASOURCE_SITE_LIST_TABLE_NAME, required = false) String tableName)
            throws ScmServerException {
        datasourceService.deleteDataLocal(wsName, ScmDataInfo.forOpenExistData(type, dataId,
                new Date(createTime), wsVersion, tableName));
    }

    @DeleteMapping(path = "/datasource/{data_id}", params = "action=delete_data_in_site_list")
    public void deleteDataInSiteList(@PathVariable("data_id") String dataId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST) List<Integer> siteList,
            @RequestBody(required = false) List<ScmFileLocation> siteLocationList)
            throws ScmServerException {
        List<ScmFileLocation> siteLocations;
        if (siteLocationList != null){
            siteLocations = siteLocationList;
        } else {
            siteLocations = new ArrayList<>();
            for (Integer siteId : siteList){
                // 在当前 delete 流程中， ScmFileLocation 的生命周期里没有使用到 lastAccessTime 的地方
                // 由于接口中没有传递 lastAccessTime，构造 ScmFileLocation 使用的是 crateTime 填充 lastAccessTime,
                siteLocations.add(new ScmFileLocation(siteId, createTime, createTime, 1, null));
            }
        }
        datasourceService.deleteDataInSiteList(wsName, dataId, type, createTime, siteLocations);
    }

    @GetMapping("/datasource/{data_id}")
    public void readData(@PathVariable("data_id") String dataId,
            @RequestParam(value = CommonDefine.RestArg.DATASOURCE_SITE_NAME, required = false) String targetSiteName,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            @RequestParam(CommonDefine.RestArg.FILE_READ_FLAG) int readFlag,
            @RequestParam(value = CommonDefine.RestArg.DATASOURCE_SITE_LIST_WS_VERSION, required = false, defaultValue = "1") Integer wsVersion,
            @RequestParam(value = CommonDefine.RestArg.DATASOURCE_SITE_LIST_TABLE_NAME, required = false) String tableName,
                         HttpServletResponse response) throws ScmServerException {
        response.setHeader("Content-Disposition", "attachment; filename=" + dataId);

        ServletOutputStream os = RestUtils.getOutputStream(response);
        if (targetSiteName == null || targetSiteName
                .equals(ScmContentModule.getInstance().getLocalSiteInfo().getName())) {
            DatasourceReaderDao dao = datasourceService.readData(wsName, dataId, type, createTime,
                    readFlag, os, wsVersion, tableName);
            try {
                response.setHeader(CommonDefine.RestArg.DATA_LENGTH, dao.getSize() + "");
                dao.read(os);
            }
            finally {
                dao.close();
            }
        }
        else {
            ScmContentModule contentModule = ScmContentModule.getInstance();
            ScmSite targetSiteInfo = contentModule.getSiteInfo(targetSiteName);
            if (null == targetSiteInfo) {
                throw new ScmServerException(ScmError.SERVER_NOT_EXIST,
                        "site is not exist:siteName=" + targetSiteName);
            }
            if (ScmStrategyMgr.getInstance().strategyType() == StrategyType.STAR
                    && !contentModule.isInMainSite() && !targetSiteInfo.isRootSite()) {
                throw new ScmServerException(ScmError.OPERATION_FORBIDDEN,
                        "Under the star strategy, cannot read other branchSite data at a branchSite"
                                + ":sourceSite=" + contentModule.getLocalSiteInfo().getName()
                                + ",targetSite=" + targetSiteName);
            }
            ScmInnerRemoteDataReader innerRemoteDataReader = null;
            try {
                innerRemoteDataReader = new ScmInnerRemoteDataReader(targetSiteInfo.getId(),
                        contentModule.getWorkspaceInfoCheckExist(wsName),
                        ScmDataInfo.forOpenExistData(type, dataId, new Date(createTime), wsVersion,
                                tableName),
                        readFlag);
                response.setHeader(CommonDefine.RestArg.DATA_LENGTH,
                        String.valueOf(innerRemoteDataReader.getExpectDataLen()));
                byte[] buf = new byte[Const.TRANSMISSION_LEN];
                while (true) {
                    int len = innerRemoteDataReader.read(buf, 0, Const.TRANSMISSION_LEN);
                    if (len <= -1) {
                        break;
                    }
                    os.write(buf, 0, len);
                }
            }
            catch (IOException e) {
                throw new ScmSystemException("failed to read remote data, remoteSite="
                        + targetSiteName + ", dataId=" + dataId, e);
            }
            finally {
                if (innerRemoteDataReader != null) {
                    innerRemoteDataReader.close();
                }
            }
        }

        RestUtils.flush(os);
    }

    // TODO:数据不存在：404
    @RequestMapping(value = "/datasource/{data_id}", method = RequestMethod.HEAD)
    public void headDataInfo(
            @PathVariable("data_id") String dataId,
            @RequestParam(value = CommonDefine.RestArg.DATASOURCE_SITE_NAME, required = false) String siteName,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            @RequestParam(value = CommonDefine.RestArg.DATASOURCE_SITE_LIST_WS_VERSION, required = false, defaultValue = "1") Integer wsVersion,
            @RequestParam(value = CommonDefine.RestArg.DATASOURCE_SITE_LIST_TABLE_NAME, required = false) String tableName,
            HttpServletResponse response) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        int localSiteId = contentModule.getLocalSite();
        int dataLocationSiteId = localSiteId;
        if (siteName != null) {
            ScmSite siteInfo = contentModule.getSiteInfo(siteName);
            if (null == siteInfo) {
                throw new ScmServerException(ScmError.SERVER_NOT_EXIST,
                        "site is not exist:siteName=" + siteName);
            }
            dataLocationSiteId = siteInfo.getId();
        }
        if (localSiteId == dataLocationSiteId) {

            BSONObject dataInfo = datasourceService.getDataInfo(wsName, ScmDataInfo
                    .forOpenExistData(type, dataId, new Date(createTime), wsVersion, tableName));
            response.setHeader(CommonDefine.RestArg.DATASOURCE_DATA_HEADER, dataInfo.toString());
        }
        else {
            ScmDataInfo scmDataInfo = ScmDataInfo.forOpenExistData(type, dataId,
                    new Date(createTime), wsVersion, tableName);
            long size = FileCommonOperator.getSize(siteName, wsName, scmDataInfo);
            BSONObject retInfo = new BasicBSONObject();
            retInfo.put(CommonDefine.RestArg.DATASOURCE_DATA_SIZE, size);
            response.setHeader(CommonDefine.RestArg.DATASOURCE_DATA_HEADER,
                    retInfo.toString());
        }
    }

    @PostMapping(value = "/datasource/{data_id}")
    public ResponseEntity<String> createData(@PathVariable("data_id") String dataId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            HttpServletRequest request) throws ScmServerException, IOException {
        ScmDataWriterContext writerContext;
        ScmWorkspaceInfo wsInfo = null;
        InputStream is = request.getInputStream();
        try {
            wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(wsName);
            writerContext = new ScmDataWriterContext();
            datasourceService.createDataInLocal(wsName,
                    ScmDataInfo.forCreateNewData(type, dataId, new Date(createTime),
                            wsInfo.getVersion()),
                    is, writerContext);
        }
        catch (ScmServerException e) {
            if (wsInfo != null) {
                // 出现异常，需要将本次写入数据采用的工作区版本号带给对端，如果是数据已存在，对端可以直接将版本号写到文件元数据中
                if (e.getExtraInfo() == null) {
                    e.setExtraInfo(new BasicBSONObject());
                }
                e.getExtraInfo().put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION,
                        wsInfo.getVersion());
            }
            throw e;
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(is);
        }
        ResponseEntity.BodyBuilder ret = ResponseEntity.ok();
        String tableName = writerContext.getTableName();
        if (tableName != null) {
            ret.header(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TABLE_NAME, tableName);
        }
        ret.header(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION, wsInfo.getVersion() + "");
        ret.body("");
        return ret.build();
    }

    @DeleteMapping(value = "/datasource/tables")
    public void deleteDataTable(
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TABLE_NAMES) List<String> tableNames,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_NAME, required = false) String wsName,
            @RequestBody(required = false) DataTableDeleteOption option) throws ScmServerException {
        ScmLocation location = null;
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmSite siteInfo = contentModule.getLocalSiteInfo();
        try {
            if (option != null) {
                location = DatalocationFactory.createDataLocation(siteInfo.getDataUrl().getType(),
                        option.getWsLocalSiteLocation(), siteInfo.getName());
            }
            datasourceService.deleteDataTables(tableNames, wsName, location);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "Failed to delete data tables:" + tableNames, e);
        }
    }
}
