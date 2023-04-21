package com.sequoiacm.infrastructure.common;

public class ScmQuotaUtils {

    public static long convertToBytes(String input) {
        if (input == null || input.length() < 2) {
            throw new IllegalArgumentException("invalid value: " + input);
        }

        char unit = input.toLowerCase().charAt(input.length() - 1);
        long multiplier;
        switch (unit) {
            case 'm':
                multiplier = 1024L * 1024L;
                break;
            case 'g':
                multiplier = 1024L * 1024L * 1024L;
                break;
            default:
                throw new IllegalArgumentException("unsupported unit: " + unit);
        }

        String numberStr = input.substring(0, input.length() - 1);
        long number;
        try {
            number = Long.parseLong(numberStr);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid value: " + input);
        }

        if (number > 0 && Long.MAX_VALUE / multiplier < number) {
            throw new IllegalArgumentException("value is too big: " + number + unit);
        }
        else if (number < 0 && Long.MIN_VALUE / multiplier > number) {
            throw new IllegalArgumentException("value is too small: " + number + unit);
        }

        return number * multiplier;
    }

    public static long parseMaxSize(String maxSize) {
        try {
            return convertToBytes(maxSize);
        }
        catch (IllegalArgumentException e) {
            if (isNegativeNumber(maxSize)) {
                return -1;
            }
            else {
                throw e;
            }
        }

    }

    private static boolean isNegativeNumber(String maxSize) {
        try {
            long number = Long.parseLong(maxSize);
            return number < 0;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    public static String formatSize(long sizeBytes) {
        String[] units = { "B", "KB", "MB", "GB" };
        int unitIndex = 0;

        double size = (double) Math.abs(sizeBytes);
        boolean isNegative = sizeBytes < 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        String res = String.format("%.2f%s", size, units[unitIndex]);
        if (isNegative) {
            res = "-" + res;
        }
        return res;
    }
}
