package com.sequoiacm.fulltext.server.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.common.IOUtils;

@Component
public class XlsxParser extends TextualParser {

    @Override
    public String parse(InputStream src) throws FullTextException {
        XSSFExcelExtractor ext = null;
        XSSFWorkbook wb = null;
        try {
            wb = new XSSFWorkbook(src);
            ext = new XSSFExcelExtractor(wb);
            ext.setIncludeSheetNames(true);
            return ext.getText();
        }
        catch (IOException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR, "failed to parse file", e);
        }
        finally {
            IOUtils.close(ext, wb);
        }
    }

    @Override
    public List<MimeType> type() {
        return Arrays.asList(MimeType.XLSX);
    }

}
