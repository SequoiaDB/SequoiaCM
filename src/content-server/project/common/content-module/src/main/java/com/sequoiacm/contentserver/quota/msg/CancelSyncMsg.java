package com.sequoiacm.contentserver.quota.msg;

public class CancelSyncMsg implements QuotaSyncMsg {

    private String type;
    private String name;
    private int syncRoundNumber;

    public CancelSyncMsg(String type, String name, int syncRoundNumber) {
        this.type = type;
        this.name = name;
        this.syncRoundNumber = syncRoundNumber;
    }

    @Override
    public int getSyncRoundNumber() {
        return syncRoundNumber;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CancelSyncMsg{" + "type='" + type + '\'' + ", name='" + name + '\''
                + ", syncRoundNumber=" + syncRoundNumber + '}';
    }
}
