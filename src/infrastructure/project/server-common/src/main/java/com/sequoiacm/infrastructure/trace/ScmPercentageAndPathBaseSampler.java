package com.sequoiacm.infrastructure.trace;

import com.sequoiacm.infrastructure.common.ScmRequestAttributeDefine;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.sampler.PercentageBasedSampler;
import org.springframework.cloud.sleuth.sampler.SamplerProperties;

import java.util.HashSet;
import java.util.Set;

public class ScmPercentageAndPathBaseSampler implements Sampler {

    private static final Set<String> ignoreSpanNames = new HashSet<>();
    private static final String SERVICE_TRACE = "service-trace";

    static {
        ignoreSpanNames.add("health");
        ignoreSpanNames.add("http:/internal/v1/health");
        ignoreSpanNames.add("http:/service-center/eureka/apps/gateway");
        ignoreSpanNames.add("http:/service-center/eureka/apps/service-center");
        ignoreSpanNames.add("http:/service-center/eureka/apps/");
        ignoreSpanNames.add("http:/error");
    }
    private PercentageBasedSampler percentageBasedSampler;

    private ScmTraceConfig traceConfig;

    public ScmPercentageAndPathBaseSampler(ScmTraceConfig traceConfig) {
        this.traceConfig = traceConfig;
        SamplerProperties samplerProperties = new SamplerProperties();
        samplerProperties.setPercentage(traceConfig.getSamplePercentage() / 100f);
        percentageBasedSampler = new PercentageBasedSampler(samplerProperties);
    }

    @Override
    public boolean isSampled(Span span) {
        if (!traceConfig.isEnabled() || traceConfig.getSamplePercentage() == 0) {
            return false;
        }

        if (!inSampleServices()) {
            return false;
        }

        if (shouldIgnoreSpan(span)) {
            return false;
        }

        if (traceConfig.getSamplePercentage() == 100) {
            return true;
        }

        return percentageBasedSampler.isSampled(span);
    }

    private boolean shouldIgnoreSpan(Span span) {
        return ignoreSpanNames.contains(span.getName());
    }

    private boolean inSampleServices() {
        String requestService = null;
        if (ScmTracePreFilter.getCurrentRequest() != null) {
            requestService = (String) ScmTracePreFilter.getCurrentRequest()
                    .getAttribute(ScmRequestAttributeDefine.FORWARD_SERVICE_ATTRIBUTE);
        }
        if (requestService == null) {
            return false;
        }
        return !requestService.equals(SERVICE_TRACE)
                && traceConfig.isSampledService(requestService);
    }
}
