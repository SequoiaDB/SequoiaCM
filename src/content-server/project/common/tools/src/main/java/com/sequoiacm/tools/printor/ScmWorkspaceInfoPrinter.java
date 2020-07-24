package com.sequoiacm.tools.printor;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.element.ScmWorkspaceInfo;

public class ScmWorkspaceInfoPrinter {
    private List<String> nameList = new ArrayList<>();
    private List<String> idList = new ArrayList<>();
    private List<String> metaList = new ArrayList<>();
    private List<String> dataList = new ArrayList<>();
    private List<String> dataSharTypeList = new ArrayList<>();
    private List<String> metaShardTypeList = new ArrayList<>();
    private List<String> dataOpsList = new ArrayList<>();

    private String colOfName = "Name";
    private String colOfId = "Id";
    private String colOfShardType = "DataShardingType";
    private String colOfMetaShardType = "MetaShardingType";
    private String colOfMeta = "Meta";
    private String colOfData = "Data";
    private String colOfDataOption = "DataOption";
    private int maxNameLen = colOfName.length() + 1;
    private int maxIdLen = colOfId.length() + 1;
    private int maxMetaShardTypeLen = colOfMetaShardType.length() + 1;
    private int maxDataShardTypeLen = colOfShardType.length() + 1;
    private int maxMetaLen = colOfMeta.length() + 1;
    private int maxDataOpsLen = colOfDataOption.length() + 1;

    public ScmWorkspaceInfoPrinter(List<ScmWorkspaceInfo> list) throws ScmToolsException {

        for (ScmWorkspaceInfo info : list) {
            nameList.add(info.getName());
            if (info.getName().length() >= maxNameLen) {
                maxNameLen = info.getName().length() + 1;
            }
            idList.add(info.getId() + "");
            if ((info.getId() + "").length() >= maxIdLen) {
                maxIdLen = (info.getId() + "").length() + 1;
            }

            String shardType = info.getDataShardingTypeBSON() == null ? "null"
                    : info.getDataShardingTypeBSON().toString();
            dataSharTypeList.add(shardType);
            if (shardType.length() >= maxDataShardTypeLen) {
                maxDataShardTypeLen = shardType.length() + 1;
            }

            String metaShardType = info.getMetaShardType() == null ? "null"
                    : info.getMetaShardType();
            metaShardTypeList.add(metaShardType);
            if (metaShardType.length() >= maxMetaShardTypeLen) {
                maxMetaShardTypeLen = metaShardType.length() + 1;
            }

            metaList.add(info.getMetaLocationBSON().toString());
            if ((info.getMetaLocationBSON().toString()).length() >= maxMetaLen) {
                maxMetaLen = (info.getMetaLocationBSON().toString()).length() + 1;
            }

            String dataOpStr = info.getDataOptionBSON() == null ? "null"
                    : info.getDataOptionBSON().toString();
            dataOpsList.add(dataOpStr);
            if (dataOpStr.length() >= maxDataOpsLen) {
                maxDataOpsLen = dataOpStr.length() + 1;
            }

            dataList.add(info.getDataLocationBSON().toString());
        }
    }

    public void print() {
        // print head
        System.out.print(colOfName);
        ScmCommon.printSpace(maxNameLen - colOfName.length());

        System.out.print(colOfId);
        ScmCommon.printSpace(maxIdLen - colOfId.length());

        boolean isMetaShardTypeListHasValue = isContainNotNull(metaShardTypeList);
        if (isMetaShardTypeListHasValue) {
            System.out.print(colOfMetaShardType);
            ScmCommon.printSpace(maxMetaShardTypeLen - colOfMetaShardType.length());
        }

        boolean isDataShardTypeListHasValue = isContainNotNull(dataSharTypeList);
        if (isDataShardTypeListHasValue) {
            System.out.print(colOfShardType);
            ScmCommon.printSpace(maxDataShardTypeLen - colOfShardType.length());
        }

        boolean isDataOpsListHasValue = isContainNotNull(dataOpsList);
        if (isDataOpsListHasValue) {
            System.out.print(colOfDataOption);
            ScmCommon.printSpace(maxDataOpsLen - colOfDataOption.length());
        }

        System.out.print(colOfMeta);
        ScmCommon.printSpace(maxMetaLen - colOfMeta.length());

        System.out.println(colOfData);
        for (int i = 0; i < nameList.size(); i++) {
            System.out.print(nameList.get(i));
            ScmCommon.printSpace(maxNameLen - nameList.get(i).length());

            System.out.print(idList.get(i));
            ScmCommon.printSpace(maxIdLen - idList.get(i).length());

            if (isMetaShardTypeListHasValue) {
                System.out.print(metaShardTypeList.get(i));
                ScmCommon.printSpace(maxMetaShardTypeLen - metaShardTypeList.get(i).length());
            }

            if (isDataShardTypeListHasValue) {
                System.out.print(dataSharTypeList.get(i));
                ScmCommon.printSpace(maxDataShardTypeLen - dataSharTypeList.get(i).length());
            }

            if (isDataOpsListHasValue) {
                System.out.print(dataOpsList.get(i));
                ScmCommon.printSpace(maxDataOpsLen - dataOpsList.get(i).length());
            }

            System.out.print(metaList.get(i));
            ScmCommon.printSpace(maxMetaLen - metaList.get(i).length());

            System.out.println(dataList.get(i));
        }

        System.out.println("Total:" + nameList.size());
    }

    private boolean isContainNotNull(List<String> list) {
        if (list == null || list.size() == 0) {
            return false;
        }

        for (String s : list) {
            if (s != null && !s.equals("null")) {
                return true;
            }
        }
        return false;
    }
}
