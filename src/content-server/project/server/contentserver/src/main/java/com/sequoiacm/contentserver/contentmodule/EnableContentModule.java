package com.sequoiacm.contentserver.contentmodule;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(ContentModuleAutoConfig.class)
public @interface EnableContentModule {

}
