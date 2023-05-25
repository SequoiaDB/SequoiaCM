package com.sequoiacm.infrastructure.sdbversion;

import java.util.ArrayList;
import java.util.List;

public class VersionRange {
    private Version left;
    private boolean leftInclusive;

    private Version right;
    private boolean rightInclusive;

    private String srcRange;

    public static List<VersionRange> parse(String versionRangesStr) {
        ArrayList<VersionRange> ret = new ArrayList<>();
        String requiredVersionStrTrim = versionRangesStr.trim();
        // requiredVersionStr:
        // 多个范围：[3.6.1, 4.6.2];(5.0,5.6]
        // 指定版本 3.6.1
        String[] versionRanges = requiredVersionStrTrim.split(";");
        for (String versionRange : versionRanges) {
            versionRange = versionRange.trim();
            ret.add(new VersionRange(versionRange));
        }
        return ret;
    }

    VersionRange(String range) {
        if (range == null || range.isEmpty()) {
            throw new IllegalArgumentException("range is null or empty");
        }
        srcRange = range;
        range = range.trim();

        try {

            // range = 3.6.1
            if (isSingleVersionNumbers(range)) {
                left = new Version(range);
                right = left;
                leftInclusive = true;
                rightInclusive = true;
                return;
            }

            // range = [3.6.1, 4.2.0]
            checkVersionRangeValid(range);
            leftInclusive = range.charAt(0) == '[';
            rightInclusive = range.charAt(range.length() - 1) == ']';

            String rangNoWrapper = range.substring(1, range.length() - 1);
            rangNoWrapper = rangNoWrapper.trim();
            String[] leftAndRight = rangNoWrapper.split(",");
            if (leftAndRight.length != 2) {
                throw new IllegalArgumentException(
                        "range is invalid, the range can only contain two version numbers, range="
                                + range);
            }

            left = new Version(leftAndRight[0]);
            right = new Version(leftAndRight[1]);
            if (left.compareTo(right) > 0) {
                throw new IllegalArgumentException(
                        "range is invalid, the left version numbers cannot bigger than right version numbers, range="
                                + range);
            }
        }
        catch (Exception e) {
            throw new IllegalArgumentException("range is invalid: " + range, e);
        }
    }

    public boolean isInRange(Version version) {
        int leftRet = version.compareTo(left);
        if (leftRet < 0 || (leftRet == 0 && !leftInclusive)) {
            return false;
        }

        int rightRet = version.compareTo(right);
        if (rightRet > 0 || (rightRet == 0 && !rightInclusive)) {
            return false;
        }

        return true;
    }

    private boolean isSingleVersionNumbers(String range) {
        return range.charAt(0) != '[' && range.charAt(0) != '('
                && range.charAt(range.length() - 1) != ']'
                && range.charAt(range.length() - 1) != ')';
    }

    private void checkVersionRangeValid(String range) {
        if (range.charAt(0) != '(' && range.charAt(0) != '[') {
            throw new IllegalArgumentException(
                    "range is invalid, only '(' or '[' is supported at the beginning, range="
                            + range);
        }

        if (range.charAt(range.length() - 1) != ')' && range.charAt(range.length() - 1) != ']') {
            throw new IllegalArgumentException(
                    "range is invalid, only ')' or ']' is supported at the ending, range=" + range);
        }
    }

    @Override
    public String toString() {
        return srcRange;
    }
}
