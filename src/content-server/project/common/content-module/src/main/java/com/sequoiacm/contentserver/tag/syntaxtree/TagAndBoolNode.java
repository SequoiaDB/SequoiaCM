package com.sequoiacm.contentserver.tag.syntaxtree;

import com.sequoiacm.exception.ScmServerException;
import org.apache.commons.lang.StringUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.ArrayList;
import java.util.List;

public class TagAndBoolNode extends TagBoolTreeNode {

    public TagAndBoolNode(List<TagSyntaxTreeNode> subNodes) {
        super(subNodes);
    }

    @Override
    public BSONObject toSdbFileMatcher() throws ScmServerException {
        List<BSONObject> subNodesBson = new ArrayList<>();
        for (TagSyntaxTreeNode node : this.subNodes) {
            subNodesBson.add(node.toSdbFileMatcher());
        }
        return new BasicBSONObject("$and", subNodesBson);
    }

    @Override
    public String toString() {
        return "{$and: [ " + StringUtils.join(subNodes, ", ") + "]}";
    }
}
