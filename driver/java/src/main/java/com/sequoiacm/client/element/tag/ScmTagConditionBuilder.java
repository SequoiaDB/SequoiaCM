// 屏蔽标签功能：SEQUOIACM-1411
//package com.sequoiacm.client.element.tag;
//
//import org.bson.BSONObject;
//
//import java.util.Arrays;
//
///**
// * Builder for {@link ScmTagCondition}.
// */
//public class ScmTagConditionBuilder {
//    /**
//     * Create a builder.
//     *
//     * @return builder。
//     */
//    public static ScmTagConditionBuilder builder() {
//        return new ScmTagConditionBuilder();
//    }
//
//    private ScmTagConditionBuilder() {
//    }
//
//    /**
//     * Create a custom tag condition builder.
//     *
//     * @return a custom tag condition builder.
//     */
//    public ScmCustomTagConditionBuilder customTag() {
//        return new ScmCustomTagConditionBuilder();
//    }
//
//    /**
//     * Create a tag condition builder.
//     *
//     * @return a tag condition builder.
//     */
//    public ScmTagsConditionBuilder tags() {
//        return new ScmTagsConditionBuilder();
//    }
//
//    /**
//     * Create a 'and' condition builder.
//     *
//     * @param conditions
//     *            sub condition.
//     * @return a 'and' condition builder.
//     */
//    public ScmTagAndConditionBuilder and(ScmTagCondition... conditions) {
//        return new ScmTagAndConditionBuilder(Arrays.asList(conditions));
//    }
//
//    /**
//     * Create a 'or' condition builder.
//     *
//     * @param conditions
//     *            sub condition.
//     * @return a 'or' condition builder.
//     */
//    public ScmTagOrConditionBuilder or(ScmTagCondition... conditions) {
//        return new ScmTagOrConditionBuilder(Arrays.asList(conditions));
//    }
//
//    /**
//     * Create tag condition from bson.
//     *
//     * @param bson
//     *            bson
//     * @return tag condition
//     */
//    public ScmTagCondition fromBson(BSONObject bson) {
//        return new ScmTagCondition(bson);
//    }
//}
