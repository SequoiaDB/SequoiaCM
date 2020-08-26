package com.sequoiacm.fulltext.server.parser;

import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequoiacm.common.MimeType;
import com.sequoiacm.fulltext.server.exception.FullTextException;

public abstract class TextualParser {
    @Autowired
    private TextualParserConfig config;

    public abstract String parse(InputStream src) throws FullTextException;

    public abstract List<MimeType> type();

    public int fileSizeLimit() {
        return config.getFileSizeLimit();
    }
    
}

@Configuration
@ConfigurationProperties("scm.fulltext.textualParser")
class TextualParserConfig {
    private int fileSizeLimit = 10 * 1024 * 1024;

    public int getFileSizeLimit() {
        return fileSizeLimit;
    }

    public void setFileSizeLimit(int fileSizeLimit) {
        this.fileSizeLimit = fileSizeLimit;
    }
}
