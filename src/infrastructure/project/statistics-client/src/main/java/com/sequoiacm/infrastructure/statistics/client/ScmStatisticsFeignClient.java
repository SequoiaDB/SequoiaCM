package com.sequoiacm.infrastructure.statistics.client;

import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsRawData;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/internal/v1/")
public interface ScmStatisticsFeignClient {

    @PostMapping("/statistics/raw_data/{type}")
    void reportRawData(@PathVariable("type") String type,
            @RequestBody List<ScmStatisticsRawData> rawDataList) throws ScmFeignException;

}
