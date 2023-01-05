package com.sequoiacm.fulltext.server.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.common.IOUtils;

@Component
public class DocxParser extends TextualParser {

    @Override
    public String parse(InputStream src) throws FullTextException {
        XWPFWordExtractor ext = null;
        try {
            XWPFDocument doc = new XWPFDocument(src);
            ext = new XWPFWordExtractor(doc);
            return ext.getText();
        }
        catch (IOException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR, "failed to parse file", e);
        }
        finally {
            IOUtils.close(ext);
        }
    }

    @Override
    public List<MimeType> type() {
        return Arrays.asList(MimeType.DOCX);
    }

}
