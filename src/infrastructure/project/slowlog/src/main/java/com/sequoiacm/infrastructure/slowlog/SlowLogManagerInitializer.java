package com.sequoiacm.infrastructure.slowlog;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

// AutoConfig by resources/META-INF/spring.factories
//存在 scm.slowlog.enabled 配置项时，初始化 SlowLogManager
public class SlowLogManagerInitializer implements SmartApplicationListener {

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            String property = ((ApplicationEnvironmentPreparedEvent) event).getEnvironment()
                    .getProperty("scm.slowlog.enabled");
            if (property != null) {
                SlowLogManager.init();
            }
        }
    }

    @Override
    public int getOrder() {
        /**
         * 1. 在 LoggingApplicationListener 后初始化，确保 SlowLogManager.init() 中的日志能正常输出
         * 2. 在 ScmConfClassScanner 之前初始化，否则 ScmConfClassScanner 会把原始class载入jvm，这里将无法执行类修改
         */
        return LoggingApplicationListener.DEFAULT_ORDER + 1;
    }
}
