package com.sequoiadb.infrastructure.map.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;

import com.sequoiacm.infrastructure.metasource.MetaCursor;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.ScmSystemException;

public class ServerUtils {
    public static void checkMapName(String mapName) throws ScmMapServerException {
        if (StringUtils.isEmpty(mapName)) {
            throw new ScmMapServerException(ScmMapError.INVALID_ARGUMENT,
                    "missing required map_name=null");
        }
    }

    public static void checkKeyType(String keyType) throws ScmMapServerException {
        Class<?> keyClass = null;
        try {
            keyClass = Class.forName(keyType);
        }
        catch (ClassNotFoundException e) {
        }
        if (keyClass == null || keyClass.isPrimitive()) {
            throw new ScmMapServerException(ScmMapError.INVALID_ARGUMENT,
                    "key class is not primitive class: keyType=" + keyType);
        }

    }

    /**
     * traverse cursor, and close cursor
     * 
     * @param cursor
     * @param writer
     * @throws ScmMapServerException
     */
    public static void putCursorToWriter(MetaCursor cursor, PrintWriter writer,
            int listInstanceCheckInterval) throws ScmMapServerException {
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
                    if (count++ == listInstanceCheckInterval) {
                        if (writer.checkError()) {
                            throw new ScmMapServerException(ScmMapError.NETWORK_IO,
                                    "failed to write response to client because of ioexception");
                        }
                        count = 0;
                    }
                }
            }
            writer.write("]");
            if (writer.checkError()) {
                throw new ScmMapServerException(ScmMapError.NETWORK_IO,
                        "failed to write response to client because of ioexception");
            }
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(), "Failed to put cursor to writer", e);
        }
        catch (Exception e) {
            throw new ScmSystemException("traverse cursor failed", e);
        }
        finally {
            cursor.close();
            writer.flush();
        }
    }

    public static PrintWriter getWriter(HttpServletResponse response) throws ScmMapServerException {
        try {
            return response.getWriter();
        }
        catch (IOException e) {
            throw new ScmSystemException("Failed to get writer", e);
        }
    }

}
