package com.sequoiacm.contentserver.pipeline.file;

import com.sequoiacm.exception.ScmServerException;

public class PipelineResult {
    public enum Status {
        SUCCESS,
        REDO_PIPELINE
    }

    public static PipelineResult redo(ScmServerException e) {
        return new PipelineResult(Status.REDO_PIPELINE, e);
    }

    public static PipelineResult success() {
        return new PipelineResult(Status.SUCCESS, null);
    }

    public PipelineResult(Status status, ScmServerException cause) {
        this.status = status;
        this.cause = cause;
    }

    private Status status;
    private ScmServerException cause;

    public Status getStatus() {
        return status;
    }

    public ScmServerException getCause() {
        return cause;
    }
}
