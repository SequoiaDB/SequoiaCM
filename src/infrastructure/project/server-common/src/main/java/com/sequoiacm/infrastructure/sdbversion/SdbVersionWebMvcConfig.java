package com.sequoiacm.infrastructure.sdbversion;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

public class SdbVersionWebMvcConfig extends WebMvcConfigurerAdapter {

    private final SdbVersionInterceptor interceptor;

    public SdbVersionWebMvcConfig(SdbVersionInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        super.addInterceptors(registry);
        registry.addInterceptor(interceptor);
    }
}
