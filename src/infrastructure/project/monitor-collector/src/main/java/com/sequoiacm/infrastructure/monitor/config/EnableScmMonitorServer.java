package com.sequoiacm.infrastructure.monitor.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.sequoiacm.infrastructure.monitor.controller.MonitorCollectorController;
import com.sequoiacm.infrastructure.monitor.service.impl.MonitorCollectorServiceImpl;

@Import({ MonitorCollectorController.class, MonitorCollectorServiceImpl.class })
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableScmMonitorServer {

}
