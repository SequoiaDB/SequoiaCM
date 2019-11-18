package com.sequoiacm.client.common;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.ScmIdParser;

/**
 * Utility class
 */
public class ScmUtil {

    /**
     * Utility class for id
     */
    public static class Id {

        private Id() {

        }

        /**
         * Get second information from ScmId
         *
         * @param id
         *            ScmId
         * @return second information of ScmId
         * @throws ScmException
         *             If error happens
         */
        public static long getSecond(ScmId id) throws ScmException {
            checkID(id);
            try {
                ScmIdParser idP = new ScmIdParser(id.get());
                return idP.getSecond();
            }
            catch (Exception e) {
                throw new ScmException(ScmError.INVALID_ID, e.getMessage(), e);
            }
        }

        /**
         * Get create month string from ScmId
         *
         * @param id
         *            ScmId
         * @return create month of ScmId
         * @throws ScmException
         *             If error happens
         *
         */
        public static String getCreateMonth(ScmId id) throws ScmException {
            checkID(id);
            try {
                ScmIdParser idP = new ScmIdParser(id.get());
                return idP.getMonth();
            }
            catch (Exception e) {
                throw new ScmException(ScmError.INVALID_ID, e.getMessage(), e);
            }
        }

        private static void checkID(ScmId id) throws ScmException {
            if (id == null) {
                throw new ScmInvalidArgumentException("id is null");
            }
        }
    }
}
