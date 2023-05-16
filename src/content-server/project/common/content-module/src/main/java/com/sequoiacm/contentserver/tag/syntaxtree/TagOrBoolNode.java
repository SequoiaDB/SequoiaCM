package com.sequoiacm.contentserver.tag.syntaxtree;

import com.sequoiacm.exception.ScmServerException;
import org.apache.commons.lang.StringUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.ArrayList;
import java.util.List;

public class TagOrBoolNode extends TagBoolTreeNode {
    public TagOrBoolNode(List<TagSyntaxTreeNode> subNodes) {
        super(subNodes);
    }

    @Override
    public BSONObject toSdbFileMatcher() throws ScmServerException {
        List<BSONObject> subNodesBson = new ArrayList<>();
        for (TagSyntaxTreeNode node : this.subNodes) {
            subNodesBson.add(node.toSdbFileMatcher());
        }
        return new BasicBSONObject("$or", subNodesBson);
    }

    @Override
    public String toString() {
        return "{$or: [ " + StringUtils.join(subNodes, ", ") + "]}";
    }
}
