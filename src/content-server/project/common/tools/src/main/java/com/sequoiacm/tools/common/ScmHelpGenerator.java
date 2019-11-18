package com.sequoiacm.tools.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;

import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;

public class ScmHelpGenerator {
    List<String> optionList = new ArrayList<>();
    List<String> descslist = new ArrayList<>();
    List<Boolean> isHideList = new ArrayList<>();
    private int maxOptsLen = 0;

    public ScmHelpGenerator() {

    }

    public ScmHelpGenerator addOptHelp(String opt, String desc) {
        return addOptHelp(opt, desc, false);
    }

    public ScmHelpGenerator addOptHelp(String opt, String desc, boolean isHide) {
        opt = " " + opt;
        String[] descsArr = desc.split("\n");
        optionList.add(opt);
        if (opt.length() >= maxOptsLen) {
            maxOptsLen = opt.length() + 2;
        }
        descslist.add(descsArr[0]);
        isHideList.add(isHide);

        for (int i = 1; i < descsArr.length; i++) {
            optionList.add("");
            descslist.add(descsArr[i]);
            isHideList.add(isHide);
        }
        return this;
    }

    public ScmHelpGenerator addTitle(String title) {
        if (optionList.size() != 0) {
            optionList.add("");
            descslist.add("");
        }
        optionList.add(title);
        descslist.add("");
        isHideList.add(false);
        return this;
    }

    public void printHelp(boolean isPrintHideOpt) {
        System.out.println("Command options:");
        for (int i = 0; i < optionList.size(); i++) {
            if (isPrintHideOpt == false && isHideList.get(i) == true) {
                continue;
            }
            System.out.print(optionList.get(i));
            ScmCommon.printSpace(maxOptsLen - optionList.get(i).length());
            System.out.println(descslist.get(i));
        }
        System.out.println();
    }

    public Option createOpt(String shortOpt, String longOpt, String desc, boolean isRequire,
            boolean hasArg, boolean isHide) throws ScmToolsException {
        return createOpt(shortOpt, longOpt, desc, isRequire, hasArg, isHide, false);
    }

    public Option createOpt(String shortOpt, String longOpt, String desc, boolean isRequire,
            boolean hasArg, boolean isHide, boolean hasArgs) throws ScmToolsException {
        Builder opb;
        String opt;
        if (longOpt != null && shortOpt != null) {
            opb = Option.builder(shortOpt).longOpt(longOpt).desc(desc);
            opt = "-" + shortOpt + " [ --" + longOpt + " ]";
        }
        else if (longOpt == null) {
            opb = Option.builder(shortOpt).desc(desc);
            opt = "-" + shortOpt;
        }
        else if (shortOpt == null) {
            opb = Option.builder().longOpt(longOpt).desc(desc);
            opt = "--" + longOpt;
        }
        else {
            throw new ScmToolsException(
                    "Inner Error,failed to generate help msg,longOpt is null,shortOpt is null",
                    ScmExitCode.SYSTEM_ERROR);
        }

        if (hasArgs) {
            opb.hasArgs();
            opt = opt + "<key>=<value>";
        }
        else if (hasArg) {
            opb.hasArg(true);
            opt = opt + " arg";
        }

        if (isRequire) {
            opb.required(true);
        }
        addOptHelp(opt, desc, isHide);
        return opb.build();

    }


}
