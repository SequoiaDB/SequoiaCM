package com.sequoiacm.fulltext.server.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;

@Component
public class TextualParserMgr {
    private Map<MimeType, TextualParser> parsers = new HashMap<>();

    @Autowired
    public TextualParserMgr(List<TextualParser> parserList) throws FullTextException {
        for (TextualParser p : parserList) {
            for (MimeType type : p.type()) {
                TextualParser oldParser = parsers.put(type, p);
                if (oldParser != null) {
                    throw new FullTextException(ScmError.SYSTEM_ERROR,
                            "conflict textualParser, mimeType=" + type + ", conflict classes=["
                                    + p.getClass().getName() + ", " + oldParser.getClass().getName()
                                    + "+]");
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(TextualParserMgr.class.getName());
    }

    public TextualParser getParser(MimeType type) throws FullTextException {
        TextualParser p = parsers.get(type);
        if (p == null) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "no such textual parser, mimeType:" + type);
        }
        return p;
    }
}
