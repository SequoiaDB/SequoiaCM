package com.sequoiacm.common;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BSONTimestamp;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonHelper {
    private static SimpleDateFormat ymFullDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static SimpleDateFormat ymdDateFormat = new SimpleDateFormat("yyyyMMdd");
    private static SimpleDateFormat ymDateFormat = new SimpleDateFormat("yyyyMM");
    private static SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy");
    private static SimpleDateFormat monthDateFormat = new SimpleDateFormat("MM");
    private static Logger logger = LoggerFactory.getLogger(CommonHelper.class);
    public static final String MONTH1 = "01";
    public static final String MONTH3 = "03";
    public static final String MONTH4 = "04";
    public static final String MONTH6 = "06";
    public static final String MONTH7 = "07";
    public static final String MONTH9 = "09";
    public static final String MONTH10 = "10";

    public static final String QUARTER1 = "Q1";
    public static final String QUARTER2 = "Q2";
    public static final String QUARTER3 = "Q3";
    public static final String QUARTER4 = "Q4";

    private static final List<String> FILE_FIELDS_NEED_CHECK_WHEN_MERGE = new ArrayList<String>();
    static {
        FILE_FIELDS_NEED_CHECK_WHEN_MERGE.add(FieldName.FIELD_CLFILE_FILE_MD5);
    }

    public static boolean getFileLocationList(BasicBSONList siteList,
            List<ScmFileLocation> locationList) {
        boolean isExistNull = false;
        for (Object o : siteList) {
            BSONObject bo = (BSONObject) o;
            if (null != bo) {
                ScmFileLocation location = new ScmFileLocation(bo);
                locationList.add(location);
            }
            else {
                isExistNull = true;
            }
        }

        return isExistNull;
    }

    public static Map<Integer, ScmFileLocation> getFileLocationList(BasicBSONList siteList) {
        Map<Integer, ScmFileLocation> fileLocationMap = new HashMap<Integer, ScmFileLocation>();
        for (Object o : siteList) {
            BSONObject bo = (BSONObject) o;
            if (null != bo) {
                ScmFileLocation location = new ScmFileLocation(bo);
                fileLocationMap.put(location.getSiteId(), location);
            }
        }

        return fileLocationMap;
    }

    public static List<Integer> getFileLocationIdList(BasicBSONList siteList) {
        List<ScmFileLocation> locationList = new ArrayList<ScmFileLocation>();
        getFileLocationList(siteList, locationList);
        return getFileLocationIdList(locationList);
    }

    public static List<Integer> getFileLocationIdList(List<ScmFileLocation> locationList) {
        List<Integer> siteIdList = new ArrayList<Integer>();
        for (ScmFileLocation loc : locationList) {
            siteIdList.add(loc.getSiteId());
        }
        return siteIdList;
    }

    public static boolean isSiteExist(int siteId, List<ScmFileLocation> siteList) {
        for (ScmFileLocation info : siteList) {
            if (siteId == info.getSiteId()) {
                return true;
            }
        }

        return false;
    }

    public static String bytesToHexStr(byte[] b) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            int x = b[i] & 0xFF;
            String s = Integer.toHexString(x).toUpperCase();
            if (s.length() == 1) {
                buf.append("0");
            }

            buf.append(s);
        }

        return buf.toString();
    }

    private static boolean isHexStr(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                continue;
            }

            if (c >= 'a' && c <= 'f') {
                continue;
            }

            if (c >= 'A' && c <= 'F') {
                continue;
            }

            return false;
        }

        return true;
    }

    public static byte[] hexStrToBytes(String s) throws IllegalArgumentException {
        final int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("input is not a avaliable hex string:s=" + s);
        }

        int length = s.length() / 2;
        byte b[] = new byte[length];
        for (int i = 0; i < b.length; i++) {
            String tmp = s.substring(i * 2, i * 2 + 2);
            if (!isHexStr(tmp)) {
                throw new IllegalArgumentException("input is not a avaliable hex string:s=" + s);
            }

            b[i] = (byte) Integer.parseInt(tmp, 16);
        }

        return b;
    }

    public static long getDuration(Date begin, Date end) {
        if (null != end && null != begin) {
            return end.getTime() - begin.getTime();
        }
        else {
            return Long.MAX_VALUE;
        }
    }

    public static String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSS");
        return formatter.format(date);
    }

    public static boolean isNumber(String numStr) {
        Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");
        Matcher isNum = pattern.matcher(numStr);
        if (isNum.matches()) {
            return true;
        }

        return false;
    }

    public static String getQuarter(String month) {
        StringBuilder sb = new StringBuilder();
        if (month.compareTo(MONTH6) <= 0) {
            if (month.compareTo(MONTH3) <= 0) {
                sb.append(QUARTER1);
            }
            else {
                sb.append(QUARTER2);
            }
        }
        else {
            if (month.compareTo(MONTH9) <= 0) {
                sb.append(QUARTER3);
            }
            else {
                sb.append(QUARTER4);
            }
        }

        return sb.toString();
    }

    public static int getQuarterStartMonth(String quarterStr) {
        if (QUARTER1.equals(quarterStr)) {
            return 1;
        }
        else if (QUARTER2.equals(quarterStr)) {
            return 4;
        }
        else if (QUARTER3.equals(quarterStr)) {
            return 7;
        }
        else if (QUARTER4.equals(quarterStr)) {
            return 10;
        }
        else {
            throw new IllegalArgumentException("invalid quarter:" + quarterStr);
        }
    }

    public static String getCurrentYearMonth(Date date) {
        synchronized (CommonHelper.class) {
            return ymDateFormat.format(date);
        }
    }

    public static String getNextYearMonth(Date date) {
        synchronized (CommonHelper.class) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MONTH, 1);
            date = calendar.getTime();
            return ymDateFormat.format(date);
        }
    }

    public static String getCurrentMonth(Date date) {
        synchronized (CommonHelper.class) {
            return monthDateFormat.format(date);
        }
    }

    public static String getCurrentYear(Date date) {
        synchronized (CommonHelper.class) {
            return yearDateFormat.format(date);
        }
    }

    public static String getNextYear(Date date) {
        synchronized (CommonHelper.class) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.YEAR, 1);
            date = calendar.getTime();
            return yearDateFormat.format(date);
        }
    }

    public static String getShardingStr(ScmShardingType type, Date createDate) {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case YEAR:
                sb.append(CommonHelper.getCurrentYear(createDate));
                break;

            case MONTH:
                sb.append(CommonHelper.getCurrentYearMonth(createDate));
                break;

            case QUARTER:
                sb.append(CommonHelper.getCurrentYear(createDate));
                String month = CommonHelper.getCurrentMonth(createDate);
                sb.append(CommonHelper.getQuarter(month));
                break;

            default:
                // default do nothing
                break;
        }
        return sb.toString();
    }

    public static ScmMonthRange getMonthRange(ScmShardingType type, Date createDate) {
        String lowYearMonth;
        String upperYearMonth;

        switch (type) {
            case YEAR:
                lowYearMonth = CommonHelper.getCurrentYear(createDate) + CommonHelper.MONTH1;
                upperYearMonth = CommonHelper.getNextYear(createDate) + CommonHelper.MONTH1;
                break;

            case MONTH:
                lowYearMonth = CommonHelper.getCurrentYearMonth(createDate);
                upperYearMonth = CommonHelper.getNextYearMonth(createDate);
                break;

            default:
                String year = CommonHelper.getCurrentYear(createDate);
                String quarter = CommonHelper.getQuarter(CommonHelper.getCurrentMonth(createDate));

                if (quarter.equals(CommonHelper.QUARTER1)) {
                    lowYearMonth = year + CommonHelper.MONTH1;
                    upperYearMonth = year + CommonHelper.MONTH4;
                }
                else if (quarter.equals(CommonHelper.QUARTER2)) {
                    lowYearMonth = year + CommonHelper.MONTH4;
                    upperYearMonth = year + CommonHelper.MONTH7;
                }
                else if (quarter.equals(CommonHelper.QUARTER3)) {
                    lowYearMonth = year + CommonHelper.MONTH7;
                    upperYearMonth = year + CommonHelper.MONTH10;
                }
                else {
                    // QUARTER4
                    lowYearMonth = year + CommonHelper.MONTH10;

                    String nextYear = CommonHelper.getNextYear(createDate);
                    upperYearMonth = nextYear + CommonHelper.MONTH1;
                }
                break;
        }
        return new ScmMonthRange(lowYearMonth, upperYearMonth);
    }

    public static String getCurrentDay(Date date) {
        synchronized (CommonHelper.class) {
            return ymdDateFormat.format(date);
        }
    }

    public static String getDateBeforeDays(Date date, int days) {
        synchronized (CommonHelper.class) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_MONTH, -Math.abs(days));
            return ymdDateFormat.format(calendar.getTime());
        }
    }

    public static String getFullDateStr(Date date) {
        synchronized (CommonHelper.class) {
            return ymFullDateFormat.format(date);
        }
    }

    public static Date getDate(long date) {
        return new Date(date);
    }

    public static Date getDate(BSONTimestamp ts) {
        int seconds = ts.getTime();
        int inc = ts.getInc();

        long ms = (long) seconds * 1000 + inc / 1000;
        return new Date(ms);
    }

    public static boolean equals(List<String> left, List<String> right) {
        if (left == null || right == null) {
            if (left == null && right == null) {
                return true;
            }

            return false;
        }

        if (left.size() != right.size()) {
            return false;
        }

        Collections.sort(left);
        Collections.sort(right);
        for (int i = 0; i < left.size(); i++) {
            if (!left.get(i).equals(right.get(i))) {
                return false;
            }
        }

        return true;
    }

    public static long toLongValue(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        else {
            return (Long) value;
        }
    }

    public static long getUnsignedValue(int v) {
        return v & 0x0ffffffffL;
    }

    public static int readAsMuchAsPossible(InputStream is, byte[] buf, int offset, int length)
            throws IOException {
        if (offset < 0 || offset + length > buf.length) {
            throw new RuntimeException(
                    "bufferLength=" + buf.length + ",offset=" + offset + ",length=" + length);
        }

        if (length <= 0) {
            return 0;
        }

        int maxLength = offset + length;
        int realOffset = offset;
        int tempLength = 0;
        while (realOffset < maxLength && tempLength > -1) {
            tempLength = is.read(buf, realOffset, maxLength - realOffset);
            if (tempLength > 0) {
                realOffset += tempLength;
            }
        }

        if (realOffset > offset) {
            return realOffset - offset;
        }

        return -1;
    }

    public static int readAsMuchAsPossible(InputStream is, byte[] buf) throws IOException {
        return readAsMuchAsPossible(is, buf, 0, buf.length);
    }

    // 通过当前表的记录currentFileRec， 补全历史表中的记录 historyFileRec，返回一个完整的历史文件记录
    public static BSONObject completeHisotryFileRec(BSONObject historyFileRec,
            BSONObject currentFileRec) {
        BasicBSONObject ret = new BasicBSONObject();
        ret.putAll(currentFileRec);
        ret.putAll(historyFileRec);
        for (String key : FILE_FIELDS_NEED_CHECK_WHEN_MERGE) {
            // key是一个SCM系统迭代后新引入的文件字段，key 被设计为同时记录在历史表和当前表
            // 那么可能会存在这种兼容性场景：currentFileRec 包含 key 字段，historyFileRec 不含 key 字段
            // 以下处理将这种场景识别出来，并将合并后的记录中的 key 字段置 null
            if (!historyFileRec.containsField(key) && currentFileRec.containsField(key)) {
                ret.put(key, null);
            }
        }
        return ret;
    }
}
