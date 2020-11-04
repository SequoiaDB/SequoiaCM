package com.sequoiacm.fulltext.server.service;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ScmEleCursor;
import com.sequoiacm.content.client.model.ScmFileInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.es.EsDoumentCursor;
import com.sequoiacm.fulltext.server.es.EsSearchRes;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.common.IOUtils;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;

public class FulltextSearchCursor implements FulltextCursor {
    private EsDoumentCursor esCursor;
    private ContentserverClient csClient;
    private String wsName;
    private int scop;
    private BasicBSONObject fullFileCondition;
    private List<FulltextSearchRes> nextBatch;
    private BasicBSONList esFileIds;
    private ObjectMapper objMapper = new ObjectMapper();

    public FulltextSearchCursor(ScmWorkspaceFulltextExtData wsExternalData, int scope,
            BSONObject contentCondition, BSONObject fileCondition, ContentserverClient csClient,
            EsClient esClient) throws FullTextException {
        this.csClient = csClient;
        this.wsName = wsExternalData.getWsName();
        this.scop = scope;

        // 找内容服务查询文件的匹配条件
        fullFileCondition = new BasicBSONObject();
        BasicBSONList fullFileConditionAndArr = new BasicBSONList();

        // fullFileConditionAndArr[0] 描述用户的文件匹配条件
        fullFileConditionAndArr.add(fileCondition);

        // fullFileConditionAndArr[1] 描述文件的索引状态，必须是 created
        BasicBSONObject fileStatus = new BasicBSONObject(
                FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                        + ScmFileFulltextExtData.FIELD_IDX_STATUS,
                ScmFileFulltextStatus.CREATED.name());
        fullFileConditionAndArr.add(fileStatus);

        // fullFileConditionAndArr[2] 描述es搜索出来的文件ID列表，现在填个空 List
        esFileIds = new BasicBSONList();
        BasicBSONObject dollarIn = new BasicBSONObject("$in", esFileIds);
        fullFileConditionAndArr.add(new BasicBSONObject(FieldName.FIELD_CLFILE_ID, dollarIn));

        fullFileCondition.put("$and", fullFileConditionAndArr);

        esCursor = esClient.search(wsExternalData.getIndexDataLocation(), contentCondition);
    }

    public List<FulltextSearchRes> getNext() throws FullTextException {
        if (hasNext()) {
            List<FulltextSearchRes> ret = nextBatch;
            nextBatch = null;
            return ret;
        }
        return null;
    }

    @Override
    public boolean hasNext() throws FullTextException {
        if (nextBatch != null) {
            return true;
        }
        nextBatch = getNextFromServer();
        if (nextBatch == null) {
            return false;
        }
        return true;
    }

    public List<FulltextSearchRes> getNextFromServer() throws FullTextException {
        while (true) {

            // 先从es拿一批到满足全文检索条件的文件 esDocuments
            List<EsSearchRes> esDocuments = esCursor.getNextBatch();
            if (esDocuments == null || esDocuments.size() <= 0) {
                return null;
            }
            // 排序是为了后续对 esDocuments 集合进行二分查找
            Collections.sort(esDocuments, EsSearchResComp.INSTANCE);

            // 将es中查到的文件id列表填充到查询scm文件的匹配条件中
            fillEsFileIdsToScmFileFilter(esDocuments);

            ScmEleCursor<ScmFileInfo> scmFileCursor = null;
            try {
                scmFileCursor = csClient.listFile(wsName, fullFileCondition, scop, null, 0, -1);

                // ret 表示返回给客户端的一批结果
                List<FulltextSearchRes> ret = new ArrayList<>();

                while (scmFileCursor.hasNext()) {
                    ScmFileInfo scmFile = scmFileCursor.getNext();
                    ScmFileFulltextExtData scmFileExtData = new ScmFileFulltextExtData(
                            scmFile.getExternalData());

                    // 将scm中查询到的文件包装成 EsSearchRes 对象，然后二分查找 esDocuments
                    // 中是否存在该对象，
                    // 存在表示这个scm文件是我们要的，不存在表示可能在 es 中查询到了残留数据，跳过这个文件。
                    EsSearchRes keyDoc = new EsSearchRes(scmFileExtData.getIdxDocumentId(),
                            scmFile.getId(),
                            scmFile.getMajorVersion() + "." + scmFile.getMinorVersion(), 0, null);
                    int destIdx = Collections.binarySearch(esDocuments, keyDoc,
                            new EsSearchResComp());
                    if (destIdx < 0) {
                        continue;
                    }
                    EsSearchRes esDoc = esDocuments.get(destIdx);
                    // TODO: 后续考虑能自定义一些返回字段
                    FulltextSearchRes e = new FulltextSearchRes(scmFile, esDoc.getScore(),
                            esDoc.getHighlight());
                    ret.add(e);
                }
                if (ret.size() <= 0) {
                    continue;
                }
                // 给客户端的数据需要是按分数从大到小排序的
                // 这条代码保证本批数据的顺序，同时由于从 es 中查询的时候也是按分数排序的，所以
                // 整个游标返回的数据都是有序的。
                Collections.sort(ret, FulltextSearchResComp.INSTANCE);
                return ret;
            }
            catch (ScmServerException e) {
                throw new FullTextException(e.getError(),
                        "failed to query scm file in contentserver", e);
            }
            finally {
                IOUtils.close(scmFileCursor);
            }
        }
    }

    private void fillEsFileIdsToScmFileFilter(List<EsSearchRes> esDocuments) {
        esFileIds.clear();
        for (EsSearchRes esDoc : esDocuments) {
            esFileIds.add(esDoc.getFileId());
        }
    }

    @Override
    public void close() {
        esCursor.close();
    }

    @Override
    public void writeNextToWriter(PrintWriter writer) throws Exception {
        List<FulltextSearchRes> next = getNext();
        if (next == null) {
            return;
        }
        for (Iterator<FulltextSearchRes> iterator = next.iterator(); iterator.hasNext();) {
            FulltextSearchRes fulltextSearchRes = iterator.next();
            writer.write(objMapper.writeValueAsString(fulltextSearchRes));
            if (iterator.hasNext()) {
                writer.write(",");
            }
        }
    }

}

class EsSearchResComp implements Comparator<EsSearchRes> {
    public static final EsSearchResComp INSTANCE = new EsSearchResComp();

    @Override
    public int compare(EsSearchRes o1, EsSearchRes o2) {
        String o1Str = o1.getFileId() + o1.getFileVersion() + o1.getDocId();
        String o2Str = o2.getFileId() + o2.getFileVersion() + o2.getDocId();
        return o1Str.compareTo(o2Str);
    }

}

class FulltextSearchResComp implements Comparator<FulltextSearchRes> {
    public static final FulltextSearchResComp INSTANCE = new FulltextSearchResComp();

    @Override
    public int compare(FulltextSearchRes o1, FulltextSearchRes o2) {
        float ret = o2.getScore() - o1.getScore();
        if (ret < 0) {
            return -1;
        }
        if (ret == 0) {
            return 0;
        }
        return 1;
    }

}
