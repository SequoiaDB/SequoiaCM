package com.sequoiacm.contentserver.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.metasource.sequoiadb.MetaSequoiadbRecorder;
import com.sequoiacm.sequoiadb.dataservice.SequoiadbRecorder;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.common.ServiceDefine;

public class ScmLogResourceJob extends ScmBackgroundJob {
    private static final Logger logger = LoggerFactory.getLogger(ScmLogResourceJob.class);
    private static final Logger recorderLogger = LoggerFactory.getLogger(SequoiadbRecorder.class);
    private static final Logger metaRecorderLogger = LoggerFactory.getLogger(MetaSequoiadbRecorder.class);

    private boolean isRecording = false;
    private boolean isRecordingMeta = false;

    @Override
    public int getType() {
        return ServiceDefine.Job.JOB_TYPE_LOG_RESOURCE;
    }

    @Override
    public String getName() {
        return "ScmLogResourceJob";
    }

    @Override
    public long getPeriod() {
        return ServiceDefine.Job.LOG_RESOURCE_JOB_PERIOD;
    }

    private void checkSequoiadbRecorder() {
        if (recorderLogger.isDebugEnabled()) {
            if (!isRecording) {
                logger.info("start SequoiadbRecorder......");
                SequoiadbRecorder.getInstance().startRecord();
                isRecording = true;
            }
        }
        else {
            if (isRecording) {
                logger.info("stop SequoiadbRecorder");
                SequoiadbRecorder.getInstance().stopRecord();
                isRecording = false;
            }
        }
    }

    private void checkMetaSequoiadbRecorder() {
        if (metaRecorderLogger.isDebugEnabled()) {
            if (!isRecordingMeta) {
                logger.info("start MetaSequoiadbRecorder......");
                MetaSequoiadbRecorder.getInstance().startRecord();
                isRecordingMeta = true;
            }
        }
        else {
            if (isRecordingMeta) {
                logger.info("stop MetaSequoiadbRecorder");
                MetaSequoiadbRecorder.getInstance().stopRecord();
                isRecordingMeta = false;
            }
        }
    }

    @Override
    public void _run() {
        if (logger.isDebugEnabled()) {
            logger.debug("ScmSiteMgr:\n" + ScmContentServer.getInstance().getSiteMgr().toString());
            logger.debug("MetaSequoiadbRecorder:\n" + MetaSequoiadbRecorder.getInstance().toString());
            logger.debug("SequoiadbRecorder:\n" + SequoiadbRecorder.getInstance().toString());
        }

        checkSequoiadbRecorder();
        checkMetaSequoiadbRecorder();
    }

}
