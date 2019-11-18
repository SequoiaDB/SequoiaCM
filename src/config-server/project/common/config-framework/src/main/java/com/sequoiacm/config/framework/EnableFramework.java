package com.sequoiacm.config.framework;

import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.sequoiacm.config.framework.workspace,com.sequoiacm.config.framework.site,com.sequoiacm.config.framework.node")
public @interface EnableFramework {

}
