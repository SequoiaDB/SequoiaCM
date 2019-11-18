import commands
import os


class ScmCmdExecutor:
    def __init__(self, isDryRun):
        self.__isDryRun = isDryRun

    def command(self, cmd):
        print("exec cmd:" + cmd)
        if self.__isDryRun:
            return
        ret = os.system(cmd)
        if ret != 0:
            raise Exception("Failed to exec cmd:" + cmd)
