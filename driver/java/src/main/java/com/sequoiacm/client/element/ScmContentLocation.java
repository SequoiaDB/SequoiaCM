package com.sequoiacm.client.element;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Content Location class
 * For more information, you can cast to subclasses such as ScmS3ObjLocation.
 */
public class ScmContentLocation {

    private int site;
    private ScmType.DatasourceType type;
    private List<String> urls;

    public ScmContentLocation(BSONObject location) throws ScmException {
        this.site = BsonUtils.getIntegerChecked(location, FieldName.ContentLocation.FIELD_SITE);
        String typeStr = BsonUtils.getStringChecked(location, FieldName.ContentLocation.FIELD_TYPE);
        this.type = ScmType.DatasourceType.getDatasourceType(typeStr);
        BasicBSONList urlBsonList = BsonUtils.getArrayChecked(location,
                FieldName.ContentLocation.FIELD_URLS);
        this.urls = toUrlList(urlBsonList);
    }

    private List<String> toUrlList(BasicBSONList bsonList) {
        List<String> list = new ArrayList<String>();
        for (Object o : bsonList) {
            list.add((String) o);
        }
        return list;
    }

    /**
     * Gets the site id.
     *
     * @return site id.
     */
    public int getSite() {
        return site;
    }

    /**
     * Gets the datasource type.
     *
     * @return datasource type.
     */
    public ScmType.DatasourceType getType() {
        return type;
    }

    /**
     * Gets the datasource url list.
     *
     * @return datasource url list.
     */
    public List<String> getUrls() {
        return urls;
    }

    /**
     * Gets the full data(include subclass fields).
     *
     * @return full data map.
     */
    public Map<String, Object> getFullData() {
        Map<String, Object> fullData = new HashMap<String, Object>();
        fullData.put(FieldName.ContentLocation.FIELD_SITE, this.site);
        fullData.put(FieldName.ContentLocation.FIELD_TYPE, this.type.getType());
        fullData.put(FieldName.ContentLocation.FIELD_URLS, this.urls);
        return fullData;
    }

}
