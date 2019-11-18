package com.sequoiacm.perf.operation;

import com.sequoiacm.perf.common.ApiMethod;
import com.sequoiacm.perf.common.ApiType;
import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.tool.PoiExcelExport;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.vo.RecordVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.ansi;

public abstract class BaseOperation {

    public static final Logger logger = LoggerFactory.getLogger(BaseOperation.class);

    public String getDownloadPath(Config config) {
        File downloadPath;
        if (StringUtils.isEmpty(config.getFileDownloadPath())) {
            downloadPath = new File(System.getProperty("user.dir") + File.separator + "scm_perf_download");
        } else {
            downloadPath = new File(config.getFileDownloadPath());
        }

        if (!downloadPath.isDirectory()) {
            downloadPath.mkdir();
        }
        return downloadPath.getPath();
    }

    public void recordToText() {
        Recorder recorder = Recorder.getInstance();
        Map<String, RecordVo> recordMap = recorder.getRecordMap();
        FileWriter fw = null;
        File perf = new File(System.getProperty("user.dir")
                + File.separator + "scm_per_data.log");

        try {
            fw = new FileWriter(perf, true);
            PrintWriter pw = new PrintWriter(fw);
            pw.append("线程").append("\t")
                    .append("文件数").append("\t")
                    .append("文件大小(K)").append("\t")
                    .append("线程内操作总耗时(ms)").append("\t")
                    .append("请求平均耗时(ms)").append("\n");
            for (String s : recordMap.keySet()) {
                RecordVo record = recordMap.get(s);
                pw.append(record.getThreadName()).append("\t")
                        .append(String.valueOf(record.getFileNum())).append("\t")
                        .append(String.valueOf(record.getFileSize())).append("\t")
                        .append(String.valueOf(record.getTotal())).append("\t")
                        .append(String.valueOf(record.getAverage())).append("\n");
            }
            pw.flush();
            fw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void recordToExcel(String sheetName) {
        Recorder recorder = Recorder.getInstance();
        Map<String, RecordVo> recordMap = recorder.getRecordMap();
        PoiExcelExport sheet = new PoiExcelExport(System.getProperty("user.dir") + File.separator +
                "scm_perf_data" + System.currentTimeMillis() / 1000 + ".xls", sheetName);
        List<RecordVo> recordVoList = new ArrayList<>(recordMap.size());
        for (RecordVo recordVo : recordMap.values()) {
            recordVoList.add(recordVo);
        }
        String[] titleColumn = {"threadName", "fileNum", "fileSize", "total", "average"};
        String[] titleName = {"线程", "文件数", "文件大小(K)", "线程内操作总耗时(ms)", "请求平均耗时(ms)"};
        int[] titleSize = {13, 13, 13, 13, 13};

        sheet.wirteExcel(titleColumn, titleName, titleSize, recordVoList);

    }

    public void printSummarize(int threadNum, long duration, int fileNum, long fileSize, long average) {
        String summarize = String.format("totalTime=%dms; threadNum=%d; fileNum=%d; fileSize=%dK; avgRespTime=%dms",
                duration, threadNum, fileNum, fileSize, average);

        FileWriter fw = null;
        try {
            File perf = new File(System.getProperty("user.dir")
                    + File.separator + "scm_per_data.log");
            fw = new FileWriter(perf, true);
            PrintWriter pw = new PrintWriter(fw);
            pw.append("==================================").append("\n");
            pw.append("summarize:").append("\n").append(summarize).append("\n");
            pw.flush();
            fw.flush();
            pw.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(ansi().fgDefault().bgDefault());
        System.out.println(ansi().fg(GREEN).a(summarize));
        System.out.println(ansi().fgDefault().bgDefault());
    }

    public List<Integer> allocateTask(Config config) {
        List<Integer> threadTaskNum = new ArrayList<>(config.getThreadNum());
        int num = config.getFileNum() / config.getThreadNum();

        for (int i = 0; i < config.getThreadNum(); i++) {
            threadTaskNum.add(num);
        }
        int remainTask = config.getFileNum() - num*config.getThreadNum();


        for (int i = 0; i < remainTask; i++) {
            threadTaskNum.set(i, threadTaskNum.get(i) + 1);
        }
        return threadTaskNum;
    }

}
