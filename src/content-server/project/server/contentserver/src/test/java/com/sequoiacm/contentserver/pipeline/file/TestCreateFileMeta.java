package com.sequoiacm.contentserver.pipeline.file;

import static org.mockito.ArgumentMatchers.anyString;

import org.apache.commons.lang.StringUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.common.ScmTestBase;
import com.sequoiacm.common.TestFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileMetaResult;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.config.MetaSourceLocation;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;

@PrepareForTest({ ScmContentModule.class })
public class TestCreateFileMeta extends ScmTestBase {

    @Configuration
    @ComponentScan(value = { "com.sequoiacm.contentserver.bucket",
            "com.sequoiacm.contentserver.pipeline.file" })
    static class InnerConfig {
    }

    private static String WORKSPACE = "ws_default";

    @Mock
    private ScmContentModule mockContentModule;

    @Mock
    private ScmWorkspaceInfo wsInfo;

    private FileMetaOperator fileMetaOperator;

    @BeforeClass
    public void setUp() throws Exception {
        loadSpringContext();
        mockContentModule();
    }

    @Test
    public void test() throws ScmServerException {
        FileMeta fileMeta = FileMeta.fromRecord(generateFileRec());
        CreateFileMetaResult result = fileMetaOperator.createFileMeta(WORKSPACE, fileMeta, null);
        FileMeta newFile = result.getNewFile();
        Assert.assertTrue(StringUtils.equals(fileMeta.getName(), newFile.getName()));
        Assert.assertTrue(StringUtils.equals(fileMeta.getCreateMonth(), newFile.getCreateMonth()));
    }

    @AfterClass
    public void tearDown() {

    }

    private void mockContentModule() throws Exception {
        PowerMockito.mockStatic(ScmContentModule.class);
        PowerMockito.doReturn(mockContentModule).when(ScmContentModule.class, "getInstance");

        // mock metaService
        ScmMetaService metaService = TestFactory.getInstance().getMetaService();
        PowerMockito.when(mockContentModule.getMetaService()).thenReturn(metaService);
        // mock extra
        BSONObject metaLocation = new BasicBSONObject();
        metaLocation.put(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID, TestDefine.ROOT_SITE_ID);
        metaLocation.put(FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN, TestDefine.META_DOMAIN);
        BSONObject metaShardingType = new BasicBSONObject();
        metaShardingType.put(FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE,
                ScmShardingType.YEAR.getName());
        MetaSourceLocation metaSourceLocation = new SdbMetaSourceLocation(metaLocation,
                metaShardingType);

        PowerMockito.when(wsInfo.getName()).thenReturn(WORKSPACE);
        PowerMockito.when(wsInfo.getMetaLocation()).thenReturn(metaSourceLocation);
        PowerMockito.when(mockContentModule.getWorkspaceInfoCheckExist(anyString()))
                .thenReturn(wsInfo);
    }

    private void loadSpringContext() {
        // 手动加载 Spring 容器
        ApplicationContext context = new AnnotationConfigApplicationContext(InnerConfig.class);
        fileMetaOperator = (FileMetaOperator) context.getBean("fileMetaOperator");
        // 对注解了 @Mock 的对象进行模拟
        MockitoAnnotations.initMocks(this);
    }

    private BSONObject generateFileRec() {
        BSONObject fileRec = new BasicBSONObject();
        fileRec.put(FieldName.FIELD_CLFILE_ID, "637836fc40000100e3b31b861");
        fileRec.put(FieldName.FIELD_CLFILE_NAME, "com/sequoiacm/contentserver/pipeline/file3");

        fileRec.put(FieldName.FIELD_CLFILE_MINOR_VERSION, 0);
        fileRec.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, 1);
        fileRec.put(FieldName.FIELD_CLFILE_TYPE, 1);
        fileRec.put(FieldName.FIELD_CLFILE_INNER_USER, "admin");
        fileRec.put(FieldName.FIELD_CLFILE_INNER_UPDATE_USER, "admin");
        fileRec.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, 1669034292491L);
        fileRec.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME, 1669034292491L);
        fileRec.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, "202211");
        fileRec.put(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME, 1669034292491L);
        fileRec.put(FieldName.FIELD_CLFILE_FILE_DATA_TYPE, ENDataType.Normal.getValue());
        fileRec.put(FieldName.FIELD_CLFILE_FILE_SIZE, 4096);
        fileRec.put(FieldName.FIELD_CLFILE_CUSTOM_TAG, new BasicBSONObject());

        fileRec.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, "637836fc40000100e3b31b8432");
        fileRec.put(FieldName.FIELD_CLFILE_VERSION_SERIAL, "1.0");
        fileRec.put(FieldName.FIELD_CLFILE_DELETE_MARKER, false);
        return fileRec;
    }
}
