package com.sequoiacm.testcommon;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.jcraft.jsch.*;

/**
 * @author huangqiaohui
 */
public class Ssh {
    private final static Logger log = Logger.getLogger( Ssh.class.getName() );

    private static final int CHANNEL_CONNECT_TIMEOUT = 60 * 1000;
    private String host;
    private String username;
    private String password;
    private int port;
    private String stdout;
    private String stderr;
    private int exitStatus;
    private Session session = null;
    // ssh建立的后台命令集合（key：Channel id ，value：Channel）
    private Map< Integer, Channel > backgroundCMD = new HashMap< Integer, Channel >();

    public Ssh( String host ) throws JSchException {
        this( host, TestScmBase.sshUserName, TestScmBase.sshPassword );
    }

    /**
     * @descreption 使用给定参数及22端口创建ssh对象
     * @param host
     * @param username
     * @param password
     * @return
     * @throws JSchException
     */
    public Ssh( String host, String username, String password )
            throws JSchException {
        this( host, username, password, 22 );
    }

    /**
     * @descreption 使用给定参数创建ssh对象
     * @param host
     * @param username
     * @param password
     * @param port
     * @return
     * @throws JSchException
     */
    public Ssh( String host, String username, String password, int port )
            throws JSchException {
        super();
        this.host = host;
        this.username = username;
        this.password = password;
        this.port = port;
        JSch jsch = new JSch();
        try {
            session = jsch.getSession( username, host, port );
            session.setPassword( password );
            session.setConfig( "StrictHostKeyChecking", "no" );
            session.connect( CHANNEL_CONNECT_TIMEOUT );
        } catch ( JSchException e ) {
            if ( session != null ) {
                session.disconnect();
            }
            throw e;
        }
    }

    /**
     * @descreption 本地发送文件至远程主机
     * @param localPath
     * @param remotePath
     * @return
     * @throws Exception
     */
    public void scpTo( String localPath, String remotePath ) throws Exception {
        ChannelSftp channel = null;
        try {
            channel = ( ChannelSftp ) session.openChannel( "sftp" );
            channel.connect( CHANNEL_CONNECT_TIMEOUT );
            channel.put( localPath, remotePath );
        } finally {
            if ( channel != null ) {
                channel.disconnect();
            }
        }
    }

    /**
     * @descreption 下载远程主机文件至本地
     * @param localPath
     * @param remotePath
     * @return
     * @throws Exception
     */
    public void scpFrom( String localPath, String remotePath )
            throws Exception {
        ChannelSftp channel = null;
        try {
            channel = ( ChannelSftp ) session.openChannel( "sftp" );
            channel.connect( CHANNEL_CONNECT_TIMEOUT );
            channel.get( remotePath, localPath );
        } finally {
            if ( channel != null ) {
                channel.disconnect();
            }
        }
    }

    /**
     * @descreption 在远程主机上执行命令，并等待其执行结果，标准输出存入stdout，标准出错存入stderr,返回值存入exitStatus(注意：每一次调用exec都将覆盖上一次的执行结果,返回值不为零将抛出异常)
     * @param command
     * @return
     * @throws Exception
     */
    public void exec( String command ) throws Exception {
        Channel channel = null;
        try {
            channel = session.openChannel( "exec" );
            ( ( ChannelExec ) channel ).setCommand( command );
            channel.setInputStream( null );
            getResult( channel, CHANNEL_CONNECT_TIMEOUT );

            if ( exitStatus != 0 ) {
                throw new Exception( "ssh failed to execute commond '" + command
                        + "',stderr:" + stderr + " ,stdout:" + stdout
                        + ",errcode: " + exitStatus );
            }
        } catch ( IOException | JSchException e ) {
            throw e;
        } finally {
            if ( channel != null ) {
                channel.disconnect();
            }
        }
    }

    /**
     * @descreption 将命令发送至远程主机，返回当前命令的channelId，backgroundCMD将会记录执行本条命令的ChannelId及其Channel对象，waitBackgroudCMDDown方法可以根据channelid检测命令的执行结果
     * @param command
     * @return channelID
     * @throws Exception
     */
    public int execBackground( String command ) throws Exception {
        Channel channel = null;
        try {
            channel = session.openChannel( "exec" );
            ( ( ChannelExec ) channel ).setCommand( command );
            channel.setInputStream( null );
            channel.connect( CHANNEL_CONNECT_TIMEOUT );
            backgroundCMD.put( channel.getId(), channel );
            return channel.getId();
        } catch ( JSchException e ) {
            if ( channel != null ) {
                channel.disconnect();
            }
            throw e;
        }
    }

    /**
     * @descreption 等待给定channelId所执行的命令结束，覆盖stdout，stderr，exitstatus保存结果
     * @param channelId
     * @return
     * @throws Exception
     */
    public void waitBackgroudCMDDown( int channelId ) throws Exception {
        waitBackgroudCMDDown( channelId, Integer.MAX_VALUE );
    }

    /**
     * @descreption 等待给定channelId所执行的命令结束，覆盖stdout，stderr，exitstatus保存结果
     * @param timeOutSecond
     * @param channelId
     * @return
     * @throws Exception
     */
    public void waitBackgroudCMDDown( int channelId, int timeOutSecond )
            throws Exception {
        Channel channel = backgroundCMD.get( channelId );
        if ( channel == null ) {
            throw new Exception(
                    "ssh can not find this channel id(can not check channel "
                            + "id twice)" );
        }
        backgroundCMD.remove( channelId );
        try {
            getResult( channel, timeOutSecond );
        } finally {
            channel.disconnect();
        }
    }

    /**
     * @descreption 关闭Session，关闭backgroundCMD中的Channel（但这些未结束的后台命令可能仍会在远程主机正常执行） @return @throws
     */
    public void disconnect() {
        for ( Channel channel : backgroundCMD.values() ) {
            channel.disconnect();
        }
        if ( this.session != null ) {
            this.session.disconnect();
        }
    }

    /**
     * @descreption 获取scm集群安装目录
     * @return
     * @throws Exception
     */
    public String getScmInstallDir() throws Exception {
        Ssh ssh = new Ssh( host, username, password );
        String dir = null;
        try {

            ssh.exec( "ps -ef | grep sequoiacm-content-server  | grep -v grep |"
                    + "  head -n 1  | awk -F \" \" '{print $NF}'" );
            String str = ssh.getStdout();
            if ( str.length() <= 0 || !str.startsWith( "--logging.config=" ) ) {
                throw new Exception(
                        "failed to get scm install dir:ps -ef | grep "
                                + "sequoiacm-content-server  | grep -v grep |  "
                                + "head -n 1  | awk -F \\\" \\\" '{print $NF}',"
                                + " stdout=" + str );
            }
            String confFile = str.substring( "--logging.config=".length() );
            dir = confFile.substring( 0, confFile.indexOf( "conf" ) );

        } finally {
            ssh.disconnect();
        }
        return dir;

    }

    /**
     * @descreption 获取sdb集群安装目录
     * @return
     * @throws Exception
     */
    public String getSdbInstallDir() throws Exception {
        Ssh ssh = new Ssh( host, username, password );
        String dir = null;
        try {
            ssh.exec( "cat /etc/default/sequoiadb |grep INSTALL_DIR" );
            String str = ssh.getStdout();
            if ( str.length() <= 0 ) {
                throw new Exception(
                        "exec command:cat /etc/default/sequoiadb |grep "
                                + "INSTALL_DIR can not find sequoiadb install "
                                + "dir" );
            }
            dir = str.substring( str.indexOf( "=" ) + 1, str.length() - 1 );
        } finally {
            ssh.disconnect();
        }
        return dir;
    }

    /**
     * @descreption 获取SshResult信息
     * @param channel
     * @param timeOut
     * @return
     * @throws Exception
     */
    private void getResult( Channel channel, long timeOut ) throws IOException {
        StringBuffer stdoutBf = new StringBuffer();
        StringBuffer stderrBf = new StringBuffer();
        InputStream er = ( ( ChannelExec ) channel ).getErrStream();
        InputStream in = channel.getInputStream();
        byte[] tmp = new byte[ 1024 ];
        long timer = System.currentTimeMillis();
        try {
            channel.connect( 60 * 1000 );
        } catch ( JSchException e ) {
            log.severe( e.toString() );
        }
        while ( true ) {
            while ( in.available() > 0 ) {
                int i = in.read( tmp, 0, 1024 );
                if ( i < 0 ) {
                    break;
                }
                stdoutBf.append( new String( tmp, 0, i ) );

                if ( System.currentTimeMillis() - timer > timeOut * 1000 ) {
                    break;
                }
            }
            while ( er.available() > 0 ) {
                int i = er.read( tmp, 0, 1024 );
                if ( i < 0 )
                    break;
                stderrBf.append( new String( tmp, 0, i ) );
                if ( System.currentTimeMillis() - timer > timeOut * 1000 ) {
                    break;
                }
            }

            if ( channel.isClosed() ) {
                if ( in.available() > 0 || er.available() > 0 ) {
                    continue;
                }
                break;
            }

            try {
                Thread.sleep( 200 );
            } catch ( Exception e ) {
                // ignore
                // e.printStackTrace();
            }

            if ( System.currentTimeMillis() - timer > timeOut * 1000 ) {
                break;
            }
        }

        stdout = stdoutBf.toString();
        stderr = stderrBf.toString();
        exitStatus = channel.getExitStatus();
    }

    /**
     * @descreption 获取Ssh Stdout信息 @return @throws
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * @descreption 获取Ssh Stderr信息 @return @throws
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * @descreption 获取ExitStatus信息 @return @throws
     */
    public int getExitStatus() {
        return exitStatus;
    }

    /**
     * @descreption 获取Host信息 @return @throws
     */
    public String getHost() {
        return host;
    }

    /**
     * @descreption 获取UsersName信息 @return @throws
     */
    public String getUsername() {
        return username;
    }

    /**
     * @descreption 获取Password信息 @return @throws
     */
    public String getPassword() {
        return password;
    }

    /**
     * @descreption 获取port信息 @return @throws
     */
    public int getPort() {
        return port;
    }

    /**
     * @descreption 获取会话 @return @throws
     */
    public Session getSession() {
        return session;
    }

    /**
     * 获取SSH连接机器的JAVA_HOME环境变量
     * @return JAVA_HOME环境变量
     * @throws Exception
     */
    public String getJavaHome() throws Exception {
        Ssh ssh = new Ssh(host, username, password);
        String javaHome;
        try {
            // 尝试从 /etc/profile 文件中获取 JAVA_HOME 环境变量
            ssh.exec("grep -E '^\\s*export\\s+JAVA_HOME=' /etc/profile | awk -F '=' '{print $2}'");
            javaHome = ssh.getStdout().trim();

            if (javaHome.isEmpty()) {
                // 如果 /etc/profile 中未设置 JAVA_HOME，则尝试从 /etc/environment 文件中获取
                ssh.exec("grep -E '^\\s*JAVA_HOME=' /etc/environment | awk -F '=' '{print $2}'");
                javaHome = ssh.getStdout().trim();

                if (javaHome.isEmpty()) {
                    throw new Exception("Failed to get JAVA_HOME environment variable.");
                }
            }
        } finally {
            ssh.disconnect();
        }
        return javaHome;
    }
}
