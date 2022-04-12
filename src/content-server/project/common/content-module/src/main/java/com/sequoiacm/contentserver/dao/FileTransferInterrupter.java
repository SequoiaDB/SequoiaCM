package com.sequoiacm.contentserver.dao;

public interface FileTransferInterrupter {
    public boolean isInterrupted(int increaseLen);

    public void resetLen();
}
