package com.sequoiacm.contentserver.pipeline.file;

import com.sequoiacm.contentserver.pipeline.file.batch.OverwriteFileBatchFilter;
import com.sequoiacm.contentserver.pipeline.file.bucket.OverwriteFileBucketFilter;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileContext;
import com.sequoiacm.contentserver.pipeline.file.core.OverwriteFileCoreFilter;
import com.sequoiacm.contentserver.pipeline.file.dir.OverwriteFileDirFilter;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pipeline<C> {
    private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);
    private List<FilterComparableWrapper<C>> filters = new ArrayList<>();
    private volatile boolean filtersIsSorted = false;

    public PipelineResult execute(C context) throws ScmServerException {
        if (!filtersIsSorted) {
            sortFilters();
        }

        // Pipeline 触发 filter 前的阶段：用于向上下文注入一些本次 Pipeline 执行的必备参数
        preInvokeFilter(context);

        // 触发 filter
        PipelineResult ret = invokeFilter(context);

        // Pipeline 触发 filter 后的阶段：可以检查从上下文中检查本次触发是否产生了必要的输出（如 updatePipeline
        // 将会在这个阶段检查指定版本是否被更新，并登记到上下文中）
        postInvokeFilter(ret, context);

        return ret;

    }

    private PipelineResult invokeFilter(C context) throws ScmServerException {
        // Pipeline 触发每个 filter 自己的准备阶段
        filterPreparePhase(context);

        // Pipeline 触发每个 filter 自己的执行阶段
        return filterExecutionPhase(context);
    }

    private synchronized void sortFilters() {
        if (filtersIsSorted) {
            return;
        }
        Collections.sort(filters);
        filtersIsSorted = true;
    }

    void preInvokeFilter(C context) throws ScmServerException {
    }

    void postInvokeFilter(PipelineResult pipelineResult, C context) throws ScmServerException {
    }

    final void filterPreparePhase(C context) throws ScmServerException {
        for (FilterComparableWrapper<C> f : filters) {
            f.getInnerFilter().preparePhase(context);
        }
    }

    final PipelineResult filterExecutionPhase(C context) throws ScmServerException {
        for (FilterComparableWrapper<C> f : filters) {
            PipelineResult res = f.getInnerFilter().executionPhase(context);
            if (res == PipelineResult.REDO_PIPELINE) {
                return res;
            }
        }
        return PipelineResult.SUCCESS;
    }

    public String getFiltersDesc() {
        if (!filtersIsSorted) {
            sortFilters();
        }
        return filters.toString();
    }

    public void addFilter(FilterComparableWrapper<C> filter) {
        this.filters.add(filter);
    }
}
