package com.sequoiacm.tools.printor;

import com.sequoiacm.tools.common.ScmCommon;
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
        ScmCommon.printSpace(lenOfOID - ScmFiledDefine.LOB_OID.length());
        System.out.print(ScmFiledDefine.LOB_CREATE_TIME);
        ScmCommon.printSpace(lenOfTimeStamp - ScmFiledDefine.LOB_CREATE_TIME.length());
        System.out.print(ScmFiledDefine.LOB_MODIFICATION_TIME);
        ScmCommon.printSpace(lenOfTimeStamp - ScmFiledDefine.LOB_MODIFICATION_TIME.length());
        System.out.print("Status");
        ScmCommon.printSpace(lenOfAvai - "Status".length());
        System.out.print(ScmFiledDefine.LOB_SIZE + "(Byte)");
        ScmCommon.printSpace(maxLenOfSize - (ScmFiledDefine.LOB_SIZE + "(Byte)").length());
        System.out.println("CollectionFullName");
    }

    public static void printScmLobInfo(ScmLobInfo info, String clFullName) {
        System.out.print(info.getOid().toString());
        ScmCommon.printSpace(lenOfOID - info.getOid().toString().length());

        String createTimeStr = sdf.format(info.getCreateTime());
        System.out.print(createTimeStr);
        ScmCommon.printSpace(lenOfTimeStamp - createTimeStr.length());

        String modificationTimeStr = sdf.format(info.getModificationTime());
        System.out.print(modificationTimeStr);
        ScmCommon.printSpace(lenOfTimeStamp - modificationTimeStr.length());

        if (info.isAvailable()) {
            System.out.print(LOB_STATUS_AVA);
            ScmCommon.printSpace(lenOfAvai - LOB_STATUS_AVA.length());
        }
        else {
            System.out.print(LOB_STATUS_UNAVA);
            ScmCommon.printSpace(lenOfAvai - LOB_STATUS_UNAVA.length());
        }
        System.out.print(info.getSize());
        ScmCommon.printSpace(maxLenOfSize - (info.getSize() + "").length());
        System.out.println(clFullName);

    }
}
