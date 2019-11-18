package com.sequoiacm.contentserver.metadata.impl;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.contentserver.metadata.AttrRule;
import com.sequoiacm.contentserver.metadata.DateRule;

public class DateAttrInfo extends AttrInfoBase {
    
    private static final Logger logger = LoggerFactory.getLogger(DateAttrInfo.class);
    
    private DateTimeFormatter formatter;
    private DateRule dateRule;

    public DateAttrInfo(String name, boolean isRequired, AttrRule rule) {
        super(name, isRequired);
        this.dateRule = (DateRule) rule;
        this.formatter = DateTimeFormat.forPattern(dateRule.getFormat());
    }
    
    @Override
    public AttributeType getType() {
        return AttributeType.DATE;
    }

    @Override
    public boolean check(Object o) {
        if (null == o) {
            return false;
        }

        if (!(o instanceof String)) {
            return false;
        }
        
        // limit 3 digits (.SSS)
        String dateStr = (String) o;
        String[] split = dateStr.split("\\.");
        if (split.length != 2 || split[1].length() != 3) {
            return false;
        }
        
        try {
            DateTime.parse((String) o, formatter);
        }
        catch (Exception e) {
            logger.warn("check date attr failed: value={}", o, e);
            return false;
        }
        
        return true;
    }

    @Override
    public String getRule() {
        return this.dateRule.toStringFormat();
    }
}
