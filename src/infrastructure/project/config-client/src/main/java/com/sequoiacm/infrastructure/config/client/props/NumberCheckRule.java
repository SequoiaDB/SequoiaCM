package com.sequoiacm.infrastructure.config.client.props;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;

public class NumberCheckRule implements PropCheckRule {
    private static final Logger logger = LoggerFactory.getLogger(NumberCheckRule.class);
    private final ConversionService conversionService;
    private long min = Long.MIN_VALUE;
    private long max = Long.MAX_VALUE;

    public NumberCheckRule(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public NumberCheckRule(ConversionService conversionService, Long max, Long min) {
        this(conversionService);
        if (max != null) {
            this.max = max;
        }
        if (min != null) {
            this.min = min;
        }
    }

    @Override
    public boolean checkValue(String value) {
        Number v;
        try {
            v = conversionService.convert(value, Number.class);
        } catch (ConversionFailedException e) {
            logger.warn("failed to convert value to number: value={}", value, e);
            return false;
        }

        if (v.longValue() <= max && v.longValue() >= min) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    @Override
    public String toString() {
        return "[number value, max=" + max + ",min" + min + "]";
    }
}
