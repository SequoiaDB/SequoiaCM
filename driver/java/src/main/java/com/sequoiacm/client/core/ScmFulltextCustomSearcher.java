package com.sequoiacm.client.core;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;

public class ScmFulltextCustomSearcher {
    private ScmWorkspace ws;
    private BSONObject fulltextCondition;
    private BSONObject fileCondition;
    private ScopeType scope = ScopeType.SCOPE_CURRENT;

    ScmFulltextCustomSearcher(ScmWorkspace ws) {
        this.ws = ws;
    }

    /**
     * Set the file scope.
     * @param scope
     *          file scope.
     * @return {@code this}
     * @throws ScmInvalidArgumentException
     *          if error happens.
     */
    public ScmFulltextCustomSearcher scope(ScopeType scope) throws ScmInvalidArgumentException {
        if (scope == null) {
            throw new ScmInvalidArgumentException("scope is null");
        }
        this.scope = scope;
        return this;
    }

    /**
     * Set the file condition.
     * @param fileCondition
     *          file condition.
     * @return  {@code this}
     */
    public ScmFulltextCustomSearcher fileCondition(BSONObject fileCondition) {
        this.fileCondition = fileCondition;
        return this;
    }

    /**
     * Set the fulltext condition.
     * @param fulltextCondition
     *          fulltext condition.
     * @return  {@code this}
     * @throws ScmInvalidArgumentException
     *          if error happens.
     */
    public ScmFulltextCustomSearcher fulltextCondition(BSONObject fulltextCondition)
            throws ScmInvalidArgumentException {
        if (fulltextCondition == null) {
            throw new ScmInvalidArgumentException("fulltextCondition is null");
        }
        this.fulltextCondition = fulltextCondition;
        return this;
    }

    /**
     * Performs search. 
     * @return A cursor to traverse
     * @throws ScmException
     *          if error happens.
     */
    public ScmCursor<ScmFulltextSearchResult> search() throws ScmException {
        if (fulltextCondition == null) {
            throw new ScmInvalidArgumentException("fulltextCondition is null");
        }
        BsonReader bsonReader = ws.getSession().getDispatcher().fulltextSearch(ws.getName(),
                scope.getScope(), fileCondition == null ? new BasicBSONObject() : fileCondition,
                fulltextCondition);
        return new ScmBsonCursor<ScmFulltextSearchResult>(bsonReader,
                new ScmFultextSearchResBsonConverter());
    }
}
