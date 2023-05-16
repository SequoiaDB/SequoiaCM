package com.sequoiacm.contentserver.tag.syntaxtree;

import com.sequoiacm.contentserver.exception.ScmSystemException;

import java.util.ArrayList;
import java.util.List;

public class TagSyntaxTree {
    private TagSyntaxTreeNode root;

    private List<TagMatcherNode> matcherNodes = new ArrayList<>();

    public TagSyntaxTree(TagSyntaxTreeNode root) throws ScmSystemException {
        this.root = root;
        scanMatcherNode(root);
    }

    private void scanMatcherNode(TagSyntaxTreeNode root) throws ScmSystemException {
        if (root instanceof TagMatcherNode) {
            matcherNodes.add((TagMatcherNode) root);
            return;
        }
        if (root instanceof TagBoolTreeNode) {
            for (TagSyntaxTreeNode node : ((TagBoolTreeNode) root).getSubNodes()) {
                scanMatcherNode(node);
            }
            return;
        }
        throw new ScmSystemException("Unknown TagSyntaxTreeNode type: " + root);
    }

    public List<TagMatcherNode> getMatcherNodes() {
        return matcherNodes;
    }

    public TagSyntaxTreeNode getRoot() {
        return root;
    }

    public String toString() {
        return root.toString();
    }
}
