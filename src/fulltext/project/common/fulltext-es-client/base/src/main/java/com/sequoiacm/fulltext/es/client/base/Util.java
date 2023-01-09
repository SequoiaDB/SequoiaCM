package com.sequoiacm.fulltext.es.client.base;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.common.FulltextDocDefine;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Util {

    public static List<EsSearchRes> parseEsScrollHits(BasicBSONList hitsArr) {
        if (hitsArr == null || hitsArr.size() <= 0) {
            return Collections.emptyList();
        }
        List<EsSearchRes> ret = new ArrayList<>();
        for (Object hit : hitsArr) {
            BSONObject hitBson = (BSONObject) hit;
            float score = BsonUtils.getNumberChecked(hitBson, "_score").floatValue();
            BSONObject source = BsonUtils.getBSONChecked(hitBson, "_source");
            String docId = BsonUtils.getStringChecked(hitBson, "_id");
            String fileId = BsonUtils.getStringChecked(source, FulltextDocDefine.FIELD_FILE_ID);
            String fileVersion = BsonUtils.getStringChecked(source,
                    FulltextDocDefine.FIELD_FILE_VERSION);

            List<String> highlightStrings = new ArrayList<>();
            BSONObject highLight = BsonUtils.getBSON(hitBson, "highlight");
            if (highLight != null) {
                for (Object value : highLight.toMap().values()) {
                    highlightStrings.addAll((Collection<? extends String>) value);
                }
            }
            ret.add(new EsSearchRes(docId, fileId, fileVersion, score, highlightStrings));
        }
        return ret;
    }
}
