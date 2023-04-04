package com.sequoiacm.diagnose.utils;

import com.sequoiacm.client.dispatcher.CloseableFileDataEntity;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.exception.ScmExitCode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

public class CommonUtils {

    public static long writeContent(OutputStream os, CloseableFileDataEntity fileData)
            throws IOException {
        int len = 0;
        long totalRecvLen = 0;
        byte[] buf = new byte[1024];
        while ((len = fileData.readAsMuchAsPossible(buf)) != -1) {
            totalRecvLen += len;
            os.write(buf, 0, len);
        }
        fileData.close();
        return totalRecvLen;
    }

    public static long getLineCount(String file) throws ScmToolsException {
        long linesCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    // 去掉空格后不为空
                    linesCount++;
                }
            }
            return linesCount;
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to read file,file: " + file,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public static String covertMillisToMinAndSeconds(double time) {
        double remainingMillis = time % 1000;
        double totalSeconds = time / 1000;
        double minutes = totalSeconds / 60;
        double remainingSeconds = totalSeconds % 60;
        return (int) minutes + " min " + (int) remainingSeconds + " s " + (int) remainingMillis
                + " ms";
    }

    public static String getSystemTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        return format.format(date);
    }

    public static void configLog(String logFilePath) throws ScmToolsException {
        InputStream is = null;
        try {
            is = new FileInputStream(logFilePath);
            ScmHelper.configToolsLog(is);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to read log configuration file,file:" + logFilePath,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(is);
        }
    }

    public static String calcMd5(ScmDataReader reader) throws ScmToolsException {
        byte[] buf = new byte[1024 * 64];
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            while (true) {
                int readLen = reader.read(buf, 0, buf.length);
                if (readLen <= -1) {
                    break;
                }
                md5.update(buf, 0, readLen);
            }
            byte[] md5Bytes = md5.digest();
            return DatatypeConverter.printBase64Binary(md5Bytes);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to calc md5", ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public static BSONObject getMatcher(String begin, String end) throws ScmToolsException {
        BSONObject gteTime = new BasicBSONObject("$gte", parseDateToTime(begin + " 000000 000"));
        BSONObject createTime1 = new BasicBSONObject("create_time", gteTime);
        BSONObject lteTime = new BasicBSONObject("$lte", parseDateToTime(end + " 235959 999"));
        BSONObject createTime2 = new BasicBSONObject("create_time", lteTime);
        BasicBSONList l1 = new BasicBSONList();
        l1.add(createTime1);
        l1.add(createTime2);
        // {"$and": [{"create_time": {"$gte": beginTime}},{"create_time": {"$lte":
        // endTime}}]}
        BSONObject matcher1 = new BasicBSONObject("$and", l1);

        BSONObject gteMonth = new BasicBSONObject("$gte", getMonth(begin));
        BSONObject createMonth1 = new BasicBSONObject("create_month", gteMonth);
        BSONObject lteMonth = new BasicBSONObject("$lte", getMonth(end));
        BSONObject createMonth2 = new BasicBSONObject("create_month", lteMonth);
        BasicBSONList l2 = new BasicBSONList();
        l2.add(createMonth1);
        l2.add(createMonth2);
        // {"$and": [{"create_month": {"$gte": begin}},{"create_month": {"$lte": end}}]}
        BSONObject matcher2 = new BasicBSONObject("$and", l2);

        BasicBSONList l = new BasicBSONList();
        l.add(matcher2);
        l.add(matcher1);
        // {"$and": [matcher1,matcher2]}
        return new BasicBSONObject("$and", l);
    }

    private static String getMonth(String time) {
        return time.substring(0, 6);
    }

    private static long parseDateToTime(String source) throws ScmToolsException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HHmmss SSS");
        Date date = null;
        try {
            date = format.parse(source);
            return date.getTime();
        }
        catch (ParseException e) {
            throw new ScmToolsException("Failed to parse date to time,date:" + source,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public static String buildCommandInfo(CommandLine cl, String commandName, String passwdOp) {
        Iterator<Option> iterator = cl.iterator();
        StringBuilder stringBuilder = new StringBuilder(commandName);
        while (iterator.hasNext()) {
            Option op = iterator.next();
            String opt = op.getLongOpt() == null ? op.getOpt() : op.getLongOpt();
            if (opt.equals(passwdOp)) {
                stringBuilder.append(" ").append("--").append(opt);
                if (null != op.getValue()) {
                    stringBuilder.append(" ").append("********");
                }
            }
            else {
                stringBuilder.append(" ").append("--").append(opt).append(" ")
                        .append(op.getValue());
            }
        }
        return stringBuilder.toString();
    }
}
