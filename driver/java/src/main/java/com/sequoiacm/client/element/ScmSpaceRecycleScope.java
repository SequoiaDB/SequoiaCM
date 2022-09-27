package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;

/**
 * Space recycle scope.
 */
public abstract class ScmSpaceRecycleScope {

    private static final String MONTH_BEFORE_SCOPE_SUFFIX = " month before";

    /**
     * 
     * @param month
     *            the month count.
     * @return month before scope.
     * @throws ScmInvalidArgumentException
     *             if happens.
     */
    public static ScmSpaceRecycleScope mothBefore(int month) throws ScmInvalidArgumentException {
        if (month < 0) {
            throw new ScmInvalidArgumentException(
                    "month must be greater than or equal to 0, month=" + month);
        }
        return new ScmSpaceRecycleScopeMonthBefore(month);
    }

    static ScmSpaceRecycleScope getScope(String scopeStr) throws ScmInvalidArgumentException {
        if (scopeStr != null && scopeStr.endsWith(MONTH_BEFORE_SCOPE_SUFFIX)) {
            return new ScmSpaceRecycleScopeMonthBefore(
                    Integer.parseInt(scopeStr.replace(MONTH_BEFORE_SCOPE_SUFFIX, "").trim()));
        }
        throw new ScmInvalidArgumentException("invalid scopeStr:" + scopeStr);
    }

    public abstract String getScope();

    static class ScmSpaceRecycleScopeMonthBefore extends ScmSpaceRecycleScope {

        private final int month;

        public ScmSpaceRecycleScopeMonthBefore(int month) {
            this.month = month;
        }

        @Override
        public String getScope() {
            return month + MONTH_BEFORE_SCOPE_SUFFIX;
        }

        @Override
        public String toString() {
            return getScope();
        }
    }
}
