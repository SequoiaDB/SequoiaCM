package com.sequoiacm.infrastructure.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostManager {
    private static final Logger logger = LoggerFactory.getLogger(HostManager.class);

    private static final String CAT_STAT_COMMAND = "cat /proc/stat";
    private static final String CAT_MEM_COMMAND = "cat /proc/meminfo";

    public static Map<String, Object> getStat() throws Exception {
        Map<String, Object> statInfo = new HashMap<String, Object>();
        Process pro = null;
        BufferedReader in = null;
        try {
            Runtime r = Runtime.getRuntime();
            pro = r.exec(CAT_STAT_COMMAND);
            in = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            String line = null;
            String idle, sys, user, other;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("cpu")) {
                    line = line.trim();
                    String[] temp = line.split("\\s+");
                    idle = temp[4];
                    sys = temp[3];
                    user = temp[1];
                    long count = Long.parseLong(temp[2]) + Long.parseLong(temp[5])
                            + Long.parseLong(temp[6]) + Long.parseLong(temp[7]);
                    other = String.valueOf(count);
                    statInfo.put("idle", idle);
                    statInfo.put("sys", sys);
                    statInfo.put("user", user);
                    statInfo.put("other", other);
                    break;
                }
            }

            return statInfo;
        }
        catch (Exception e) {
            logger.error("fail to get host stat:command={}", CAT_STAT_COMMAND);
            throw e;
        }
        finally {
            closeReaderSilence(in);
            closeProcessSilence(pro);
        }
    }

    private static void closeReaderSilence(Reader r) {
        if (null != r) {
            try {
                r.close();
            }
            catch (Exception e) {
                logger.warn("fail to close Reader", e);
            }
        }
    }

    private static void closeProcessSilence(Process p) {
        if (null != p) {
            try {
                p.destroy();
            }
            catch (Exception e) {
                logger.warn("fail to close Process", e);
            }
        }
    }

    public static Map<String, Object> getMemInfo() throws Exception {
        Map<String, Object> meminfo = new HashMap<String, Object>();
        Process pro = null;
        BufferedReader in = null;
        try {
            Runtime r = Runtime.getRuntime();
            pro = r.exec(CAT_MEM_COMMAND);
            in = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                String[] temp = line.split("\\s+");
                if (temp[0].startsWith("MemTotal")) {
                    meminfo.put("total_ram", temp[1]);
                }
                else if (temp[0].startsWith("MemFree")) {
                    meminfo.put("free_ram", temp[1]);
                }
                else if (temp[0].startsWith("SwapTotal")) {
                    meminfo.put("total_swap", temp[1]);
                }
                else if (temp[0].startsWith("SwapFree")) {
                    meminfo.put("free_swap", temp[1]);
                }
            }

            return meminfo;
        }
        catch (Exception e) {
            logger.error("fail to get host memory:commaon={}", CAT_MEM_COMMAND);
            throw e;
        }
        finally {
            closeReaderSilence(in);
            closeProcessSilence(pro);
        }
    }
}
