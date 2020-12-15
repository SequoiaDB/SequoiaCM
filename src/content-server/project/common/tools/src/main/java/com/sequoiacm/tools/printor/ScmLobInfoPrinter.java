package com.sequoiacm.tools.printor;

import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.common.ScmFiledDefine;
import com.sequoiacm.tools.element.ScmLobInfo;

import java.text.SimpleDateFormat;

public class ScmLobInfoPrinter {
    private static final String LOB_STATUS_UNAVA = "unavailable";
    private static final String LOB_STATUS_AVA = "available";

    private final static int lenOfOID = 24 + 1;
    private static int lenOfTimeStamp = "yyyy-MM-dd-HH:mm:ss.SSS".length() + 1;
    private static int lenOfAvai = LOB_STATUS_UNAVA.length() + 1;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSS");
    private static int maxLenOfSize = 15; //the size col max width

    public static void printHead() {
        System.out.print(ScmFiledDefine.LOB_OID);
        ScmContentCommon.printSpace(lenOfOID - ScmFiledDefine.LOB_OID.length());
        System.out.print(ScmFiledDefine.LOB_CREATE_TIME);
        ScmContentCommon.printSpace(lenOfTimeStamp - ScmFiledDefine.LOB_CREATE_TIME.length());
        System.out.print(ScmFiledDefine.LOB_MODIFICATION_TIME);
        ScmContentCommon.printSpace(lenOfTimeStamp - ScmFiledDefine.LOB_MODIFICATION_TIME.length());
        System.out.print("Status");
        ScmContentCommon.printSpace(lenOfAvai - "Status".length());
        System.out.print(ScmFiledDefine.LOB_SIZE + "(Byte)");
        ScmContentCommon.printSpace(maxLenOfSize - (ScmFiledDefine.LOB_SIZE + "(Byte)").length());
        System.out.println("CollectionFullName");
    }

    public static void printScmLobInfo(ScmLobInfo info, String clFullName) {
        System.out.print(info.getOid().toString());
        ScmContentCommon.printSpace(lenOfOID - info.getOid().toString().length());

        String createTimeStr = sdf.format(info.getCreateTime());
        System.out.print(createTimeStr);
        ScmContentCommon.printSpace(lenOfTimeStamp - createTimeStr.length());

        String modificationTimeStr = sdf.format(info.getModificationTime());
        System.out.print(modificationTimeStr);
        ScmContentCommon.printSpace(lenOfTimeStamp - modificationTimeStr.length());

        if (info.isAvailable()) {
            System.out.print(LOB_STATUS_AVA);
            ScmContentCommon.printSpace(lenOfAvai - LOB_STATUS_AVA.length());
        }
        else {
            System.out.print(LOB_STATUS_UNAVA);
            ScmContentCommon.printSpace(lenOfAvai - LOB_STATUS_UNAVA.length());
        }
        System.out.print(info.getSize());
        ScmContentCommon.printSpace(maxLenOfSize - (info.getSize() + "").length());
        System.out.println(clFullName);

    }
}
