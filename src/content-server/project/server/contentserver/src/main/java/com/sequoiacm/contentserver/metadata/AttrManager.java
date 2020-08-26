package com.sequoiacm.contentserver.metadata;

import org.bson.BSONObject;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.metadata.impl.BooleanAttrInfo;
import com.sequoiacm.contentserver.metadata.impl.DateAttrInfo;
import com.sequoiacm.contentserver.metadata.impl.DoubleAttrInfo;
import com.sequoiacm.contentserver.metadata.impl.IntegerAttrInfo;
import com.sequoiacm.contentserver.metadata.impl.StringAttrInfo;
import com.sequoiacm.contentserver.model.MetadataAttr;

public class AttrManager {

    private static final AttrManager INSTANCE = new AttrManager();
    
    private AttrManager() {
    }
    
    public static AttrManager getInstance() {
        return INSTANCE;
    }

    public AttrInfo createAttrInfo(MetadataAttr attr) throws ScmServerException {
        AttributeType type = attr.getType();
        BSONObject ruleBson = attr.getCheckRule();
        String name = attr.getName();
        boolean required = attr.isRequired();
        
        switch (type) {
            case INTEGER:
                AttrRule intRule = new IntegerRule(ruleBson);
                return new IntegerAttrInfo(name, required, intRule);
                
            case STRING:
                AttrRule strRule = new StringRule(ruleBson);
                return new StringAttrInfo(name, required, strRule);

            case DOUBLE:
                AttrRule doubleRule = new DoubleRule(ruleBson);
                return new DoubleAttrInfo(name, required, doubleRule);

            case BOOLEAN:
                AttrRule boolRule = new BooleanRule(ruleBson);
                return new BooleanAttrInfo(name, required, boolRule);
                
            case DATE:
                AttrRule dateRule = new DateRule(ruleBson);
                return new DateAttrInfo(name, required, dateRule);
                
            default:
                throw new ScmInvalidArgumentException("unsupport type: " + type);
        }
    }
    
    public void validType(String type) throws ScmInvalidArgumentException {
        AttributeType attrType = AttributeType.getType(type);
        if (attrType == AttributeType.UNKOWN_TYPE) {
            throw new ScmInvalidArgumentException("unsupport type: " + type);
        }
    }

    public void validCheckRule(AttributeType type, BSONObject rule)
            throws ScmServerException {
        switch (type) {
            case STRING:
                new StringRule(rule);
                break;

            case INTEGER:
                new IntegerRule(rule);
                break;

            case DOUBLE:
                new DoubleRule(rule);
                break;

            case BOOLEAN:
                new BooleanRule(rule);
                break;
                
            case DATE:
                new DateRule(rule);
                break;
                
            default:
                throw new ScmInvalidArgumentException("unsupport type: " + type);
        }
    }
}
