package com.infomaximum.database.core.iterator;

import com.infomaximum.database.core.anotation.Field;
import com.infomaximum.database.core.index.IndexUtils;
import com.infomaximum.database.core.structentity.HashStructEntities;
import com.infomaximum.database.core.structentity.StructEntity;
import com.infomaximum.database.core.structentity.StructEntityIndex;
import com.infomaximum.database.datasource.DataSource;
import com.infomaximum.database.datasource.KeyValue;
import com.infomaximum.database.domainobject.EntitySource;
import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectUtils;
import com.infomaximum.database.domainobject.key.FieldKey;
import com.infomaximum.database.domainobject.key.IndexKey;
import com.infomaximum.database.exeption.DataSourceDatabaseException;
import com.infomaximum.database.exeption.runtime.NotFoundIndexDatabaseException;
import com.infomaximum.database.utils.EqualsUtils;
import com.infomaximum.database.utils.TypeConvert;

import java.util.*;

/**
 * Created by kris on 30.04.17.
 */
public class IteratorFindEntityImpl<E extends DomainObject> implements IteratorEntity<E> {

    private final DataSource dataSource;
    private final Class<E> clazz;
    private final StructEntity structEntity;
    private final StructEntityIndex structEntityIndex;
    private final long indexIteratorId;
    private final List<Field> filterFields;
    private final List<Object> filterValues;

    private E nextElement;

    public IteratorFindEntityImpl(DataSource dataSource, Class<E> clazz, Map<String, Object> filters) throws DataSourceDatabaseException {
        this.dataSource = dataSource;
        this.clazz = clazz;
        this.structEntity = HashStructEntities.getStructEntity(clazz);
        this.structEntityIndex = structEntity.getStructEntityIndex(filters.keySet());

        checkIndex(filters);

        List<Field> filterFields = null;
        List<Object> filterValues = null;

        long[] values = new long[filters.size()];
        for (int i = 0; i < structEntityIndex.sortedFields.size(); ++i) {
            Field field = structEntityIndex.sortedFields.get(i);
            Object value = filters.get(field.name());
            values[i] = IndexUtils.buildHash(value, field.type());
            if (IndexUtils.toLongCastable(field.type())) {
                continue;
            }

            if (filterFields == null) {
                filterFields = new ArrayList<>();
                filterValues = new ArrayList<>();
            }

            filterFields.add(field);
            filterValues.add(value);
        }

        this.indexIteratorId = dataSource.createIterator(structEntityIndex.columnFamily, IndexKey.getKeyPrefix(values));
        this.filterFields = filterFields;
        this.filterValues = filterValues;

        nextElement = loadNextElement();
    }

    private E loadNextElement() throws DataSourceDatabaseException {
        while (true) {
            KeyValue keyValue = dataSource.next(indexIteratorId);
            if (keyValue == null) {
                return null;
            }

            IndexKey key = IndexKey.unpack(keyValue.getKey());
            if (checkFilter(key.getId())) {
                return DomainObjectUtils.buildDomainObject(dataSource, clazz, new EntitySource(key.getId(), null));
            }
        }
    }

    @Override
    public boolean hasNext() {
        return nextElement != null;
    }

    @Override
    public E next() throws DataSourceDatabaseException {
        if (nextElement == null) {
            throw new NoSuchElementException();
        }

        E element = nextElement;
        nextElement = loadNextElement();
        if (nextElement == null) {
            close();
        }

        return element;
    }

    @Override
    public void close() throws DataSourceDatabaseException {
        dataSource.closeIterator(indexIteratorId);
    }

    private void checkIndex(final Map<String, Object> filters) {
        if (structEntityIndex == null) {
            throw new NotFoundIndexDatabaseException(clazz, filters.keySet());
        }

        for (Field field : structEntityIndex.sortedFields) {
            Object filterValue = filters.get(field.name());
            if (filterValue != null && !EqualsUtils.equalsType(field.type(), filterValue.getClass())) {
                throw new RuntimeException("Not equals type field " + field.type() + " and type value " + filterValue.getClass());
            }
        }
    }

    private boolean checkFilter(long id) throws DataSourceDatabaseException {
        if (filterFields == null) {
            return true;
        }
        for (int i = 0; i < filterFields.size(); ++i) {
            Field field = filterFields.get(i);
            byte[] value = dataSource.getValue(structEntity.annotationEntity.name(), new FieldKey(id, field.name()).pack());
            if (!EqualsUtils.equals(filterValues.get(i), TypeConvert.get(field.type(), value))) {
                return false;
            }
        }

        return true;
    }
}
