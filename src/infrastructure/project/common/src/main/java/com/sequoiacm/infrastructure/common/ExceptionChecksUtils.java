package com.sequoiacm.infrastructure.common;

import com.sequoiacm.exception.ScmError;

public class ExceptionChecksUtils {

    public static boolean isOldVersion(String message, int errorCode, String methodName) {
        if (methodName.equals("getSalt")) {
            return ((message.equals("Access Denied")
                    && errorCode == ScmError.HTTP_FORBIDDEN.getErrorCode())
                    || (message.equals("Unauthorized")
                            && errorCode == ScmError.HTTP_UNAUTHORIZED.getErrorCode()));
        }
        else if (methodName.equals("v2LocalLogin")) {
            return ((message.equals("v2 is not exist")
                    && errorCode == ScmError.HTTP_NOT_FOUND.getErrorCode())
                    || (message.equals("No message available")
                            && errorCode == ScmError.HTTP_NOT_FOUND.getErrorCode()));
        }
        else if (methodName.equals("v2AlterUser") || methodName.equals("v2CreateUser")) {
            return (message.equals("No message available")
                    && errorCode == ScmError.HTTP_NOT_FOUND.getErrorCode());
        }
        else {
            return false;
        }
    }

    public static boolean isNotLocalUser(String error, int errorCode) {
        return error.equals(ScmError.FIND_SALT_FAILED.getErrorDescription())
                && errorCode == ScmError.FIND_SALT_FAILED.getErrorCode();
    }
}
