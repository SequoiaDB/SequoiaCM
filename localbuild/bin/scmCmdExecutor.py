import os


class ScmCmdExecutor:
    def __init__(self, isDryRun):
        self.__isDryRun = isDryRun

    def command(self, cmd):
        self.command(cmd, True, True)

    def command(self, cmd, needPrint=True, strictMode=True):
        if needPrint:
            print("exec cmd:" + cmd)
        if self.__isDryRun:
            return
        ret = os.system(cmd)
        if ret != 0 and strictMode:
            raise Exception("Failed to exec cmd:" + cmd)
        else:
            return ret
