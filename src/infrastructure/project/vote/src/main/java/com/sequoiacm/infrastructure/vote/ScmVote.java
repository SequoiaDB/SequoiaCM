package com.sequoiacm.infrastructure.vote;

public interface ScmVote {
    public abstract void startVote() throws Exception;
    public abstract String getId();
    public abstract boolean isLeader();
    public abstract String getLeader();
    public abstract void close();
}
