package com.sequoiacm.deploy.ssh;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import com.sequoiacm.deploy.config.SshConfig;
import com.sequoiacm.deploy.module.HostInfo;

public class SshMgr {
    private Map<String, Queue<Ssh>> caches = new HashMap<>();
    private SshConfig sshConfig = new SshConfig();
    private static volatile SshMgr instance;

    public static SshMgr getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (SshMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new SshMgr();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                @Override
                public void run() {
                    SshMgr.destroyInstance();
                }
            }));
            return instance;
        }
    }

    public static void destroyInstance() {
        if (instance == null) {
            return;
        }
        synchronized (SshMgr.class) {
            if (instance == null) {
                return;
            }
            instance.close();
            instance = null;
        }
    }

    public synchronized Ssh getSsh(HostInfo host) throws IOException {
        Ssh ssh = null;
        Queue<Ssh> sshs = caches.get(host.getHostName());
        if (sshs != null) {
            ssh = sshs.poll();
            if (ssh != null) {
                return ssh;
            }
        }
        return new Ssh(this, host.getHostName(), host.getPort(), host.getUserName(),
                host.getPassword(), sshConfig);
    }

    synchronized void release(Ssh ssh) {
        Queue<Ssh> sshs = caches.get(ssh.getHost());
        if (sshs == null) {
            sshs = new LinkedList<>();
            caches.put(ssh.getHost(), sshs);
        }
        sshs.add(ssh);

    }

    public synchronized void close() {
        for (Entry<String, Queue<Ssh>> cache : caches.entrySet()) {
            for (Ssh ssh : cache.getValue()) {
                ssh.disconnect();
            }
        }
    }
}
