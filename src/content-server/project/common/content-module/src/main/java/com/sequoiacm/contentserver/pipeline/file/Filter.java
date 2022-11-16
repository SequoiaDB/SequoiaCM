package com.sequoiacm.contentserver.pipeline.file;

import com.sequoiacm.exception.ScmServerException;

public interface Filter<C> {

    // Pipeline
    //    step1：触发每个filter的准备阶段
    //    step2：触发每个filter的执行阶段


    // 准备阶段：定义一些前置处理检查操作，检查结果可以存入上下文，检查结果可以被每个 filter 的执行阶段读取到
    default void preparePhase(C context) throws ScmServerException{
    }

    // 执行阶段：定义主要的业务元数据操作
    PipelineResult executionPhase(C context) throws ScmServerException;
}
