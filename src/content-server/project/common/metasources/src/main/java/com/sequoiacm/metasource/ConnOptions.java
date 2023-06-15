package com.sequoiacm.metasource;

public class ConnOptions {

    private int socketTimeout = 0;

    public ConnOptions(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    @Override
    public String toString() {
        return "ConnOptions{" + "socketTimeout=" + socketTimeout + '}';
    }
}
