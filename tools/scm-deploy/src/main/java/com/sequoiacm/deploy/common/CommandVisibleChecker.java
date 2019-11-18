package com.sequoiacm.deploy.common;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public interface CommandVisibleChecker {
    boolean isVisible(String subCommand);

    //return null if the subcommand is invisible
    Options filterInvisibleOptions(String subCommand, Options ops);

    void checkIsValid(String subCommand, CommandLine cl) throws IllegalArgumentException;
}
