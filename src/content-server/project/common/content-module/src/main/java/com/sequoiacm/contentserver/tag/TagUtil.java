package com.sequoiacm.contentserver.tag;

import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagName;

import java.util.ArrayList;
import java.util.List;

public class TagUtil {

    // 求 子集（subSet） 在 全集（fullSet） 中的补集
    static List<TagName> tagNameComplementarySet(List<TagInfo> fullSet, List<TagInfo> subSet) {
        List<TagName> ret = new ArrayList<>();
        for (TagInfo tagInfoInFullSet : fullSet) {

            boolean isFoundInSubSet = false;
            for (TagInfo tagInfoInSubset : subSet) {
                if (tagInfoInFullSet.getTagName().equals(tagInfoInSubset.getTagName())) {
                    isFoundInSubSet = true;
                    break;
                }
            }

            if (!isFoundInSubSet) {
                ret.add(tagInfoInFullSet.getTagName());
            }
        }
        return ret;
    }

}
