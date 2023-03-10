import paramiko
import sys
import os
rootDir = sys.path[0]+os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
paramiko.util.log_to_file(rootDir + ".." + os.sep + "tmp" + os.sep + "paramiko.log")

class SSHConnection:

    def __init__(self, host='XXX.XXX.XXX.XXX', port=22, user='XX', pwd='XXX'):
        self.host = host
        self.port = port
        self.user = user
        self.pwd = pwd
        self.__transport = paramiko.Transport((self.host, self.port))
        self.__transport.connect(username = self.user, password = self.pwd)
        self.sftp = paramiko.SFTPClient.from_transport(self.__transport)

    def close(self):
        self.sftp.close()
        self.__transport.close()

    def upload(self, local_path, remote_path):
        self.sftp.put(local_path, remote_path)

    def download(self, local_path, remote_path):
        self.sftp.get(local_path, remote_path)

    def mkdir(self ,target_path, mode='0777'):
        self.sftp.mkdir(target_path, mode)

    def rmdir(self, target_path):
        self.sftp.rmdir(target_path)

    def listdir(self, target_path):
        return self.sftp.listdir(target_path)

    def remove(self, target_path):
        self.sftp.remove(target_path)

    def cmd(self,command):
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.load_system_host_keys()
        try:
            ssh._transport = self.__transport
        except BadHostKeyException as e:
            print(e)
            sys.exit()
        except AuthenticationException as e:
            print(e)
            sys.exit()
        except SSHExpection as e:
            print(e)
            sys.exit()
        print("exec cmd:" + command)
        ssh_stdin, ssh_stdout, ssh_stderr = ssh.exec_command(command)
        channel = ssh_stdout.channel
        status = channel.recv_exit_status()
        result = ssh_stdout.read()
        msg = ssh_stderr.read()
        if len(str(result).strip()) != 0:
            print(result)
        else:
            print(msg)
        list = [status, result, msg]
        return list