package com.sequoiacm.metasource;

import java.util.HashMap;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;

// change rel table record to file table record
public class RelMetaCursor implements MetaCursor {
    private static final Map<String, String> REL_FIELD_MAP_FILE_FIELD = new HashMap<>();
    static {
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_DIRECTORY_ID,
                FieldName.FIELD_CLFILE_DIRECTORY_ID);
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_FILENAME, FieldName.FIELD_CLFILE_NAME);
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_FILEID, FieldName.FIELD_CLFILE_ID);
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_USER, FieldName.FIELD_CLFILE_INNER_USER);
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_UPDATE_USER,
                FieldName.FIELD_CLFILE_INNER_UPDATE_USER);
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_CREATE_TIME,
                FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_UPDATE_TIME,
                FieldName.FIELD_CLFILE_INNER_UPDATE_TIME);
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_MAJOR_VERSION,
                FieldName.FIELD_CLFILE_MAJOR_VERSION);
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_MINOR_VERSION,
                FieldName.FIELD_CLFILE_MINOR_VERSION);
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_FILE_SIZE,
                FieldName.FIELD_CLFILE_FILE_SIZE);
        REL_FIELD_MAP_FILE_FIELD.put(FieldName.FIELD_CLREL_FILE_MIME_TYPE,
                FieldName.FIELD_CLFILE_FILE_MIME_TYPE);
    }

    private MetaCursor innerCursor;

    public RelMetaCursor(MetaCursor relTableCursor) {
        this.innerCursor = relTableCursor;
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        BSONObject relRecord = innerCursor.getNext();
        if (relRecord == null) {
            return null;
        }
        BSONObject fileRec = new BasicBSONObject();
        for (String key : relRecord.keySet()) {
            String fileKey = REL_FIELD_MAP_FILE_FIELD.get(key);
            if (fileKey == null && !key.equals("_id")) {
                throw new ScmMetasourceException(
                        "inner error,unknown rel table key:record=" + relRecord.toString() + ",key="
                                + key);
            }
            if(fileKey != null) {
                fileRec.put(fileKey, relRecord.get(key));
            }
        }
        return fileRec;
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        return innerCursor.hasNext();
    }

    @Override
    public void close() {
        innerCursor.close();
    }
}
