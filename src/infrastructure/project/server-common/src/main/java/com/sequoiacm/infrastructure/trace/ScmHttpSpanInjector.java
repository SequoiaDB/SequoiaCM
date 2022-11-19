package com.sequoiacm.infrastructure.trace;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanTextMap;
import org.springframework.cloud.sleuth.instrument.web.ZipkinHttpSpanInjector;

public class ScmHttpSpanInjector extends ZipkinHttpSpanInjector {

    @Override
    public void inject(Span span, SpanTextMap map) {
        if (!span.isExportable()) {
            // Span 不需要采样时，不进行请求头注入，以减少请求头占用
            return;
        }
        super.inject(span, map);
    }
}
