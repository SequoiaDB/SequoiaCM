package com.sequoiacm.client.element.privilege;

@Deprecated
public class ScmPrivilegeDefine {
    public static final String READ = "READ";

    public static final String CREATE = "CREATE";

    public static final String DELETE = "DELETE";

    public static final String UPDATE = "UPDATE";

    public static final String ALL = "ALL";

    public static String join(String... args) {
        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        for (String p : args) {
            if (!isFirst) {
                sb.append("|").append(p);
            }
            else {
                sb.append(p);
                isFirst = false;
            }
        }

        return sb.toString();
    }
}
