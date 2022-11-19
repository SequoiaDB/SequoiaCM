package com.sequoiacm.cloud.servicetrace;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.stereotype.Component;

@Component
public class ScmEmbeddedServletContainerCustomizer implements EmbeddedServletContainerCustomizer {

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        if (container instanceof UndertowEmbeddedServletContainerFactory) {

            UndertowEmbeddedServletContainerFactory factory = (UndertowEmbeddedServletContainerFactory) container;
            factory.addBuilderCustomizers(new UndertowBuilderCustomizer() {
                @Override
                public void customize(Undertow.Builder builder) {
                    // 开启 Undertow 的统计功能，用于获取连接数
                    builder.setServerOption(UndertowOptions.ENABLE_STATISTICS, true);
                }
            });
        }
    }
}
