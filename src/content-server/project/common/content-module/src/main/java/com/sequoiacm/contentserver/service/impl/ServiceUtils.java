package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

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
        putCursorToWriter(cursor, new Converter<BSONObject>() {
            @Override
            public String toJSON(BSONObject bsonObject) {
                return bsonObject.toString();
            }
        }, writer);
    }

    public static <T> void putCursorToWriter(ScmObjectCursor<T> cursor, Converter<T> c,
            PrintWriter writer) throws ScmServerException {
        int count = 0;
        try {
            writer.write("[");
            if (cursor.hasNext()) {
                while (true) {
                    writer.write(c.toJSON(cursor.getNext()));
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

