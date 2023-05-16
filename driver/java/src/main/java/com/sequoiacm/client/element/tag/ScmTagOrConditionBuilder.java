package com.sequoiacm.client.element.tag;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link ScmTagCondition}.
 */
public class ScmTagOrConditionBuilder {
    private List<ScmTagCondition> conditionList = new ArrayList<ScmTagCondition>();

    ScmTagOrConditionBuilder(List<ScmTagCondition> conditions) {
        for (ScmTagCondition condition : conditions) {
            if (condition != null) {
                conditionList.add(condition);
            }
        }
    }

    /**
     * Or condition.
     * 
     * @param condition
     *            condition
     * @return this
     */
    public ScmTagOrConditionBuilder or(ScmTagCondition condition) {
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
        return new ScmTagCondition(new BasicBSONObject("$or", bsonList));
    }
}
