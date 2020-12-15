package com.sequoiacm.tools.element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.common.ScmFiledDefine;
import com.sequoiacm.tools.exception.ScmExitCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeArgWrapper {
    private Pattern patternQ = Pattern.compile("^\\d{4}Q[1-4]$"); // 2017Q3
    private Pattern patternYM = Pattern.compile("^\\d{4,6}$");// 2017 or 201701
                                                              // or 20172
    private Logger logger = LoggerFactory.getLogger(TimeArgWrapper.class);

    private String shardingType;
    private Date upper;
    private Date lower;
    private String srcTime;

    public String getSrcTime() {
        return srcTime;
    }

    public TimeArgWrapper(String time) throws ScmToolsException {
        this.srcTime = time;
        init(time);
    }

    public boolean isContain(Date date) {
        int cpRes = lower.compareTo(date);
        if (cpRes == -1 || cpRes == 0) {
            cpRes = upper.compareTo(date);
            if (cpRes == 1) {
                return true;
            }
        }
        return false;
    }

    public boolean containOrEqual(TimeArgWrapper anotherTimeWrapper) {
        Date anotherLower = anotherTimeWrapper.getLower();
        Date anotherUpper = anotherTimeWrapper.getUpper();
        int cpRes = this.lower.compareTo(anotherLower);
        if (cpRes == -1 || cpRes == 0) {
            cpRes = this.upper.compareTo(anotherUpper);
            if (cpRes == 1 || cpRes == 0) {
                return true;
            }
        }
        return false;

    }

    public String getShardingType() {
        return shardingType;
    }

    public Date getUpper() {
        return upper;
    }

    public Date getLower() {
        return lower;
    }

    private void init(String time) throws ScmToolsException {
        String upperTime = time.toUpperCase();
        String ShardingType;
        Calendar calendar = Calendar.getInstance();
        Date lower = null;
        Date upper = null;
        if (patternQ.matcher(upperTime).find()) {
            ShardingType = ScmFiledDefine.WORKSPACE_SHARDING_QUARTER_STR;
            String year = upperTime.substring(0, 4);
            String quarter = upperTime.substring(4);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
            Date date = null;
            try {
                date = sdf.parse(year);
            }
            catch (ParseException e) {
                logger.error("parse year error,year:" + year, e);
                throw new ScmToolsException(
                        "parse year error,year:" + year + ",error:" + e.getMessage(),
                        ScmExitCode.PARSE_ERROR);
            }
            calendar.setTime(date);
            switch (quarter) {
                case ScmContentCommon.DateUtil.QUARTER1:
                    lower = date;
                    calendar.add(Calendar.MONTH, 3);
                    upper = calendar.getTime();
                    break;
                case ScmContentCommon.DateUtil.QUARTER2:
                    calendar.add(Calendar.MONTH, 3);
                    lower = calendar.getTime();
                    calendar.add(Calendar.MONTH, 3);
                    upper = calendar.getTime();
                    break;
                case ScmContentCommon.DateUtil.QUARTER3:
                    calendar.add(Calendar.MONTH, 6);
                    lower = calendar.getTime();
                    calendar.add(Calendar.MONTH, 3);
                    upper = calendar.getTime();
                    break;
                case ScmContentCommon.DateUtil.QUARTER4:
                    calendar.add(Calendar.MONTH, 9);
                    lower = calendar.getTime();
                    calendar.add(Calendar.MONTH, 3);
                    upper = calendar.getTime();
                    break;
                default:
                    // will not arrive here
                    throw new ScmToolsException("invalid arg time:" + time,
                            ScmExitCode.INVALID_ARG);
            }

        }
        else if (patternYM.matcher(upperTime).find()) {
            if (upperTime.length() == 4) {
                ShardingType = ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                try {
                    lower = sdf.parse(upperTime);
                    calendar.setTime(lower);
                    calendar.add(Calendar.YEAR, 1);
                    upper = calendar.getTime();
                }
                catch (ParseException e) {
                    logger.error("parse year error,year:" + upperTime, e);
                    throw new ScmToolsException(
                            "parse year error,year:" + upperTime + ",error:" + e.getMessage(),
                            ScmExitCode.PARSE_ERROR);
                }
            }
            else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
                ShardingType = ScmFiledDefine.WORKSPACE_SHARDING_MONTH_STR;
                String month = upperTime.substring(4);
                int monthInteger = ScmContentCommon.convertStrToInt(month);
                if (monthInteger > 12 || monthInteger < 1) {
                    throw new ScmToolsException("Invalid time:" + upperTime,
                            ScmExitCode.INVALID_ARG);
                }
                if (month.length() == 1) {
                    upperTime = upperTime.substring(0, 4) + "0" + upperTime.substring(4);
                }
                try {
                    lower = sdf.parse(upperTime);
                }
                catch (ParseException e) {
                    logger.error("parse year month error,year:" + upperTime, e);
                    throw new ScmToolsException(
                            "parse year month error,year:" + upperTime + ",error:" + e.getMessage(),
                            ScmExitCode.PARSE_ERROR);
                }
                calendar.setTime(lower);
                calendar.add(Calendar.MONTH, 1);
                upper = calendar.getTime();

            }
        }
        else {
            throw new ScmToolsException("invalid time arg:" + time, ScmExitCode.INVALID_ARG);
        }
        this.shardingType = ShardingType;
        this.lower = lower;
        this.upper = upper;
    }

    public String getTime() {
        return srcTime;
    }

}
