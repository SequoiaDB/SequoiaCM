package com.sequoiacm.test.ssh;

import com.sequoiacm.test.common.ShutdownHook;
import com.sequoiacm.test.common.ShutdownHookMgr;
import com.sequoiacm.test.module.HostInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class SshMgr {

    private Map<String, Queue<Ssh>> caches = new HashMap<>();
    private static volatile SshMgr INSTANCE;

    private SshMgr() {

    }

    public static SshMgr getInstance() {
        if (INSTANCE == null) {
            synchronized (SshMgr.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SshMgr();
                    ShutdownHookMgr.getInstance().addHook(new ShutdownHook(5) {
                        @Override
                        public void onShutdown() {
                            destroyInstance();
                        }
                    });
                }
            }
        }

        return INSTANCE;
    }

    public Ssh getSsh(HostInfo hostInfo) throws IOException {
        Ssh ssh;
        Queue<Ssh> sshQueue = caches.get(hostInfo.getHostname());
        if (sshQueue != null) {
            ssh = sshQueue.poll();
            if (ssh != null) {
                return ssh;
            }
        }
        return new Ssh(this, hostInfo.getHostname(), hostInfo.getPort(), hostInfo.getUser(),
                hostInfo.getPassword());
    }

    public static void destroyInstance() {
        if (INSTANCE != null) {
            synchronized (SshMgr.class) {
                if (INSTANCE != null) {
                    INSTANCE.close();
                    INSTANCE = null;
                }
            }
        }
    }

    synchronized void release(Ssh ssh) {
        Queue<Ssh> sshQueue = caches.get(ssh.getHost());
        if (sshQueue == null) {
            sshQueue = new LinkedList<>();
            caches.put(ssh.getHost(), sshQueue);
        }
        sshQueue.add(ssh);

    }

    public synchronized void close() {
        for (Map.Entry<String, Queue<Ssh>> cache : caches.entrySet()) {
            for (Ssh ssh : cache.getValue()) {
                ssh.disconnect();
            }
        }
    }
}
