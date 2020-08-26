package com.sequoiacm.fulltext.server.operator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;

@Component
public class FulltextIdxOperatorMgr {

    private Map<ScmFulltextStatus, FulltextIdxOperator> status2Operator;

    @Autowired
    public FulltextIdxOperatorMgr(List<FulltextIdxOperator> operators) {
        status2Operator = new HashMap<>();
        for (FulltextIdxOperator op : operators) {
            status2Operator.put(op.operatorForStatus(), op);
        }
    }

    public FulltextIdxOperator getOperator(ScmFulltextStatus status) {
        FulltextIdxOperator op = status2Operator.get(status);
        if (op == null) {
            throw new IllegalArgumentException("no such operator:" + status);
        }
        return op;
    }
}
