package com.sequoiacm.datasource.metadata.sequoiadb;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmError;

public class SdbLocation extends ScmLocation {

    private String domain;

    public SdbLocation(BSONObject sdbLocation) throws ScmDatasourceException {
        super(sdbLocation);
        domain = (String) sdbLocation.get(FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);
        if(domain == null) {
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "domain not exists:" + sdbLocation);
        }
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public String getType() {
        return "sequoiadb";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id=").append(getSiteId()).append(",");
        sb.append("domain=").append(domain);

        return sb.toString();
    }

    @Override
    public boolean equals(Object right) {
        if (right == this) {
            return true;
        }

        if (!(right instanceof SdbLocation)) {
            return false;
        }

        if (!super.equals(right)) {
            return false;
        }

        SdbLocation r = (SdbLocation)right;
        return domain.equals(r.domain);
    }
}
