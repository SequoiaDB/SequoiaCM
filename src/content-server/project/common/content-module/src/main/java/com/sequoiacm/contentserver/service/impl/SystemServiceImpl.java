package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.service.ISystemService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SystemServiceImpl implements ISystemService {
    private static final Logger logger = LoggerFactory.getLogger(SystemServiceImpl.class);

    @Autowired
    private Environment environment;

    @Override
    public BSONObject getConfs(String[] keys) throws ScmServerException {
        BSONObject respBSON = new BasicBSONObject();
        for (String key : keys) {
            String value = PropertiesUtils.getProperty(key);
            if (value == null) {
                value = environment.getProperty(key);
            }
            respBSON.put(key, value);
        }
        return respBSON;
    }

    @Override
    public MetaCursor getNodeList(BSONObject condition) throws ScmServerException {
        MetaAccessor serverAccessor = ScmContentModule.getInstance().getMetaService()
                .getMetaSource().getServerAccessor();
        try {
            return serverAccessor.query(condition, null, null);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get node list: condition=" + condition, e);
        }
    }
}
