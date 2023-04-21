package com.sequoiacm.contentserver.quota.msg;

public class BeginSyncMsg implements QuotaSyncMsg {
    private String type;
    private String name;
    private int syncRoundNumber;
    private long expireTime;

    public BeginSyncMsg(String type, String name, int syncRoundNumber, long expireTime) {
        this.type = type;
        this.name = name;
        this.syncRoundNumber = syncRoundNumber;
        this.expireTime = expireTime;
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

    public long getExpireTime() {
        return expireTime;
    }

    @Override
    public String toString() {
        return "BeginSyncMsg{" + "type='" + type + '\'' + ", name='" + name + '\''
                + ", syncRoundNumber=" + syncRoundNumber + ", expireTime=" + expireTime + '}';
    }
}
