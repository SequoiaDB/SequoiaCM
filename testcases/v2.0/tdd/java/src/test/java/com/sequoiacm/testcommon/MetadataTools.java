package com.sequoiacm.testcommon;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmDoubleRule;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;

public class MetadataTools extends ScmTestMultiCenterBase {

    public static List<ScmId> prepareAttribute(ScmWorkspace ws) throws ScmException {
        List<ScmId> attrIds = new ArrayList<>();
        
        ScmAttributeConf attrConf = new ScmAttributeConf().setName("ID_NUM")
                .setRequired(true).setType(AttributeType.STRING).setCheckRule(new ScmStringRule(18));
        ScmId attrId = ScmFactory.Attribute.createInstance(ws, attrConf).getId();
        attrIds.add(attrId);
        
        attrConf.setName("ID_NAME").setRequired(true).setType(AttributeType.STRING)
                .setCheckRule(null);
        attrId = ScmFactory.Attribute.createInstance(ws, attrConf).getId();
        attrIds.add(attrId);
        
        attrConf.setName("FILE_TYPE").setRequired(true).setType(AttributeType.STRING)
                .setCheckRule(null);
        attrId = ScmFactory.Attribute.createInstance(ws, attrConf).getId();
        attrIds.add(attrId);
        
        attrConf.setName("ID_ADD").setRequired(false).setType(AttributeType.STRING)
                .setCheckRule(null);
        attrId = ScmFactory.Attribute.createInstance(ws, attrConf).getId();
        attrIds.add(attrId);
        
        attrConf.setName("DATE_BEGIN").setRequired(false).setType(AttributeType.DATE)
                .setCheckRule(null);
        attrId = ScmFactory.Attribute.createInstance(ws, attrConf).getId();
        attrIds.add(attrId);
        
        attrConf.setName("DATE_END").setRequired(false).setType(AttributeType.DATE)
                .setCheckRule(null);
        attrId = ScmFactory.Attribute.createInstance(ws, attrConf).getId();
        attrIds.add(attrId);
        
        attrConf.setName("TIME_NUM").setRequired(false).setType(AttributeType.INTEGER)
                .setCheckRule(new ScmIntegerRule(100, 1000));
        attrId = ScmFactory.Attribute.createInstance(ws, attrConf).getId();
        attrIds.add(attrId);
        
        ScmDoubleRule doubleRule = new ScmDoubleRule();
        doubleRule.setMinimum(10000.0);
        attrConf.setName("HANDER_PRICE").setRequired(false).setType(AttributeType.DOUBLE)
                .setCheckRule(doubleRule);
        attrId = ScmFactory.Attribute.createInstance(ws, attrConf).getId();
        attrIds.add(attrId);
        
        attrConf.setName("IS_ENABLE").setRequired(false).setType(AttributeType.BOOLEAN)
                .setCheckRule(null);
        attrId = ScmFactory.Attribute.createInstance(ws, attrConf).getId();
        attrIds.add(attrId);
        
        return attrIds;
    }
    
    public static List<ScmId> prepareAttrAndAttachToClass(ScmWorkspace ws, ScmClass... scmClass)
            throws ScmException {
        List<ScmId> attrIds = prepareAttribute(ws);
        for (ScmId attrId : attrIds) {
            for (ScmClass cls : scmClass) {
                cls.attachAttr(attrId);
            }
        }
        return attrIds;
    }
}
