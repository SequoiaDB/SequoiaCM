package com.sequoiacm.fulltext.server.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.common.IOUtils;

@Component
public class TxtParser extends TextualParser {

    @Override
    public String parse(InputStream src) throws FullTextException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024 * 4];
        InputStreamReader isReader = null;
        BufferedReader bfReader = null;
        try {
            isReader = new InputStreamReader(src, "utf8");
            bfReader = new BufferedReader(isReader);
            while (true) {
                int readLen = bfReader.read(buf);
                if (readLen <= -1) {
                    return sb.toString();
                }
                sb.append(new String(buf, 0, readLen));
            }
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR, "failed to parse", e);
        }
        finally {
            IOUtils.close(bfReader, isReader);
        }
    }

    @Override
    public List<MimeType> type() {
        return Arrays.asList(MimeType.PLAIN, MimeType.HTML, MimeType.XML, MimeType.CSS);
    }

}
