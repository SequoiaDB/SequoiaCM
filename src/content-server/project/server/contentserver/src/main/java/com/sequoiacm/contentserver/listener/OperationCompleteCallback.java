package com.sequoiacm.contentserver.listener;

public interface OperationCompleteCallback {
    public static final OperationCompleteCallback EMPTY_CALLBACK = new OperationCompleteCallback() {
        @Override
        public void onComplete() {
        }
    };

    public void onComplete();
}
