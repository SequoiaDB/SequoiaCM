package com.sequoiacm.contentserver.pipeline.file;

import com.sequoiacm.exception.ScmServerException;

public class PipelineResult {

    public enum Status {
        SUCCESS,
        REDO_PIPELINE
    }

    public static PipelineResult redo(ScmServerException e) {
        return new PipelineResult(Status.REDO_PIPELINE, e, -1);
    }

    public static PipelineResult redo(ScmServerException e, int silenceTimeMsBeforeRedo) {
        return new PipelineResult(Status.REDO_PIPELINE, e, silenceTimeMsBeforeRedo);
    }

    public static PipelineResult success() {
        return new PipelineResult(Status.SUCCESS, null, -1);
    }

    private PipelineResult(Status status, ScmServerException cause, int silenceTimeMsBeforeRedo) {
        this.status = status;
        this.cause = cause;
        this.silenceTimeMsBeforeRedo = silenceTimeMsBeforeRedo;
    }

    private Status status;
    private ScmServerException cause;

    private final int silenceTimeMsBeforeRedo;

    public Status getStatus() {
        return status;
    }

    public ScmServerException getCause() {
        return cause;
    }

    public int getSilenceTimeMsBeforeRedo() {
        return silenceTimeMsBeforeRedo;
    }
}
