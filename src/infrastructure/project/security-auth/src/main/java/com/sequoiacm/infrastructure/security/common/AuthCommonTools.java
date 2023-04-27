package com.sequoiacm.infrastructure.security.common;

public class AuthCommonTools {
    public static boolean isBigUser(String userDetail, String encoding) {
        int length = 0;
        try {
            length = userDetail.getBytes(encoding).length;
        }
        catch (Exception e) {
            // 编码类型为 null 或者编码类型不支持
            length = userDetail.getBytes().length;
        }
        return length >= 7 * 1024;
    }
}
