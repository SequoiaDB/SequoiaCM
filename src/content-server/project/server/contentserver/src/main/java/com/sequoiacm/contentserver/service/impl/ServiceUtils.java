package com.sequoiacm.contentserver.service.impl;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;

public class ServiceUtils {

    /**
     * traverse cursor, and close cursor
     * 
     * @param cursor
     * @param writer
     * @throws ScmServerException
     */
    public static void putCursorToWriter(MetaCursor cursor, PrintWriter writer)
            throws ScmServerException {
        int count = 0;
        try {
            writer.write("[");
            if (cursor.hasNext()) {
                while (true) {
                    writer.write(cursor.getNext().toString());
                    if (cursor.hasNext()) {
                        writer.write(",");
                    }
                    else {
                        break;
                    }
                    if (count++ == PropertiesUtils.getListInstanceCheckInterval()) {
                        if (writer.checkError()) {
                            throw new ScmServerException(ScmError.NETWORK_IO,
                                    "failed to write response to client because of ioexception");
                        }
                        count = 0;
                    }
                }
            }
            writer.write("]");
            if (writer.checkError()) {
                throw new ScmServerException(ScmError.NETWORK_IO,
                        "failed to write response to client because of ioexception");
            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to put cursor to writer", e);
        }
        catch (Exception e) {
            throw new ScmSystemException("traverse cursor failed", e);
        }
        finally {
            cursor.close();
            writer.flush();
        }
    }

    public static PrintWriter getWriter(HttpServletResponse response) throws ScmServerException {
        try {
            return response.getWriter();
        }
        catch (IOException e) {
            throw new ScmSystemException("Failed to get writer", e);
        }
    }
}
