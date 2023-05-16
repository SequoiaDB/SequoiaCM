package com.sequoiacm.contentserver.tag.syntaxtree;

import java.util.List;

public abstract class TagBoolTreeNode implements TagSyntaxTreeNode{
    protected List<TagSyntaxTreeNode> subNodes;

    public TagBoolTreeNode(List<TagSyntaxTreeNode> subNodes) {
        this.subNodes = subNodes;
    }

    public List<TagSyntaxTreeNode> getSubNodes() {
        return subNodes;
    }
}
