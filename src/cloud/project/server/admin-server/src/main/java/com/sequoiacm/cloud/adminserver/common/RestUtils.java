package com.sequoiacm.cloud.adminserver.common;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;

@Component
public class RestUtils {

    /**
     * traverse cursor, and close cursor
     * 
     * @param cursor
     * @param writer
     * @throws StatisticsException
     */
    public static void putCursorToWriter(MetaCursor cursor, PrintWriter writer)
            throws StatisticsException {
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
                            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                                    "failed to write response to client because of io exception");
                        }
                        count = 0;
                    }
                }
            }
            writer.write("]");
            if (writer.checkError()) {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "failed to write response to client because of ioexception");
            }
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR, "traverse cursor failed",
                    e);
        }
        finally {
            cursor.close();
            writer.flush();
        }
    }

    public static PrintWriter getWriter(HttpServletResponse response) throws StatisticsException {
        try {
            return response.getWriter();
        }
        catch (IOException e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR, "Failed to get writer", e);
        }
    }
}
