package com.sequoiacm.perf.tool;

import com.sequoiacm.perf.vo.RecordVo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Recorder {

    private static Recorder recorder = new Recorder();

    private Recorder() {
    }

    public static Recorder getInstance() {
        return recorder;
    }

    private Map<String, RecordVo> recordMap = new ConcurrentHashMap<>();

    public void record(String threadName, RecordVo record) {
        recordMap.put(threadName, record);
    }

    public Map<String, RecordVo> getRecordMap() {
        return recordMap;
    }




}
