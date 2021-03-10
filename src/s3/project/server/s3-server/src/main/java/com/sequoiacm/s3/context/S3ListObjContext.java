package com.sequoiacm.s3.context;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.s3.common.S3Codec;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.cursor.S3ObjectCursor;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.Batch;
import com.sequoiacm.s3.model.ListObjRecord;
import com.sequoiacm.s3.remote.ScmContentServerClient;

public class S3ListObjContext {
    private S3ObjectCursor objCursor;
    private S3ListObjContextMeta meta;
    private S3ListObjContextMgr contextMgr;
    private boolean isNewContext;

    public boolean isNewContext() {
        return isNewContext;
    }

    public void setNewContext(boolean isNewContext) {
        this.isNewContext = isNewContext;
    }

    public S3ListObjContext(ScmContentServerClient client, S3ListObjContextMgr contextMgr,
            S3ListObjContextMeta meta) throws S3ServerException {
        this.meta = meta;
        this.contextMgr = contextMgr;
        if (meta.getDelimiter() == null) {
            this.objCursor = new S3ObjectCursor(client, meta.getFormatedPrefix(), meta.getLastMarker(),
                    meta.getBucketDir(), false);
            return;
        }
        if (meta.getDelimiter().equals(S3CommonDefine.SCM_DIR_SEP)) {
            this.objCursor = new S3ObjectCursor(client, meta.getFormatedPrefix(), meta.getLastMarker(),
                    meta.getBucketDir(), true);
            return;
        }
        throw new S3ServerException(S3Error.INVALID_ARGUMENT,
                "unsupported delimiter:" + meta.getDelimiter());
    }

    public S3ListObjContextMeta getMeta() {
        return meta;
    }

    public boolean hasMore() throws S3ServerException {
        return objCursor.hasNext();
    }

    public Batch query(int maxKey, boolean fetchOwner, String encodingType)
            throws S3ServerException {
        List<ListObjRecord> prefixList = new ArrayList<>();
        List<ListObjRecord> contentList = new ArrayList<>();
        int count = 0;
        ListObjRecord last = null;
        while (objCursor.hasNext()) {
            if (count >= maxKey) {
                break;
            }
            ListObjRecord r = objCursor.getNext(fetchOwner, encodingType);
            if (r == null) {
                break;
            }
            count++;
            last = r;
            if (r.isContent()) {
                contentList.add(r);
                continue;
            }

            prefixList.add(r);
        }
        Batch b = new Batch();
        if (prefixList.size() > 0) {
            b.setCmPrefix(prefixList);
        }
        if (contentList.size() > 0) {
            b.setContent(contentList);
        }
        updateLastMarker(last, encodingType);
        return b;
    }

    public String getLastMarker() {
        String m = meta.getLastMarker();
        if(m != null && m.startsWith(S3CommonDefine.SCM_DIR_SEP )) {
            return m.substring(1);
        }
        return m;
    }

    private void updateLastMarker(ListObjRecord r, String encodingType) throws S3ServerException {
        if (r == null) {
            return;
        }
        if (r.isContent()) {
            meta.updateLastMarker(S3Codec.decode(r.getKey(), encodingType));
            return;
        }
        meta.updateLastMarker(S3Codec.decode(r.getPrefix(), encodingType));

    }

    public void release() throws S3ServerException {
        contextMgr.remove(meta.getId());
    }

    public void save() throws S3ServerException {
        meta.setUpdateTime(System.currentTimeMillis());
        contextMgr.updateContextMeta(meta);
    }
}
