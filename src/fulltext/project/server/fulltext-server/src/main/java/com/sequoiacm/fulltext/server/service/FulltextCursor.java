package com.sequoiacm.fulltext.server.service;

import java.io.PrintWriter;

public interface FulltextCursor {
    public boolean hasNext() throws Exception;

    public void writeNextToWriter(PrintWriter writer) throws Exception;

    public void close();
}
