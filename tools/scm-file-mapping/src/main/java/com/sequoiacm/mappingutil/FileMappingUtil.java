package com.sequoiacm.mappingutil;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.mappingutil.command.ScmFileMappingToolImpl;

public class FileMappingUtil {

    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("fileMappingUtil");
        try {
            cmd.addTool(new ScmFileMappingToolImpl());
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        cmd.execute(args);
    }
}
