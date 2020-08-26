package com.sequoiacm.contentserver.metadata;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metadata.impl.WSClassInfo;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.MetadataAttr;
import com.sequoiacm.contentserver.model.MetadataClass;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;

public class MetaDataManager {
    private static final Logger logger = LoggerFactory.getLogger(MetaDataManager.class);
    private static MetaDataManager instance = new MetaDataManager();

    // wsName map MetaData
    private Map<String, WSClassInfo> metadataInfo = new ConcurrentHashMap<>();

    private MetaDataManager() {
    }

    public static MetaDataManager getInstence() {
        return instance;
    }

    public static void reload() throws ScmServerException {
        synchronized (MetaDataManager.class) {
            try {
                MetaDataManager newMdMgr = new MetaDataManager();
                newMdMgr.initMetaDataInfomation();
                instance = newMdMgr;
            }
            catch (ScmServerException e) {
                e.resetError(ScmError.SERVER_RELOAD_CONF_FAILED);
                throw e;
            }
            catch (Exception e) {
                throw new ScmServerException(ScmError.SERVER_RELOAD_CONF_FAILED,
                        "reload metadata conf failed", e);
            }
        }
    }

    public void reloadMetaDataByWsName(String wsName) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmMetaService metaService = contentServer.getMetaService();
        logger.info("refresh metaData cache:wsName={}", wsName);
        WSClassInfo wsClassInfo = queryWorkspaceClassInfo(metaService, wsName);
        metadataInfo.put(wsName, wsClassInfo);
    }

    public void removeMetaDataByWsName(String wsName) {
        metadataInfo.remove(wsName);
    }

    public void initMetaDataInfomation() throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmMetaService metaService = contentServer.getMetaService();
        metadataInfo.clear();
        List<String> wsNames = contentServer.getWorkspaceNames();
        try {
            for (String wsName : wsNames) {
                // get class by workspace
                WSClassInfo wsClassInfo = queryWorkspaceClassInfo(metaService, wsName);
                metadataInfo.put(wsName, wsClassInfo);
            }
        }
        catch (Exception e) {
            throw new ScmSystemException("failed to init metadata", e);
        }

    }

    private WSClassInfo queryWorkspaceClassInfo(ScmMetaService metaService, String wsName)
            throws ScmServerException {
        List<MetadataClass> classList = metaService.listClassInfo(wsName, null);
        WSClassInfo wsClassInfo = new WSClassInfo();
        for (MetadataClass metaClass : classList) {
            ClassInfo classInfo = new ClassInfo(metaClass.getId());
            List<MetadataAttr> attrList = metaService.getAttrListForClass(wsName,
                    metaClass.getId());
            for (MetadataAttr metaAttr : attrList) {
                AttrInfo attrInfo = AttrManager.getInstance().createAttrInfo(metaAttr);
                classInfo.addAttrInfo(attrInfo);
            }

            wsClassInfo.addClassInfo(classInfo);
        }
        return wsClassInfo;
    }

    public void checkProperty(String wsName, String classId, BSONObject oneProperty)
            throws ScmServerException {
        ClassInfo classInfo = getClassInfo(wsName, classId);

        Set<String> keySet = oneProperty.keySet();
        if (keySet.size() != 1) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT,
                    "only support one property:oneProperty=" + oneProperty);
        }

        String key = null;
        Object value = null;
        for (String tmpKey : keySet) {
            key = tmpKey;
            value = oneProperty.get(key);
        }

        try {
            check(classInfo, key, value);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR, "checkProperty failed:wsName="
                    + wsName + ",classId=" + classId + "onePropery=" + oneProperty, e);
        }

        // convert the string representation of Boolean to Boolean
        if (classInfo.getAttrInfo(key).getType() == AttributeType.BOOLEAN) {
            oneProperty.put(key, Boolean.parseBoolean(String.valueOf(oneProperty.get(key))));
        }
    }

    private ClassInfo getClassInfo(String wsName, String classId) throws ScmServerException {
        WSClassInfo wsInfo = metadataInfo.get(wsName);
        if (null == wsInfo) {
            throw new ScmServerException(ScmError.METADATA_CLASS_NOT_EXIST,
                    "class is not exist:wsName=" + wsName + ",classId=" + classId);
        }

        ClassInfo classInfo = wsInfo.getClassInfo(classId);
        if (null == classInfo) {
            throw new ScmServerException(ScmError.METADATA_CLASS_NOT_EXIST,
                    "class is not exist:wsName=" + wsName + ",classId=" + classId);
        }

        return classInfo;
    }

    private void check(ClassInfo classInfo, String key, Object value) throws ScmServerException {
        AttrInfo attrInfo = classInfo.getAttrInfo(key);
        if (null == attrInfo) {
            throw new ScmServerException(ScmError.METADATA_ATTR_NOT_IN_CLASS,
                    "attr is not exist in class:classId=" + classInfo.getId() + ",attrName=" + key);
        }

        if (!attrInfo.check(value)) {
            throw new ScmServerException(ScmError.METADATA_CHECK_ERROR, "key=" + key + ",value="
                    + value + ",type=" + getObjType(value) + ",rule=" + attrInfo.getRule());
        }
    }

    private String getObjType(Object obj) {
        if (null != obj) {
            return obj.getClass().getSimpleName();
        }
        else {
            return "null";
        }

    }

    public void checkPropeties(String wsName, String classId, BSONObject properties)
            throws ScmServerException {
        if (null == classId) {
            if (null != properties) {
                throw new ScmServerException(ScmError.METADATA_CHECK_ERROR,
                        "classId=" + classId + ",properties=" + properties);
            }

            return;
        }

        ClassInfo classInfo = getClassInfo(wsName, classId);
        Set<String> requiredKeys = classInfo.getRequiredAttrName();
        if (null == properties || properties.isEmpty()) {
            // in case not property values, requiredKeys must be empty
            if (!requiredKeys.isEmpty()) {
                throw new ScmServerException(ScmError.METADATA_CHECK_ERROR,
                        "classInfo have required attrs while properties is null");
            }

            return;
        }

        Set<String> propertyKeys = properties.keySet();
        if (!propertyKeys.containsAll(requiredKeys)) {
            throw new ScmServerException(ScmError.METADATA_CHECK_ERROR,
                    "required attrInfo is not set:requiredKeys=" + requiredKeys);
        }

        // check all properties
        for (String key : propertyKeys) {
            check(classInfo, key, properties.get(key));
            // convert the string representation of Boolean to Boolean
            if (classInfo.getAttrInfo(key).getType() == AttributeType.BOOLEAN) {
                properties.put(key, Boolean.parseBoolean(String.valueOf(properties.get(key))));
            }
        }
    }

    public void validateKeyFormat(String key, String whosKey) throws ScmServerException {
        if (key.isEmpty() || key.startsWith("$") || key.contains(".")) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR, whosKey
                    + " key cannot start with '$', cannot contains '.', cannot be empty: field="
                    + key);
        }
    }

    /**
     *
     * @param oneProperty
     *            {class_properties.key:value}
     * @param outerKey
     *            class_properties
     * @return {key:value}
     */
    public BSONObject formatPropertyKey(BSONObject oneProperty, String outerKey) {
        String pattern = outerKey + ".";
        String newKey = "";
        for (String key : oneProperty.keySet()) {
            if (key.startsWith(pattern)) {
                newKey = key.substring(pattern.length());

                return new BasicBSONObject(newKey, oneProperty.get(key));
            }

            break;
        }

        return null;
    }

    public boolean isUpdateSingleClassProperty(BSONObject updator, String classPropertyKey) {
        // Just check the first property in 'updator'
        Iterator<String> iterator = updator.keySet().iterator();
        if (iterator.hasNext()) {
            return iterator.next().startsWith(classPropertyKey + ".");
        }

        return false;
    }

    public void checkUpdateProperties(String wsName, BSONObject updator, String classIdKey,
            String propertiesKey, String oldClassId) throws ScmServerException {
        if (updator.containsField(classIdKey)) {
            String classId = (String) updator.get(classIdKey);
            checkPropeties(wsName, classId, (BSONObject) updator.get(propertiesKey));

            if (!updator.containsField(propertiesKey)) {
                updator.put(propertiesKey, new BasicBSONObject());
            }
        }
        else {
            if (updator.containsField(propertiesKey)) {
                // can not only update FIELD_CLFILE_PROPERTIES
                throw new ScmInvalidArgumentException(
                        "can not only update properties:properties=" + updator);
            }

            // updator = {class_properties.key:value}
            // singleProperty = {key:value}
            BSONObject singleProperty = formatPropertyKey(updator, propertiesKey);
            if (null != singleProperty) {
                checkProperty(wsName, oldClassId, singleProperty);
                // replace the key value in 'updator' with the key value in
                // 'singleProperty'
                // updator.put(class_properties.key, singleProperty.get(key))
                updator.put(updator.keySet().iterator().next(),
                        singleProperty.get(singleProperty.keySet().iterator().next()));
            }
        }
    }
}
