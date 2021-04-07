package com.sequoiacm.cloud.gateway.statistics.decider;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import com.sequoiacm.cloud.gateway.statistics.config.ScmStatisticsConfig;

@RefreshScope
@Component
public class ScmStatisticsDeciderGroup {
    private static final Logger logger = LoggerFactory.getLogger(ScmStatisticsDeciderGroup.class);
    private List<IDecider> deciders;

    @Autowired
    public ScmStatisticsDeciderGroup(ScmStatisticsConfig statisticsConfig,
            List<IDecider> allDeciders) {
        deciders = new ArrayList<>();
        if (statisticsConfig.getTypes() == null || statisticsConfig.getTypes().size() <= 0) {
            return;
        }
        if (allDeciders == null || allDeciders.size() <= 0) {
            return;
        }
        for (IDecider decider : allDeciders) {
            if (statisticsConfig.getTypes().contains(decider.getType())
                    || statisticsConfig.getTypes().contains(decider.getType().toLowerCase())) {
                logger.info("register statistics decider:{}", decider);
                deciders.add(decider);
            }
        }
    }

    public ScmStatisticsDecisionResult decide(HttpServletRequest request) {
        for (IDecider decider : deciders) {
            ScmStatisticsDecisionResult result = decider.decide(request);
            if (result != null) {
                logger.debug("decide request: request={} {} , result={} ", request.getMethod(),
                        request.getRequestURI(), result);
                return result;
            }
        }
        logger.debug("decide request: request={} {} , result=unrecognized", request.getMethod(),
                request.getRequestURI());
        return new ScmStatisticsDecisionResult(false, null);
    }
}
