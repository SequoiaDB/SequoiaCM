package com.sequoiacm.contentserver.service;

import com.sequoiacm.exception.ScmServerException;

public interface QuotaSyncMsgNotifyService {
    long notifyBeginSync(String type, String name, int syncRoundNumber, long expireTime)
            throws ScmServerException;

    void notifySetAgreementTimeMsg(String type, String name, long agreementTime,
            int syncRoundNumber)
            throws ScmServerException;

    void notifyCancelSync(String type, String name, int syncRoundNumber) throws ScmServerException;

    void notifyFinishSync(String type, String name, int syncRoundNumber) throws ScmServerException;
}
