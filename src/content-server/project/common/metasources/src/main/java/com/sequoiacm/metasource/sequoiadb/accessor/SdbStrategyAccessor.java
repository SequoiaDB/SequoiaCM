package com.sequoiacm.metasource.sequoiadb.accessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;

public class SdbStrategyAccessor extends SdbMetaAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbStrategyAccessor.class);
    
    public SdbStrategyAccessor(SdbMetaSource metasource, String csName, String clName) {
        super(metasource, csName, clName);
    }
}
