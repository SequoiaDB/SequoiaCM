package com.sequoiacm.fulltext.es.client_6_3;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.es.client.base.EsClient;
import com.sequoiacm.fulltext.es.client.base.EsClientConfig;
import com.sequoiacm.fulltext.es.client.base.EsClientFactory;
import com.sequoiacm.fulltext.server.exception.FullTextException;

import java.net.MalformedURLException;

public class EsClientFactoryImpl implements EsClientFactory {
    @Override
    public EsClient createEsClient(EsClientConfig config) throws FullTextException {
        try {
            return new EsClientImpl(config);
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to init es client, client version is 6.3.x:\n1. please check elasticsearch server version is match or not;\n2. this es client only support http protocol (url= "
                            + config.getUrls()
                            + ");\n3. elasticsearch server must has no password",
                    e);
        }
    }
}
