package com.sequoiacm.tools.printor;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.element.ScmSiteInfo;

public class SiteInfoPrinter {
    private List<String> nameList = new ArrayList<>();
    private List<String> idList = new ArrayList<>();
    private List<String> dataUrlList = new ArrayList<>();
    private List<String> dataTypeList = new ArrayList<>();
    private List<String> metaUrlList = new ArrayList<>();
    private List<String> isMainList = new ArrayList<>();
    private String colOfName = "Name";
    private String colOfId = "Id";
    private String colOfIsMain = "IsRootSite";
    private String colOfDataType = "DataType";
    private String colOfDataUrl = "DataUrl";
    private String colOfMetaUrl = "MetaUrl";
    private int maxNameLen = colOfName.length() + 1;
    private int maxIdLen = colOfId.length() + 1;
    private int maxIsMain = colOfIsMain.length() + 1;
    private int maxDataType = colOfDataType.length() + 1;
    private int maxDataUrl = colOfDataUrl.length() + 1;

    public SiteInfoPrinter(List<ScmSiteInfo> list) {
        for (ScmSiteInfo info : list) {
            nameList.add(info.getName());
            if (info.getName().length() >= maxNameLen) {
                maxNameLen = info.getName().length() + 1;
            }
            idList.add(info.getId() + "");
            if ((info.getId() + "").length() >= maxIdLen) {
                maxIdLen = (info.getId() + "").length() + 1;
            }
            isMainList.add(info.isRootSite() + "");

            dataTypeList.add(info.getDataType());
            if (info.getDataType().length() >= maxDataType) {
                maxDataType = info.getDataType().length() + 1;
            }

            dataUrlList.add(info.getDataUrlStr());
            if (info.getDataUrlStr().length() >= maxDataUrl) {
                maxDataUrl = info.getDataUrlStr().length() + 1;
            }

            if (info.isRootSite()) {
                metaUrlList.add(info.getMetaUrlStr());
            }else{
                metaUrlList.add("");
            }

        }
    }

    public void print() {
        // print head
        System.out.print(colOfName);
        ScmContentCommon.printSpace(maxNameLen - colOfName.length());
        System.out.print(colOfId);
        ScmContentCommon.printSpace(maxIdLen - colOfId.length());
        System.out.print(colOfIsMain);
        ScmContentCommon.printSpace(maxIsMain - colOfIsMain.length());
        System.out.print(colOfDataType);
        ScmContentCommon.printSpace(maxDataType - colOfDataType.length());
        System.out.print(colOfDataUrl);
        ScmContentCommon.printSpace(maxDataUrl-colOfDataUrl.length());
        System.out.println(colOfMetaUrl);

        for (int i = 0; i < nameList.size(); i++) {
            System.out.print(nameList.get(i));
            ScmContentCommon.printSpace(maxNameLen - nameList.get(i).length());
            System.out.print(idList.get(i));
            ScmContentCommon.printSpace(maxIdLen - idList.get(i).length());
            System.out.print(isMainList.get(i));
            ScmContentCommon.printSpace(maxIsMain - isMainList.get(i).length());
            System.out.print(dataTypeList.get(i));
            ScmContentCommon.printSpace(maxDataType-dataTypeList.get(i).length());
            System.out.print(dataUrlList.get(i));
            ScmContentCommon.printSpace(maxDataUrl-dataUrlList.get(i).length());
            System.out.println(metaUrlList.get(i));
        }
        System.out.println("Total:" + nameList.size());
    }
}
