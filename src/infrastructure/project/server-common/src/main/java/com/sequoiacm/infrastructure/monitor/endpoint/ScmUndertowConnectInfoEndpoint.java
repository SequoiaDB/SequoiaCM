package com.sequoiacm.infrastructure.monitor.endpoint;

import java.lang.reflect.Field;
import java.util.List;

import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainer;
import org.springframework.util.ReflectionUtils;

import com.sequoiacm.infrastructure.monitor.model.ScmConnectionInfo;

import io.undertow.Undertow;

public class ScmUndertowConnectInfoEndpoint extends ScmAbstractConnectionInfoEndpoint {

    private final EmbeddedWebApplicationContext applicationContext;

    private Undertow undertow;

    public ScmUndertowConnectInfoEndpoint(EmbeddedWebApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public ScmConnectionInfo invoke() {
        if (undertow == null) {
            initUndertow();
        }
        ScmConnectionInfo scmConnectionInfo = new ScmConnectionInfo();
        List<Undertow.ListenerInfo> listenerInfo = undertow.getListenerInfo();
        for (Undertow.ListenerInfo info : listenerInfo) {
            if (info.getProtcol().equals("http")) {
                scmConnectionInfo
                        .setConnectionCount(info.getConnectorStatistics().getActiveConnections());
                break;
            }
        }
        return scmConnectionInfo;
    }

    private void initUndertow() {
        UndertowEmbeddedServletContainer undertowEmbeddedServletContainer = (UndertowEmbeddedServletContainer) applicationContext
                .getEmbeddedServletContainer();
        Field field = ReflectionUtils.findField(UndertowEmbeddedServletContainer.class, "undertow");
        field.setAccessible(true);
        this.undertow = (Undertow) ReflectionUtils.getField(field,
                undertowEmbeddedServletContainer);
        field.setAccessible(false);
    }
}
