package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OmDeltaStatistics {

    @JsonProperty("count_delta")
    private List<OmStatisticsInfo> countDelta;

    @JsonProperty("size_delta")
    private List<OmStatisticsInfo> sizeDelta;

    public List<OmStatisticsInfo> getCountDelta() {
        return countDelta;
    }

    public void setCountDelta(List<OmStatisticsInfo> countDelta) {
        sort(countDelta);
        this.countDelta = countDelta;
    }

    public List<OmStatisticsInfo> getSizeDelta() {
        return sizeDelta;
    }

    public void setSizeDelta(List<OmStatisticsInfo> sizeDelta) {
        sort(sizeDelta);
        this.sizeDelta = sizeDelta;
    }

    private void sort(List<OmStatisticsInfo> statisticsList) {
        Collections.sort(statisticsList, new Comparator<OmStatisticsInfo>() {

            @Override
            public int compare(OmStatisticsInfo o1, OmStatisticsInfo o2) {
                return (o1.getRecordTime().getTime() - o2.getRecordTime().getTime() >= 0) ? 1 : -1;
            }
        });
    }

}
