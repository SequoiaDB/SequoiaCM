package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
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
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

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
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime)
            throws ScmServerException {
        datasourceService.deleteDataLocal(wsName, dataId, type, createTime);
    }

    @DeleteMapping(path = "/datasource/{data_id}", params = "action=delete_data_in_site_list")
    public void deleteDataInSiteList(@PathVariable("data_id") String dataId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST) List<Integer> siteList)
            throws ScmServerException {
        datasourceService.deleteDataInSiteList(wsName, dataId, type, createTime, siteList);
    }

    @GetMapping("/datasource/{data_id}")
    public void readData(@PathVariable("data_id") String dataId,
            @RequestParam(value = CommonDefine.RestArg.DATASOURCE_SITE_NAME, required = false) String targetSiteName,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            @RequestParam(CommonDefine.RestArg.FILE_READ_FLAG) int readFlag,
            HttpServletResponse response) throws ScmServerException {
        response.setHeader("Content-Disposition", "attachment; filename=" + dataId);

        ServletOutputStream os = RestUtils.getOutputStream(response);
        if (targetSiteName == null || targetSiteName
                .equals(ScmContentModule.getInstance().getLocalSiteInfo().getName())) {
            DatasourceReaderDao dao = datasourceService.readData(wsName, dataId, type, createTime,
                    readFlag, os);
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
                        new ScmDataInfo(type, dataId, new Date(createTime)), readFlag);
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
            BSONObject dataInfo = datasourceService.getDataInfo(wsName, dataId, type, createTime);
            response.setHeader(CommonDefine.RestArg.DATASOURCE_DATA_HEADER, dataInfo.toString());
        }
        else {
            ScmDataInfo scmDataInfo = new ScmDataInfo(type, dataId, new Date(createTime));
            long size = FileCommonOperator.getSize(siteName, wsName, scmDataInfo);
            BSONObject retInfo = new BasicBSONObject();
            retInfo.put(CommonDefine.RestArg.DATASOURCE_DATA_SIZE, size);
            response.setHeader(CommonDefine.RestArg.DATASOURCE_DATA_HEADER,
                    retInfo.toString());
        }
    }

    @PostMapping(value = "/datasource/{data_id}")
    public void createData(@PathVariable("data_id") String dataId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            HttpServletRequest request) throws ScmServerException, IOException {
        InputStream is = request.getInputStream();
        try {
            datasourceService.createDataInLocal(wsName, dataId, type, createTime, is);
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(is);
        }
    }

    @DeleteMapping(value = "/datasource/tables")
    public void deleteDataTable(
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TABLE_NAMES) List<String> tableNames)
                    throws ScmServerException {
        datasourceService.deleteDataTables(tableNames);
    }
}
