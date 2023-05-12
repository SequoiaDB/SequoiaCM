package com.sequoiacm.infrastructure.sdbversion;

import java.util.ArrayList;
import java.util.List;

public class SdbVersionRange {
    private SdbVersion left;
    private boolean leftInclusive;

    private SdbVersion right;
    private boolean rightInclusive;

    private String srcRange;

    public static List<SdbVersionRange> parse(String versionRangesStr) {
        ArrayList<SdbVersionRange> ret = new ArrayList<>();
        String requiredVersionStrTrim = versionRangesStr.trim();
        // requiredVersionStr:
        // 多个范围：[3.6.1, 4.6.2];(5.0,5.6]
        // 指定版本 3.6.1
        String[] versionRanges = requiredVersionStrTrim.split(";");
        for (String versionRange : versionRanges) {
            versionRange = versionRange.trim();
            ret.add(new SdbVersionRange(versionRange));
        }
        return ret;
    }

    SdbVersionRange(String range) {
        if (range == null || range.isEmpty()) {
            throw new IllegalArgumentException("range is null or empty");
        }
        srcRange = range;
        range = range.trim();

        try {

            // range = 3.6.1
            if (range.charAt(0) != '[' && range.charAt(0) != '('
                    && range.charAt(range.length() - 1) != ']'
                    && range.charAt(range.length() - 1) != ')') {
                left = new SdbVersion(range);
                right = left;
                leftInclusive = true;
                rightInclusive = true;
                return;
            }

            // range = [3.6.1, 4.2.0]
            leftInclusive = range.charAt(0) == '[';
            rightInclusive = range.charAt(range.length() - 1) == ']';

            String rangNoWrapper = range.substring(1, range.length() - 1);
            rangNoWrapper = rangNoWrapper.trim();
            String[] leftAndRight = rangNoWrapper.split(",");
            if (leftAndRight.length != 2) {
                throw new IllegalArgumentException("range is invalid");
            }

            left = new SdbVersion(leftAndRight[0]);
            right = new SdbVersion(leftAndRight[1]);
            checkRangeValid();
        }
        catch (Exception e) {
            throw new IllegalArgumentException("range is invalid: " + range, e);
        }
    }

    public boolean isInRange(SdbVersion version) {
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

    private void checkRangeValid() {
        // 检查如 (4, 3.6.1) 这种左边的版本号大于右边的版本号的情况
        int diff = left.compareTo(right);
        if (diff > 0) {
            throw new IllegalArgumentException("range is invalid");
        }
    }

    @Override
    public String toString() {
        return srcRange;
    }
}
