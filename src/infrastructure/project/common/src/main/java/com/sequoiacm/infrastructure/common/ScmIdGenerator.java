package com.sequoiacm.infrastructure.common;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmIdGenerator {

    public static class RequestId {
        private static AtomicLong requestId = new AtomicLong();

        public static long get() {
            return requestId.incrementAndGet();
        }
    }

    public static class SessionId {
        public static String get() {
            return FileId.get(new Date());
        }

        public static String parseString(String id) {
            return FileId.parseString(id).toString();
        }
    }

    public static class TransactionId {
        public static String get() {
            return FileId.get(new Date());
        }

        public static String parseString(String id) {
            return FileId.parseString(id).toString();
        }
    }

    public static class MapId {
        public static String get() {
            return FileId.get(new Date());
        }
    }

    public static class TaskId {
        public static String get() {
            return FileId.get(new Date());
        }
    }

    public static class BatchId {
        public static String get(Date date) {
            return FileId.get(date);
        }
    }
    
    public static class ClassId {
        public static String get(Date date) {
            return FileId.get(date);
        }
    }
    
    public static class AttrId {
        public static String get(Date date) {
            return FileId.get(date);
        }
    }

    public static class DirectoryId {
        public static String get() {
            return FileId.get(new Date());
        }
    }

    public static class ScheduleId {
        public static String get(Date date) {
            return FileId.get(date);
        }
    }

    public static class FileId {
        private static final Logger logger = LoggerFactory.getLogger(FileId.class);

        private static boolean isInit = false;
        private static AtomicInteger serial = new AtomicInteger(new Random().nextInt());

        private static byte clusterId = 0;
        private static short contentServerId = 0;
        private static final int TOTAL_BYTE_LENGTH = 12;

        // old version:
        // --------0x 00 00 00 00 | -------------00 | ----------00 00 | -------00 00 00 | 00 00
        // ---seconds(32bits,0~31)| clusterId(8bits)| serverId(16bits)| threadId(24bits)| serial(16bits)

        // new version:clusterId's 6~7 bits for version value(v0:0x00, v1:0x40, v2:0x80,
        // v3:0xC0), 4~5 bits for timezone value(0: null,1: Asia/Shanghai)
        // --------0x 00 00 00 00 | -------------C0 | ----------00 00 | ---------------------00 | 00 00 00 00
        // seconds(low32bits,0~31)| clusterId(8bits)| serverId(16bits)| seconds(high8bits,32~39)| serial(32bits)

        private static final int ID_VERSION_FLAG = 0xFFFFFFC0;
        private static final int ID_VERSION_1 = 1;
        private static final int ID_TIMEZONE_1 = 1;
        private static final int ID_VERSION_BIT_WEIGHT = 6;
        private static final int ID_TIMEZONE_BIT_WEIGHT = 4;
        // 00110000
        private static final int ID_TIMEZONE_MASK = 0x30;
        private static final int ID_CLSUTER_FLAG = ~ID_VERSION_FLAG;

        // Thu Jan 01 00:00:00 CST 9998
        private static final long ID_MAX_SECONDS = 253339200000L;
        // Sat Jan 01 00:00:00 CST 1
        private static final long ID_MIN_SECONDS = -62135798400L;

        public static void init(int clusterId, int contentServerId) throws Exception {
            if ((clusterId & ID_VERSION_FLAG) > 0) {
                throw new Exception("clusterId can't be greater than " + ID_CLSUTER_FLAG);
            }

            if (contentServerId > 0x7FFF || contentServerId < 0) {
                throw new Exception("contentServerId is invalid:id=" + contentServerId + ",valid"
                        + "(0," + 0x7FFF + "]");
            }

            FileId.clusterId = (byte) clusterId;
            FileId.contentServerId = (short) contentServerId;
            isInit = true;
        }

        public static void checkCreateTime(long seconds) {
            if (seconds > ID_MAX_SECONDS || seconds < ID_MIN_SECONDS) {
                throw new RuntimeException("seconds is out of bounds:seconds=" + seconds + ",max="
                        + ID_MAX_SECONDS + ",min=" + ID_MIN_SECONDS);
            }
        }

        static String generateId(long seconds, int inc) {
            checkCreateTime(seconds);

            byte[] total = new byte[TOTAL_BYTE_LENGTH];
            ByteBuffer bb = ByteBuffer.wrap(total);
            // seconds(0 ~ 31 bits)
            putInt(bb, (int) seconds);

            // highest 0-1 bits is version, 2-3 bits is timezone, 4-7 bits is clusterId
            byte versionTimezoneClusterId = (byte) (clusterId
                    | (ID_TIMEZONE_1 << ID_TIMEZONE_BIT_WEIGHT)
                    | (ID_VERSION_1 << ID_VERSION_BIT_WEIGHT));
            putByte(bb, versionTimezoneClusterId);

            // serverId
            putShort(bb, contentServerId);

            // seconds(32 ~ 39 bits)
            putByte(bb, (byte) (seconds >> 32));

            // increase
            putInt(bb, inc);

            return byteArrayToString(total);
        }

        static String generateIdV0(long seconds, int inc) {
            if ((seconds >> 32 & 0xFFFF) > 0) {
                throw new RuntimeException("seconds is out of bounds:seconds=" + seconds);
            }

            byte[] total = new byte[TOTAL_BYTE_LENGTH];
            ByteBuffer bb = ByteBuffer.wrap(total);

            // seconds(0 ~ 31 bits)
            putInt(bb, (int) seconds);

            putByte(bb, clusterId);

            // serverId
            putShort(bb, contentServerId);

            // threadId(0 ~ 23 bits)
            int threadId = (int) Thread.currentThread().getId();
            putInt24Bits(bb, threadId);

            putShort(bb, (short) inc);

            return byteArrayToString(total);
        }

        public static ScmParesedId parseString(String id) {
            if (!isValid(id)) {
                throw new RuntimeException("id is invalid:id=" + id);
            }

            byte b[] = new byte[TOTAL_BYTE_LENGTH];
            for (int i = 0; i < b.length; i++) {
                b[i] = (byte) Integer.parseInt(id.substring(i * 2, i * 2 + 2), 16);
            }

            ByteBuffer bb = ByteBuffer.wrap(b);

            long secondsLow32Bits = getUnsignedInt(bb);
            int clusterId = getByte(bb) & 0x0FF;
            int contentServerid = getShort(bb);

            int version = clusterId >> ID_VERSION_BIT_WEIGHT;
            int timezone = (clusterId & ID_TIMEZONE_MASK) >> ID_TIMEZONE_BIT_WEIGHT;

            long seconds = 0;
            int threadId = 0;
            int serial = 0;
            if (version == 0) {
                threadId = getInt24Bits(bb);
                serial = getShort(bb);
                seconds = secondsLow32Bits;
            }
            else if (version == ID_VERSION_1) {
                long secondsHigh16Bits = getSignByte(bb);
                seconds = secondsHigh16Bits << 32 | secondsLow32Bits;
                serial = getInt(bb);

                clusterId = clusterId & ID_CLSUTER_FLAG;
            }

            return new ScmParesedId(id, seconds, clusterId, contentServerid, threadId, serial,
                    timezone);
        }

        private static String byteArrayToString(byte[] b) {
            StringBuilder buf = new StringBuilder(TOTAL_BYTE_LENGTH * 2);
            for (int i = 0; i < b.length; i++) {
                int x = b[i] & 0xFF;
                String s = Integer.toHexString(x);
                if (s.length() == 1) {
                    buf.append("0");
                }
                buf.append(s);
            }

            return buf.toString();
        }

        private static void putInt(ByteBuffer bb, int value) {
            bb.putInt(value);
        }

        private static int getInt(ByteBuffer bb) {
            return bb.getInt();
        }

        private static long getUnsignedInt(ByteBuffer bb) {
            return getInt(bb) & 0x0FFFFFFFFL;
        }

        private static void putInt24Bits(ByteBuffer bb, int value) {
            // 0x12345678(get lower 24bits from 32bits)
            bb.put((byte) (value >> 16 & 0xFF)); // 0x34
            bb.put((byte) (value >> 8 & 0xFF)); // 0x56
            bb.put((byte) (value & 0xFF)); //0x78
        }

        private static int getInt24Bits(ByteBuffer bb) {
            int value = 0;
            value = bb.get() & 0xFF;
            value = value << 8 | bb.get() & 0xFF;
            value = value << 8 | bb.get() & 0xFF;

            return value;
        }

        private static void putByte(ByteBuffer bb, byte value) {
            bb.put(value);
        }

        private static byte getByte(ByteBuffer bb) {
            return bb.get();
        }

        private static int getUnsignByte(ByteBuffer bb) {
            return getByte(bb) & 0x0FF;
        }

        private static int getSignByte(ByteBuffer bb) {
            return getByte(bb);
        }

        private static void putShort(ByteBuffer bb, short value) {
            bb.putShort(value);
        }

        private static short getShort(ByteBuffer bb) {
            return bb.getShort();
        }

        private static int getUnsighedShort(ByteBuffer bb) {
            return getShort(bb) & 0x0FFFF;
        }

        public static String get(Date createDate) {
            if (!isInit) {
                throw new RuntimeException("id generater is uninitialized");
            }

            long seconds = createDate.getTime() / 1000;
            int inc = serial.incrementAndGet();

            return generateId(seconds, inc);
        }

        private static boolean isValid(String s) {
            if (s == null) {
                return false;
            }

            final int len = s.length();
            if (len != TOTAL_BYTE_LENGTH * 2) {
                return false;
            }

            for (int i = 0; i < len; i++) {
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
    }

    public static class GlobalId {
        public static String get() {
            return "";
        }
    }

    public static long getUnsignedValue(int v) {
        return v & 0x0ffffffffL;
    }

    private static void checkId(Date date, int clusterId, int serverId, int serial) {
        String id = ScmIdGenerator.FileId.get(date);
        ScmParesedId mid = ScmIdGenerator.FileId.parseString(id);
        System.out.println("mid=" + mid + ",date=" + date);
        if (date.getTime() / 1000 != mid.getSeconds()) {
            throw new RuntimeException("real seconds=" + date.getTime() / 1000);
        }

        if (clusterId != mid.getClusterId()) {
            throw new RuntimeException("real clusterId=" + clusterId);
        }

        if (serverId != mid.getServerId()) {
            throw new RuntimeException("real serverId=" + serverId);
        }

        if (serial != mid.getSerial()) {
            throw new RuntimeException("real serial=" + serial);
        }
    }
}
