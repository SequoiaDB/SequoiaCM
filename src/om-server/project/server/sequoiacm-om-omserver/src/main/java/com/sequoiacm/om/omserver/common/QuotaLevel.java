package com.sequoiacm.om.omserver.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum QuotaLevel {

    // 区间是 左闭右开
    LOW("low", 0, 50),
    MEDIUM("medium", 50, 80),
    HIGH("high", 80, 100),
    EXCEEDED("exceeded", 100, Integer.MAX_VALUE);

    private String name;
    private int min;
    private int max;

    private static List<QuotaLevel> sortedQuotaLevels;

    static {
        sortedQuotaLevels = new ArrayList<>(Arrays.asList(QuotaLevel.values()));
        Collections.sort(sortedQuotaLevels, new Comparator<QuotaLevel>() {
            @Override
            public int compare(QuotaLevel o1, QuotaLevel o2) {
                return o2.getMax() - o1.getMax();
            }
        });
    }

    QuotaLevel(String name, int min, int max) {
        this.name = name;
        this.min = min;
        this.max = max;
    }

    public static QuotaLevel getByName(String name) {
        for (QuotaLevel level : QuotaLevel.values()) {
            if (level.name.equals(name)) {
                return level;
            }
        }
        return null;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    public String getName() {
        return name;
    }

    public static QuotaLevel getMatchedQuotaLevel(long maxSizeBytes, long usedSizeBytes,
            long maxObjects, long usedObjects) {
        for (QuotaLevel quotaLevel : sortedQuotaLevels) {
            if (quotaLevel.isMatch(maxSizeBytes, usedSizeBytes, maxObjects, usedObjects)) {
                return quotaLevel;
            }
        }
        throw new IllegalArgumentException("no matched quotaLevel");
    }

    private boolean isMatch(long maxSizeBytes, long usedSizeBytes, long maxObjects,
            long usedObjects) {
        boolean objectsPercentMatch = false;
        if (maxObjects >= 0) {
            int usedPercent = maxObjects == 0 ? 100 : (int) (usedObjects * 100 / maxObjects);
            if (usedPercent >= min && usedPercent < max) {
                objectsPercentMatch = true;
            }
        }
        boolean sizePercentMatch = false;
        if (maxSizeBytes >= 0) {
            int usedPercent = maxSizeBytes == 0 ? 100 : (int) (usedSizeBytes * 100 / maxSizeBytes);
            if (usedPercent >= min && usedPercent < max) {
                sizePercentMatch = true;
            }
        }
        return objectsPercentMatch || sizePercentMatch;
    }
}
