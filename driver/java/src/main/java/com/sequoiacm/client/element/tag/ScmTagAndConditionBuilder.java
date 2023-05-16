package com.sequoiacm.client.element.tag;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link ScmTagCondition}.
 */
public class ScmTagAndConditionBuilder {
    private List<ScmTagCondition> conditionList = new ArrayList<ScmTagCondition>();

    ScmTagAndConditionBuilder(List<ScmTagCondition> conditions) {
        for (ScmTagCondition condition : conditions) {
            if (condition != null) {
                conditionList.add(condition);
            }
        }
    }

    /**
     * Add a condition.
     * 
     * @param condition
     *            condition
     * @return this
     */
    public ScmTagAndConditionBuilder and(ScmTagCondition condition) {
        if (condition == null) {
            return this;
        }
        conditionList.add(condition);
        return this;
    }

    /**
     * Build a condition.
     * 
     * @return condition
     */
    public ScmTagCondition build() {
        BasicBSONList bsonList = new BasicBSONList();
        for (ScmTagCondition c : conditionList) {
            bsonList.add(c.getBsonObject());
        }
        return new ScmTagCondition(new BasicBSONObject("$and", bsonList));
    }
}
