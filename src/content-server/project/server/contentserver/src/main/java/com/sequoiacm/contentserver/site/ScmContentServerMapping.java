package com.sequoiacm.contentserver.site;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import org.bson.BSONObject;

public class ScmContentServerMapping {
    private int id;
    private String name;
    private int site_id;
    private String host_name;
    private int port;
    private int type;

    public ScmContentServerMapping(BSONObject record) throws ScmServerException {
        try {
            Object tmp = record.get(FieldName.FIELD_CLCONTENTSERVER_NAME);
            if (null == tmp) {
                throw new ScmInvalidArgumentException(
                        "field is not exist:fieldName=" + FieldName.FIELD_CLCONTENTSERVER_NAME);
            }
            name = (String)tmp;

            tmp = record.get(FieldName.FIELD_CLCONTENTSERVER_ID);
            if (null == tmp) {
                throw new ScmInvalidArgumentException(
                        "field is not exist:fieldName=" + FieldName.FIELD_CLCONTENTSERVER_ID);
            }
            id = (int)tmp;

            tmp = record.get(FieldName.FIELD_CLCONTENTSERVER_SITEID);
            if (null == tmp) {
                throw new ScmInvalidArgumentException(
                        "field is not exist:fieldName=" + FieldName.FIELD_CLCONTENTSERVER_SITEID);
            }
            site_id = (int)tmp;

            tmp = record.get(FieldName.FIELD_CLCONTENTSERVER_HOST_NAME);
            if (null == tmp) {
                throw new ScmInvalidArgumentException(
                        "field is not exist:fieldName=" + FieldName.FIELD_CLCONTENTSERVER_HOST_NAME);
            }
            host_name = (String)tmp;

            tmp = record.get(FieldName.FIELD_CLCONTENTSERVER_PORT);
            if (null == tmp) {
                throw new ScmInvalidArgumentException(
                        "field is not exist:fieldName=" + FieldName.FIELD_CLCONTENTSERVER_PORT);
            }
            port = (int)tmp;

            tmp = record.get(FieldName.FIELD_CLCONTENTSERVER_TYPE);
            if (null == tmp) {
                throw new ScmInvalidArgumentException(
                        "field is not exist:fieldName=" + FieldName.FIELD_CLCONTENTSERVER_TYPE);
            }
            type = (int)tmp;
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(), "parse contentserver failed:record="
                    + record.toString(), e);
        }
        catch (Exception e) {
            throw new ScmInvalidArgumentException(
                    "parse contentserver failed:record=" + record.toString(), e);
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSite_id() {
        return site_id;
    }

    public String getHost_name() {
        return host_name;
    }

    public int getPort() {
        return port;
    }

    public int getType() {
        return type;
    }
}
