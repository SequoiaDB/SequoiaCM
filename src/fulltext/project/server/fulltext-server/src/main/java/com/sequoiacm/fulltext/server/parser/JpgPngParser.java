package com.sequoiacm.fulltext.server.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.util.IOUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.lept;
import org.bytedeco.javacpp.lept.PIX;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;

@Component
public class JpgPngParser extends TextualParser {
    private static final Logger logger = LoggerFactory.getLogger(JpgPngParser.class);
    private PicParserConfig conf;

    public static void main(String[] args) throws Exception {
        JpgPngParser p = new JpgPngParser(new PicParserConfig());
        System.out.println(p.parse(new FileInputStream("./10.JPG")));
    }

    @Autowired
    public JpgPngParser(PicParserConfig conf) {
        this.conf = conf;
    }

    @Override
    public String parse(InputStream src) throws FullTextException {
        byte[] picData;
        try {
            picData = IOUtils.toByteArray(src);
        }
        catch (IOException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR, "failed to read pic data", e);
        }

        TessBaseAPI api = null;
        BytePointer text = null;
        PIX pix = null;
        try {
            api = new TessBaseAPI();
            if (api.Init(conf.getTessdataDir(), conf.getLanguage()) != 0) {
                throw new FullTextException(ScmError.SYSTEM_ERROR,
                        "failed init pic parser, see detail in error.log, tessdata="
                                + conf.getTessdataDir() + ", language=" + conf.getLanguage());
            }
            pix = lept.pixReadMem(picData, picData.length);
            api.SetImage(pix);
            text = api.GetUTF8Text();
            if (text == null) {
                throw new FullTextException(ScmError.SYSTEM_ERROR, "failed to parse pic data");
            }
            // Tess图像识别很容易在每个汉字之间加入空格
            // 当两个汉字之间存在一个空格时，去除这个空格
            Pattern p = Pattern.compile("(?<=[\\x{4e00}-\\x{9fa5}])(\\s)(?=[\\x{4e00}-\\x{9fa5}])");
            String s = text.getString();
            Matcher m = p.matcher(s);
            return m.replaceAll("");
        }
        finally {
            destoryTess(api, text, pix);
        }
    }

    private void destoryTess(TessBaseAPI api, BytePointer text, PIX pix) {
        if (api != null) {
            try {
                api.End();
            }
            catch (Exception e) {
                logger.warn("failed to rlease TessBaseAPI", e);
            }
        }
        if (text != null) {
            try {
                text.deallocate();
            }
            catch (Exception e) {
                logger.warn("failed to deallocate text bytepointer", e);
            }
        }
        if (pix != null) {
            try {
                lept.pixDestroy(pix);
            }
            catch (Exception e) {
                logger.warn("failed to destory pix", e);
            }
        }
    }

    @Override
    public List<MimeType> type() {
        return Arrays.asList(MimeType.PNG, MimeType.JPEG);
    }

    //    public static void main(String[] args) {
    //        System.out.println("hi 你好 哈哈 啊 1 hello my friend"
    //                .replaceAll("([\\u4e00-\\u9fa5])( )([\\u4e00-\\u9fa5])", "$1$3"));
    //    }
}

@Configuration
@ConfigurationProperties("scm.fulltext.textualParser.pic")
class PicParserConfig {
    private String tessdataDir = "/usr/share/tesseract-ocr/tessdata/";

    private String language = "eng+chi_sim";

    public PicParserConfig() {
    }

    public String getTessdataDir() {
        return tessdataDir;
    }

    public void setTessdataDir(String tessdataDir) {
        this.tessdataDir = tessdataDir;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
