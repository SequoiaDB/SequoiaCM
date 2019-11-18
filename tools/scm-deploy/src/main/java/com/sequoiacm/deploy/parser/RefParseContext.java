package com.sequoiacm.deploy.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;

public class RefParseContext {
    private Map<String, List<BSONObject>> processedSeactionRecords;
    private Map<String, List<Map<String, String>>> srcSeactionRecords;
    private ArrayList<String> refrencePath;

    public RefParseContext(Map<String, List<Map<String, String>>> srcSeactionRecords,
            Map<String, List<BSONObject>> processedSeactionRecords) {
        this.srcSeactionRecords = srcSeactionRecords;
        this.processedSeactionRecords = processedSeactionRecords;
        this.refrencePath = new ArrayList<>();
    }

    public Map<String, List<BSONObject>> getProcessedSeactionRecords() {
        return processedSeactionRecords;
    }

    public Map<String, List<Map<String, String>>> getSrcSeactionRecords() {
        return srcSeactionRecords;
    }

    public void addReferenceToPath(String seactionName) {
        refrencePath.add(seactionName);
    }

    public void removeReferenceFromPath(String seactionName) {
        refrencePath.remove(seactionName);
    }

    public List<String> getReferencePath() {
        return refrencePath;
    }

    public boolean isCircularReference(String seactionName) {
        return refrencePath.contains(seactionName);
    }

}
