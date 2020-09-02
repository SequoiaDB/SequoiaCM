package com.sequoiacm.contentserver.service.impl;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.stereotype.Service;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.service.ISiteService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;

@Service
public class SiteServiceImpl implements ISiteService {

    @Override
    public BSONObject getSite(String siteName) throws ScmServerException {
        MetaAccessor accessor = ScmContentServer.getInstance().getMetaService().getMetaSource().getSiteAccessor();
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLSITE_NAME, siteName);
        BSONObject site = ScmMetaSourceHelper.queryOne(accessor, matcher);
        if(site == null) {
            throw new ScmServerException(ScmError.SITE_NOT_EXIST, "site not exist:siteName=" + siteName);
        }
        return site;
    }

    @Override
    public MetaCursor getSiteList(BSONObject condition) throws ScmServerException {
        MetaAccessor accessor = ScmContentServer.getInstance().getMetaService().getMetaSource().getSiteAccessor();
        try {
            return accessor.query(condition, null, null);
        } catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get site list, condition=" + condition, e);
        }
    }
}
