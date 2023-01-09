package com.sequoiacm.fulltext.es.client8_2;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.es.client.base.EsClient;
import com.sequoiacm.fulltext.es.client.base.EsClientConfig;
import com.sequoiacm.fulltext.es.client.base.EsClientFactory;
import com.sequoiacm.fulltext.server.exception.FullTextException;

public class EsClientFactory8Impl implements EsClientFactory {
    @Override
    public EsClient createEsClient(EsClientConfig config) throws FullTextException {
        try {
            return new EsClientImpl(config);
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to init es client, client version is 8.2.x:\n1. please check elasticsearch server version is match or not;\n2. elasticsearch url is "
                            + config.getUrls()
                            + ", please check http protocol is match with server;\n3. make sure elasticsearch user/password/cert is valid;",
                    e);
        }
    }
}
