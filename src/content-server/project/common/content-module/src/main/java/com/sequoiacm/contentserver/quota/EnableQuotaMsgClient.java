package com.sequoiacm.contentserver.quota;

import com.sequoiacm.contentserver.controller.InternalQuotaController;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(InternalQuotaController.class)
public @interface EnableQuotaMsgClient {
}
