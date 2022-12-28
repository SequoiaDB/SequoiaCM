package com.sequoiacm.deploy.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum SubOption {
    HOST("host", Arrays.asList("upgrade", "rollback"), true, false) {
        @Override
        public String getDesc() {
            return getBelongOptions() + "specify host";
        }
    },

    SERVICE("service", Arrays.asList("upgrade", "rollback"), true, false) {
        @Override
        public String getDesc() {
            return getBelongOptions() + "specify service";
        }
    },
    UNATTENDED("unattended", Arrays.asList("upgrade", "rollback"), false, false) {
        @Override
        public String getDesc() {
            return getBelongOptions() + "unattended";
        }
    };

    private String name;
    private List<String> belongOptions;

    private boolean hasArgs;
    private boolean isRequire;

    private String desc;

    private SubOption(String name, List<String> belongOptions, boolean hasArgs, boolean isRequire) {
        this.name = name;
        this.belongOptions = belongOptions;
        this.hasArgs = hasArgs;
        this.isRequire = isRequire;
    }

    public static List<SubOption> getSubOption(String belongOption) {
        List<SubOption> subOptions = new ArrayList<>();
        for (SubOption subOption : SubOption.values()) {
            if (subOption.belongOptions.contains(belongOption)) {
                subOptions.add(subOption);
            }
        }
        return subOptions;
    }

    public static List<String> getAllBelongOptions() {
        List<String> allBelongOptions = new ArrayList<>();
        for (SubOption subOption : SubOption.values()) {
            for (String belongOption : subOption.getBelongOptions()) {
                if (allBelongOptions.contains(belongOption)) {
                    continue;
                }
                allBelongOptions.add(belongOption);

            }
        }
        return allBelongOptions;
    }

    public String getName() {
        return name;
    }

    public List<String> getBelongOptions() {
        return belongOptions;
    }

    public String getOneOfBelongOptionsStr() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < belongOptions.size(); i++) {
            stringBuilder.append(belongOptions.get(i));
            if (i < belongOptions.size() - 1) {
                stringBuilder.append(" or ");
            }
        }
        return stringBuilder.toString();
    }

    public boolean isHasArgs() {
        return hasArgs;
    }

    public boolean isRequire() {
        return isRequire;
    }

    public String getDesc() {
        return desc;
    }
}
