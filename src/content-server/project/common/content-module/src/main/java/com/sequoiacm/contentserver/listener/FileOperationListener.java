package com.sequoiacm.contentserver.listener;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.exception.ScmServerException;

public interface FileOperationListener {
    // 文件创建前会调用这个函数，若后续创建文件失败了，重试创建文件时会重复调用这个函数，所以函数实现要支持幂等性
    // 同时实现类不要赋予这个函数一些存储相关的操作，因为文件创建失败目前没有机制回滚这个函数所做的动作，如果确实需要，那么可以调整这个接口的定义，返回一个
    // Rollback 句柄，让文件创建失败时执行 Rollback
    public void preCreate(ScmWorkspaceInfo ws, BSONObject file) throws ScmServerException;

    public void postDelete(ScmWorkspaceInfo ws, List<BSONObject> allFileVersions)
            throws ScmServerException;

    public OperationCompleteCallback postCreate(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException;

    public OperationCompleteCallback postAddVersion(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException;

    public void postDeleteVersion(ScmWorkspaceInfo ws, BSONObject deletedVersion) throws ScmServerException;

    public OperationCompleteCallback postUpdate(ScmWorkspaceInfo ws, BSONObject fileInfo)
            throws ScmServerException;

    // 同 preCreate ，注意幂等性、不做要做存储相关的动作
    public void preAddVersion(ScmWorkspaceInfo ws, BSONObject newVersionFile)
            throws ScmServerException;

}
