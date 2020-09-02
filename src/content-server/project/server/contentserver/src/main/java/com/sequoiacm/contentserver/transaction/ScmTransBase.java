package com.sequoiacm.contentserver.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.job.ScmJobManager;

public abstract class ScmTransBase {
	private static final Logger logger = LoggerFactory.getLogger(ScmTransBase.class);
	private ScmJobManager jobManager = ScmJobManager.getInstance();

	//throw exception safely
	protected abstract void _lock() throws ScmServerException;
	protected abstract void _execute() throws ScmServerException;
	protected abstract void _innerRollback() throws ScmServerException;

	protected abstract void _unlock();
	protected abstract boolean _isInTransStatus() throws ScmServerException;

	public abstract int getSiteID();
	public abstract String getWorkspaceName();
	public abstract String getTransID();

	public void execute() throws ScmServerException {
		_lock();

		try {
			_execute();
		}
		catch (Exception e1) {
			logger.error("execute failed", e1);
			// execute failed, start to roll back.
			try {
				_innerRollback();
			}
			catch (Exception e2) {
				logger.error("roll back failed", e2);
				// roll back failed, start to run roll back in background
				startRollbackJob(getSiteID(), getWorkspaceName(), getTransID());
				return;
			}
			throw e1;
		}
		finally {
			_unlock();
		}
	}

	public void rollback() {
		try {
			_lock();
		}
		catch (Exception e) {
			logger.error("lock failed,start background job to rollback again", e);
			startRollbackJob(getSiteID(), getWorkspaceName(), getTransID());
			return;
		}

		// start to roll back.
		try {
			if (!_isInTransStatus()) {
				// check if need to roll back or not
				return;
			}

			_innerRollback();
		}
		catch (Exception e) {
			logger.error("rollback failed,start background job to rollback again", e);
			// roll back failed, start to run roll back in background
			startRollbackJob(getSiteID(), getWorkspaceName(), getTransID());
			return;
		}
		finally {
			_unlock();
		}
	}

	protected final void removeTransLog(ScmContentServer contentServer, int siteID,
			String workspaceName, String transID) {
		try {
			contentServer.deleteTransLog(workspaceName, transID);
		}
		catch (Exception e) {
			logger.warn("remove trans log failed:id=" + transID, e);
		}
	}

	protected final void startRollbackJob(int siteID, String workspaceName, String transID) {
		try {
			logger.info("start background job for rollback:transID=" + transID);
			jobManager.addRollbackID(siteID, workspaceName, transID);
		}
		catch (Exception e) {
			logger.error("schedule job failed:transID=" + transID, e);
			System.exit(-1);
		}
	}
}
