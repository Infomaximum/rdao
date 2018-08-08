package com.infomaximum.database.domainobject.iterator;

import com.infomaximum.database.domainobject.DataEnumerable;
import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.filter.BaseIntervalFilter;
import com.infomaximum.database.domainobject.filter.SortDirection;
import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.provider.DBIterator;
import com.infomaximum.database.provider.KeyPattern;
import com.infomaximum.database.provider.KeyValue;
import com.infomaximum.database.schema.*;
import com.infomaximum.database.utils.HashIndexUtils;
import com.infomaximum.database.utils.IntervalIndexUtils;
import com.infomaximum.database.utils.key.BaseIntervalIndexKey;

import java.util.*;

abstract class BaseIntervalIndexIterator<E extends DomainObject, F extends BaseIntervalFilter> extends BaseIndexIterator<E> {

    private final List<Field> checkedFilterFields;
    private final List<Object> filterValues;
    private final DBIterator.StepDirection direction;
    private final KeyPattern indexPattern;

    private KeyValue indexKeyValue;

    final long filterBeginValue, filterEndValue;

    BaseIntervalIndexIterator(DataEnumerable dataEnumerable,
                              Class<E> clazz,
                              Set<Integer> loadingFields,
                              SortDirection direction,
                              F filter) throws DatabaseException {
        super(dataEnumerable, clazz, loadingFields);
        this.direction = direction == SortDirection.ASC ? DBIterator.StepDirection.FORWARD : DBIterator.StepDirection.BACKWARD;

        StructEntity structEntity = Schema.getEntity(clazz);
        Map<Integer, Object> filters = filter.getHashedValues();
        BaseIntervalIndex index = getIndex(filter, structEntity);

        List<Field> filterFields = null;
        List<Object> filterValues = null;

        final List<Field> hashedFields = index.getHashedFields();
        long[] values = new long[hashedFields.size()];
        for (int i = 0; i < hashedFields.size(); ++i) {
            Field field = hashedFields.get(i);
            Object value = filters.get(field.getNumber());
            if (value != null) {
                field.throwIfNotMatch(value.getClass());
            }

            values[i] = HashIndexUtils.buildHash(field.getType(), value, field.getConverter());
            if (HashIndexUtils.toLongCastable(field.getType())) {
                continue;
            }

            if (filterFields == null) {
                filterFields = new ArrayList<>();
                filterValues = new ArrayList<>();
            }

            filterFields.add(field);
            filterValues.add(value);
        }

        index.checkIndexedValueType(filter.getBeginValue().getClass());
        index.checkIndexedValueType(filter.getEndValue().getClass());

        this.checkedFilterFields = filterFields != null ? filterFields : Collections.emptyList();
        this.filterValues = filterValues;

        this.dataKeyPattern = buildDataKeyPattern(filterFields, loadingFields, structEntity);
        if (this.dataKeyPattern != null) {
            this.dataIterator = dataEnumerable.createIterator(structEntity.getColumnFamily());
        }

        this.filterBeginValue = IntervalIndexUtils.castToLong(filter.getBeginValue());
        this.filterEndValue = IntervalIndexUtils.castToLong(filter.getEndValue());
        IntervalIndexUtils.checkInterval(filterBeginValue, filterEndValue);
        this.indexIterator = dataEnumerable.createIterator(index.columnFamily);

        switch (this.direction) {
            case FORWARD:
                this.indexPattern = BaseIntervalIndexKey.buildLeftBorder(values, filterBeginValue);
                break;
            case BACKWARD:
                this.indexPattern = BaseIntervalIndexKey.buildRightBorder(values, filterEndValue);
                break;
            default:
                throw new IllegalArgumentException("direction = " + direction);
        }
        this.indexKeyValue = seek(indexIterator, indexPattern);

        nextImpl();
    }

    abstract BaseIntervalIndex getIndex(F filter, StructEntity entity);
    abstract KeyValue seek(DBIterator indexIterator, KeyPattern pattern) throws DatabaseException;

    @Override
    void nextImpl() throws DatabaseException {
        while (indexKeyValue != null) {
            final long id = BaseIntervalIndexKey.unpackId(indexKeyValue.getKey());
            final int res = matchKey(id, indexKeyValue.getKey());
            if (res == KeyPattern.MATCH_RESULT_SUCCESS) {
                nextElement = findObject(id);
            } else if (res == KeyPattern.MATCH_RESULT_CONTINUE) {
                nextElement = null;
            } else {
                break;
            }
            indexKeyValue = indexIterator.step(direction);
            if (indexKeyValue != null && indexPattern.match(indexKeyValue.getKey()) != KeyPattern.MATCH_RESULT_SUCCESS) {
                indexKeyValue = null;
            }
            if (nextElement != null) {
                return;
            }
        }

        nextElement = null;
        close();
    }

    /**
     * @return KeyPattern.MATCH_RESULT_*
     */
    abstract int matchKey(long id, byte[] key);

    @Override
    boolean checkFilter(E obj) throws DatabaseException {
        for (int i = 0; i < checkedFilterFields.size(); ++i) {
            Field field = checkedFilterFields.get(i);
            if (!HashIndexUtils.equals(field.getType(), filterValues.get(i), obj.get(field.getNumber()))) {
                return false;
            }
        }

        return true;
    }
}

