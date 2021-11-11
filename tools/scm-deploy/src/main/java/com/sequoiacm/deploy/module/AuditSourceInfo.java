package com.sequoiacm.deploy.module;

import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class AuditSourceInfo extends MetaSourceInfo {

    public static final ConfCoverter<AuditSourceInfo> CONVERTER = new ConfCoverter<AuditSourceInfo>() {
        @Override
        public AuditSourceInfo convert(BSONObject bson) {
            return new AuditSourceInfo(bson);
        }
    };

    public AuditSourceInfo(BSONObject bson) {
        super.setUrl(BsonUtils.getStringChecked(bson, ConfFileDefine.AUDITSOURCE_URL));
        super.setUser(BsonUtils.getStringChecked(bson, ConfFileDefine.AUDITSOURCE_USER));
        String passwd = BsonUtils.getStringOrElse(bson, ConfFileDefine.AUDITSOURCE_PASSWORD, "");
        String passwdFile = BsonUtils.getString(bson, ConfFileDefine.AUDITSOURCE_PASSWORD_FILE);
        PasswordInfo passwordInfo = new PasswordInfo(DatasourceType.SEQUOIADB, getUser(), passwd,
                passwdFile);
        CommonUtils.assertTrue(passwordInfo.getPlaintext() != null, "password is null!");
        super.setPassword(passwordInfo.getPlaintext());
        super.setDomain(BsonUtils.getStringChecked(bson, ConfFileDefine.AUDITSOURCE_DOMAIN));
        super.setType(ConfFileDefine.SEACTION_AUDITSOURCE);
    }

}
