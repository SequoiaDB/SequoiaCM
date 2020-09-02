package com.sequoiacm.contentserver.controller;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.exception.ScmError;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

class RestUtils {

    private static final String CHARSET_UTF8 = "UTF-8";

    static PrintWriter getWriter(HttpServletResponse response) throws ScmServerException {
        try {
            return response.getWriter();
        } catch (IOException e) {
            throw new ScmSystemException("Failed to get writer", e);
        }
    }

    static ServletOutputStream getOutputStream(HttpServletResponse response) throws ScmServerException {
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            throw new ScmServerException(ScmError.NETWORK_IO, "Failed to get output stream", e);
        }
    }

    static void flush(ServletOutputStream outputStream) throws ScmServerException {
        try {
            outputStream.flush();
        } catch (IOException e) {
            throw new ScmServerException(ScmError.NETWORK_IO, "Failed to flush output stream", e);
        }
    }

    static InputStream getInputStream(MultipartFile file) throws ScmServerException {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new ScmServerException(ScmError.NETWORK_IO, "Failed to get input stream", e);
        }
    }

    static String urlEncode(String s) throws ScmServerException {
        try {
            return URLEncoder.encode(s, CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new ScmSystemException("Encoding is not supported", e);
        } catch (IllegalArgumentException e) {
            throw new ScmInvalidArgumentException("Invalid string: " + s, e);
        }
    }

    static String urlDecode(String s) throws ScmServerException {
        try {
            return URLDecoder.decode(s, CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new ScmSystemException("Encoding is not supported", e);
        } catch (IllegalArgumentException e) {
            throw new ScmInvalidArgumentException("Invalid string: " + s, e);
        }
    }

    static void checkWorkspaceName(String wsName) throws ScmInvalidArgumentException {
        if (StringUtils.isEmpty(wsName)) {
            throw new ScmInvalidArgumentException("missing required parameter:workspace_name=null");
        }
    }
}
