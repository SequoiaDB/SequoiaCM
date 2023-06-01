package com.sequoiacm.cloud.adminserver.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.sequoiacm.cloud.adminserver.core.StatisticsServer;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.ContentServerInfo;

public class CommonUtils {

    public static final String DATE_FORMAT = "yyyyMMdd"; 
    public static final String MONTH_FORMAT = "yyyyMM"; 
    private static SimpleDateFormat ymdDateFmt = new SimpleDateFormat(DATE_FORMAT);
    private static SimpleDateFormat ymDateFmt = new SimpleDateFormat(MONTH_FORMAT);
    
    public static String getYesterdayStr() {
        synchronized (CommonUtils.class) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            return ymdDateFmt.format(calendar.getTime());
        }
    }
    
    public static String getTodayStr() {
        synchronized (CommonUtils.class) {
            return ymdDateFmt.format(new Date());
        }
    }
    
    public static Date getToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
    public static Date getYesterday(Date date) {
        return addDay(date, -1);
    }
    
    public static Date getTomorrow(Date date) {
        return addDay(date, 1);
    }
    
    public static Set<String> getDateRangeStr(String startDate, String endDate)
            throws StatisticsException {
        synchronized (CommonUtils.class) {
            Date startDt = null;
            Date endDt = null;
            try {
                startDt = ymdDateFmt.parse(startDate);
                endDt = ymdDateFmt.parse(endDate);
            }
            catch (ParseException e) {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "date format mismatch", e);
            }
    
            Set<String> dateStrs = new HashSet<>();
            if (startDt.after(endDt) || startDt.equals(endDt)) {
                dateStrs.add(startDate);
                return dateStrs;
            }
            
            Date tmpDt = startDt;
            while (!tmpDt.after(endDt)) {
                dateStrs.add(ymdDateFmt.format(tmpDt));
                tmpDt = addDay(tmpDt, 1);
            }
            
            return dateStrs;
        }
    }
    
    public static Set<Date> getDateRange(String startDate, String endDate)
            throws StatisticsException {
        synchronized (CommonUtils.class) {
            Date startDt = null;
            Date endDt = null;
            try {
                startDt = ymdDateFmt.parse(startDate);
                endDt = ymdDateFmt.parse(endDate);
            }
            catch (ParseException e) {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "date format mismatch", e);
            }
            
            return getDateRange(startDt, endDt);
        }
    }
    
    public static Set<Date> getDateRange(Date start, Date end)
            throws StatisticsException {
        Set<Date> dateSet = new LinkedHashSet<>();
        if (start.after(end) || start.equals(end)) {
            dateSet.add(start);
            return dateSet;
        }
        
        Date tmpDt = start;
        while (!tmpDt.after(end)) {
            dateSet.add(tmpDt);
            tmpDt = addDay(tmpDt, 1);
        }
        
        return dateSet;
    }
    
    public static Date parseCurrentDate(String date) throws StatisticsException {
        synchronized (CommonUtils.class) {
            try {
                return ymdDateFmt.parse(date);
            }
            catch (ParseException e) {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "date format mismatch", e);
            }
        }
    }
    
    public static String formatCurrentDate(Date date) {
        synchronized (CommonUtils.class) {
            return ymdDateFmt.format(date);
        }
    }
    
    public static String getCurrentMonth(Date date) {
        synchronized (CommonUtils.class) {
            return ymDateFmt.format(date);
        }
    }
    
    public static Date addDay(Date date, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, amount);
        return calendar.getTime();
    }
    
    public static <T> T getRandomElement(List<T> list) {
        if (null == list || list.isEmpty()) {
            return null;
        }

        Random r = new Random();
        int idx = r.nextInt(list.size());
        return list.get(idx);
    }

    public static List<ContentServerInfo> getConformServers(List<Integer> siteList,
            Map<Integer, List<ContentServerInfo>> allServerMap) throws StatisticsException {
        List<ContentServerInfo> conformServers = new ArrayList<>();
        for (Integer siteId : siteList) {
            List<ContentServerInfo> contentServerInfos = allServerMap.get(siteId);
            if (contentServerInfos == null) {
                throw new StatisticsException(StatisticsError.SITE_NOT_EXISTS,
                        "site is not exist,siteId=" + siteId);
            }
            conformServers.addAll(contentServerInfos);
        }
        return conformServers;
    }

    public static Map<Integer, List<ContentServerInfo>> getAllServersMap()
            throws StatisticsException {
        List<ContentServerInfo> allServers = StatisticsServer.getInstance().getContentServers();
        Map<Integer, List<ContentServerInfo>> map = new HashMap<>();
        for (ContentServerInfo serverInfo : allServers) {
            List<ContentServerInfo> serverInfoList = map.get(serverInfo.getSiteId());
            if (serverInfoList == null) {
                serverInfoList = new ArrayList<>();
                serverInfoList.add(serverInfo);
                map.put(serverInfo.getSiteId(), serverInfoList);
            }
            else {
                serverInfoList.add(serverInfo);
            }
        }
        return map;
    }
}
