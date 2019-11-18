package com.sequoiacm.client.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.common.ClientDefine.QueryOperators;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmError;

/**
 * Utility for creating BSONObject queries
 *
 * @since 2.1
 */
@SuppressWarnings("rawtypes")
public class ScmQueryBuilder {

    /**
     * Creates a builder with an empty query
     *
     * @since 2.1
     */
    public ScmQueryBuilder() {
        _query = new BasicBSONObject();
    }

    /**
     * Returns a new QueryBuilder.
     *
     * @return a builder
     * @since 2.1
     */
    public static ScmQueryBuilder start() {
        return new ScmQueryBuilder();
    }

    /**
     * Creates a new query with a document key
     *
     * @param key
     *            Key.
     *
     * @return {@code this}
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public static ScmQueryBuilder start(final String key) throws ScmException {
        return (new ScmQueryBuilder()).put(key);
    }

    /**
     * Adds a new key to the query if not present yet. Sets this key as the current key.
     *
     * @param key
     *            Key.
     *
     * @return {@code this}
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public ScmQueryBuilder put(final String key) throws ScmException {
        if (key == null) {
            throw new ScmInvalidArgumentException("key is null");
        }
        _currentKey = key;
        if (_query.get(key) == null) {
            _query.put(_currentKey, new NullObject());
        }
        return this;
    }

    /**
     * Equivalent to {@code QueryBuilder.put(key)}. Intended for compound query chains to be more
     * readable, e.g. {@code QueryBuilder.start("a").greaterThan(1).and("b").lessThan(3) }
     *
     * @param key
     *            Key.
     *
     * @return {@code this}
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public ScmQueryBuilder and(final String key) throws ScmException {
        return put(key);
    }

    /**
     * Equivalent to the $gt operator
     *
     * @param object
     *            Value to query
     *
     * @return {@code this}
     * @since 2.1
     */
    public ScmQueryBuilder greaterThan(final Object object) {
        addOperand(QueryOperators.GT, object);
        return this;
    }

    /**
     * Equivalent to the $elemMatch operator
     *
     * @param obj
     *            Value to query
     * @return {@code this}
     * @since 2.1
     */
    public ScmQueryBuilder elemMatch(BSONObject obj) {
        addOperand(QueryOperators.ELEM_MATCH, obj);
        return this;
    }

    /**
     * Equivalent to the $gte operator
     *
     * @param object
     *            Value to query
     * @return {@code this}
     * @since 2.1
     */
    public ScmQueryBuilder greaterThanEquals(final Object object) {
        addOperand(QueryOperators.GTE, object);
        return this;
    }

    /**
     * Equivalent to the $lt operand
     *
     * @param object
     *            Value to query
     * @return {@code this}
     * @since 2.1
     */
    public ScmQueryBuilder lessThan(final Object object) {
        addOperand(QueryOperators.LT, object);
        return this;
    }

    /**
     * Equivalent to the $lte operand
     *
     * @param object
     *            Value to query
     * @return {@code this}
     * @since 2.1
     */
    public ScmQueryBuilder lessThanEquals(final Object object) {
        addOperand(QueryOperators.LTE, object);
        return this;
    }

    /**
     * Equivalent of the find({key:value})
     *
     * @param object
     *            Value to query
     * @return {@code this}
     * @since 2.1
     */
    public ScmQueryBuilder is(final Object object) {
        addOperand(null, object);
        return this;
    }

    /**
     * Equivalent of the $ne operand
     *
     * @param object
     *            Value to query
     * @return {@code this}
     * @since 2.1
     */
    public ScmQueryBuilder notEquals(final Object object) {
        addOperand(QueryOperators.NE, object);
        return this;
    }

    /**
     * Equivalent of the $in operand
     *
     * @param ors
     *            Value to query
     * @return {@code this}
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public ScmQueryBuilder in(Object... ors) {
        // List l = (List) _query.get(QueryOperators.IN);
        // if (l == null) {
        // l = new ArrayList();
        // _query.put(QueryOperators.IN, l);
        // }
        // Collections.addAll(l, ors);
        List list = new ArrayList<Object>();
        for (Object obj : ors) {
            if (obj instanceof Collection) {
                list.addAll((Collection) obj);
            }
            else {
                list.add(obj);
            }
        }
        BSONObject bobj = new BasicBSONObject();
        bobj.put(QueryOperators.IN, list);
        addOperand(null, bobj);
        return this;
    }

    /**
     * Equivalent of the $nin operand
     *
     * @param ors
     *            Value to query
     * @return {@code this}
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public ScmQueryBuilder notIn(Object... ors) {
        List list = new ArrayList<Object>();
        for (Object obj : ors) {
            if (obj instanceof Collection) {
                list.addAll((Collection) obj);
            }
            else {
                list.add(obj);
            }
        }
        BSONObject bobj = new BasicBSONObject();
        bobj.put(QueryOperators.NIN, list);
        addOperand(null, bobj);
        return this;
    }

    /**
     * Equivalent of the $exists operand
     *
     * @param object
     *            Value to query
     * @return {@code this}
     * @since 2.1
     */
    public ScmQueryBuilder exists(final Object object) {
        addOperand(QueryOperators.EXISTS, object);
        return this;
    }

    /**
     * Equivalent of the $not operand
     *
     * @param ors
     *            Value to query
     * @return {@code this}
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public ScmQueryBuilder not(BSONObject... ors) {
        List l = (List) _query.get(QueryOperators.NOT);
        if (l == null) {
            l = new ArrayList();
            _query.put(QueryOperators.NOT, l);
        }
        Collections.addAll(l, ors);
        return this;
    }

    /**
     * Equivalent to an $or operand
     *
     * @param ors
     *            the list of conditions to or together
     * @return {@code this}
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public ScmQueryBuilder or(final BSONObject... ors) {
        List l = (List) _query.get(QueryOperators.OR);
        if (l == null) {
            l = new ArrayList();
            _query.put(QueryOperators.OR, l);
        }
        Collections.addAll(l, ors);
        return this;
    }

    /**
     * Equivalent to an $and operand
     *
     * @param ands
     *            the list of conditions to and together
     * @return {@code this}
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public ScmQueryBuilder and(final BSONObject... ands) {
        List l = (List) _query.get(QueryOperators.AND);
        if (l == null) {
            l = new ArrayList();
            _query.put(QueryOperators.AND, l);
        }
        Collections.addAll(l, ands);
        return this;
    }

    /**
     * Creates a {@code BSONObject} query to be used for the driver's find operations
     *
     * @return {@code BSONObject}
     * @throws ScmException
     *             If error happens
     * @since 2.1
     *
     */
    public BSONObject get() throws ScmException {
        for (final String key : _query.keySet()) {
            if (_query.get(key) instanceof NullObject) {
                throw new ScmException(ScmError.NO_OPERAND_FOR_KEY, "No operand for key:" + key);
            }
        }
        return _query;
    }

    private void addOperand(final String op, final Object value) {
        Object valueToPut = value;
        if (op == null) {
            if (_hasNot) {
                valueToPut = new BasicBSONObject(QueryOperators.NOT, valueToPut);
                _hasNot = false;
            }
            _query.put(_currentKey, valueToPut);
            return;
        }

        Object storedValue = _query.get(_currentKey);
        BasicBSONObject operand;
        if (!(storedValue instanceof BSONObject)) {
            operand = new BasicBSONObject();
            if (_hasNot) {
                BSONObject notOperand = new BasicBSONObject(QueryOperators.NOT, operand);
                _query.put(_currentKey, notOperand);
                _hasNot = false;
            }
            else {
                _query.put(_currentKey, operand);
            }
        }
        else {
            operand = (BasicBSONObject) _query.get(_currentKey);
            if (operand.get(QueryOperators.NOT) != null) {
                operand = (BasicBSONObject) operand.get(QueryOperators.NOT);
            }
        }
        operand.put(op, valueToPut);
    }

    private static class NullObject {
    }

    private final BSONObject _query;
    private String _currentKey;
    private boolean _hasNot;

}
