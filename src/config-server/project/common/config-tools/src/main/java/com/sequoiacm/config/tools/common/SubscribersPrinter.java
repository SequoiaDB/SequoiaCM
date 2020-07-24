package com.sequoiacm.config.tools.common;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

public class SubscribersPrinter {
    private List<String> configNameList = new ArrayList<>();
    private List<String> serviceNameList = new ArrayList<>();
    private String colOfConfigName = "ConfigName";
    private String colOfSerivceName = "ServiceName";

    private int maxNameLen = colOfConfigName.length() + 1;

    public SubscribersPrinter(BasicBSONList list) {
        for (Object info : list) {
            BSONObject infoObj = (BSONObject) info;
            String configName = (String) infoObj.get(ScmRestArgDefine.CONFIG_NAME);
            String serviceName = (String) infoObj.get(ScmRestArgDefine.SERVICE_NAME);
            configNameList.add(configName);
            if (configName.length() >= maxNameLen) {
                maxNameLen = configName.length() + 1;
            }
            serviceNameList.add(serviceName);
        }
    }

    public void print() {
        // print head
        System.out.print(colOfConfigName);
        ScmCommon.printSpace(maxNameLen - colOfConfigName.length());
        System.out.println(colOfSerivceName);

        for (int i = 0; i < configNameList.size(); i++) {
            System.out.print(configNameList.get(i));
            ScmCommon.printSpace(maxNameLen - configNameList.get(i).length());
            System.out.println(serviceNameList.get(i));
        }
        System.out.println("Total:" + configNameList.size());
    }
}
