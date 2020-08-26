package com.sequoiacm.fulltext.server.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.common.IOUtils;

@Component
public class XlsParser extends TextualParser {
    public static void main(String[] args)
            throws FileNotFoundException, IOException, FullTextException {
        XlsParser x = new XlsParser();
        String ext = x.parse(new FileInputStream("D:\\data\\text_test_data\\xlsx.xlsx"));
        System.out.println(ext);
    }

    @Override
    public String parse(InputStream src) throws FullTextException {
        ExcelExtractor ext = null;
        HSSFWorkbook wb = null;
        try {
            wb = new HSSFWorkbook(src);
            ext = new ExcelExtractor(wb);
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
        return Arrays.asList(MimeType.MS_EXCEL);
    }
}
