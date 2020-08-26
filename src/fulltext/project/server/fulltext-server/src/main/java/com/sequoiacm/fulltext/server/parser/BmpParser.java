package com.sequoiacm.fulltext.server.parser;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;

@Component
public class BmpParser extends TextualParser {
    @Autowired
    private JpgPngParser pngParser;

    @Override
    public String parse(InputStream src) throws FullTextException {
        ByteArrayOutputStream png = null;
        try {
            BufferedImage bmp = ImageIO.read(src);
            png = new ByteArrayOutputStream();
            ImageIO.write(bmp, "PNG", png);
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR, "failed to parse bmp file", e);
        }
        return pngParser.parse(new ByteArrayInputStream(png.toByteArray()));
    }

    @Override
    public List<MimeType> type() {
        return Arrays.asList(MimeType.BMP);
    }

}
