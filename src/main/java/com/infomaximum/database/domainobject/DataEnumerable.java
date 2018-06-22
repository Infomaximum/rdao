package com.infomaximum.database.domainobject;

import com.infomaximum.database.provider.DBIterator;
import com.infomaximum.database.provider.DBProvider;
import com.infomaximum.database.domainobject.iterator.*;
import com.infomaximum.database.schema.Field;
import com.infomaximum.database.schema.Schema;
import com.infomaximum.database.provider.KeyPattern;
import com.infomaximum.database.provider.KeyValue;
import com.infomaximum.database.domainobject.filter.*;
import com.infomaximum.database.schema.StructEntity;
import com.infomaximum.database.utils.key.FieldKey;

import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.exception.UnexpectedEndObjectException;
import com.infomaximum.database.exception.runtime.IllegalTypeException;
import com.infomaximum.database.utils.TypeConvert;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Set;

public abstract class DataEnumerable {

    public static class NextState {

        private long nextId = -1;

        private NextState(long recordId) {
            this.nextId = recordId;
        }

        public boolean isEmpty() {
            return nextId == -1;
        }
    }

    protected final DBProvider dbProvider;

    DataEnumerable(DBProvider dbProvider) {
        this.dbProvider = dbProvider;
    }

    public DBProvider getDbProvider() {
        return dbProvider;
    }

    public abstract <T, U extends DomainObject> T getValue(final Field field, U object) throws DatabaseException;
    public abstract DBIterator createIterator(String columnFamily) throws DatabaseException;

    public <T extends DomainObject> T get(final Class<T> clazz, long id, final Set<String> loadingFields) throws DatabaseException {
        final String columnFamily = Schema.getEntity(clazz).getColumnFamily();

        try (DBIterator iterator = createIterator(columnFamily)) {
            return seekObject(DomainObject.getConstructor(clazz), loadingFields, iterator, FieldKey.buildKeyPattern(id, loadingFields));
        }
    }

    public <T extends DomainObject> T get(final Class<T> clazz, long id) throws DatabaseException {
        return get(clazz, id, null);
    }

    public <T extends DomainObject> IteratorEntity<T> find(final Class<T> clazz, Filter filter, final Set<String> loadingFields) throws DatabaseException {
        if (filter instanceof EmptyFilter) {
            return new AllIterator<>(this, clazz, loadingFields);
        } else if (filter instanceof HashFilter) {
            return new HashIndexIterator<>(this, clazz, loadingFields, (HashFilter)filter);
        } else if (filter instanceof PrefixFilter) {
            return new PrefixIndexIterator<>( this, clazz, loadingFields, (PrefixFilter)filter);
        } else if (filter instanceof IntervalFilter) {
            return new IntervalIndexIterator<>(this, clazz, loadingFields, (IntervalFilter) filter);
        } else if (filter instanceof RangeFilter) {
            return new RangeIndexIterator<>(this, clazz, loadingFields, (RangeFilter) filter);
        }

        throw new IllegalArgumentException("Unknown filter type " + filter.getClass());
    }

    public <T extends DomainObject> IteratorEntity<T> find(final Class<T> clazz, Filter filter) throws DatabaseException {
        return find(clazz, filter, null);
    }

    public <T extends DomainObject> T buildDomainObject(final Constructor<T> constructor, long id, Collection<String> preInitializedFields) {
        T obj = buildDomainObject(constructor, id);
        if (preInitializedFields == null) {
            for (Field field : obj.getStructEntity().getFields()) {
                obj._setLoadedField(field.getName(), null);
            }
        } else {
            for (String field : preInitializedFields) {
                obj._setLoadedField(field, null);
            }
        }
        return obj;
    }

    private <T extends DomainObject> T buildDomainObject(final Constructor<T> constructor, long id) {
        try {
            return constructor.newInstance(id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalTypeException(e);
        }
    }

    public <T extends DomainObject> T nextObject(final Constructor<T> constructor, Collection<String> preInitializedFields,
                                                 DBIterator iterator, NextState state) throws DatabaseException {
        if (state.isEmpty()) {
            return null;
        }

        T obj = buildDomainObject(constructor, state.nextId, preInitializedFields);
        state.nextId = readObject(obj, iterator);
        return obj;
    }

    public <T extends DomainObject> T seekObject(final Constructor<T> constructor, Collection<String> preInitializedFields,
                                                 DBIterator iterator, KeyPattern pattern) throws DatabaseException {
        KeyValue keyValue = iterator.seek(pattern);
        if (keyValue == null) {
            return null;
        }

        if (!FieldKey.unpackBeginningObject(keyValue.getKey())) {
            return null;
        }

        T obj = buildDomainObject(constructor, FieldKey.unpackId(keyValue.getKey()), preInitializedFields);
        readObject(obj, iterator);
        return obj;
    }

    public NextState seek(DBIterator iterator, KeyPattern pattern) throws DatabaseException {
        KeyValue keyValue = iterator.seek(pattern);
        if (keyValue == null) {
            return new NextState(-1);
        }

        if (!FieldKey.unpackBeginningObject(keyValue.getKey())) {
            return new NextState(-1);
        }

        return new NextState(FieldKey.unpackId(keyValue.getKey()));
    }

    private <T extends DomainObject> long readObject(T obj, DBIterator iterator) throws DatabaseException {
        KeyValue keyValue;
        while ((keyValue = iterator.next()) != null) {
            long id = FieldKey.unpackId(keyValue.getKey());
            if (id != obj.getId()) {
                if (!FieldKey.unpackBeginningObject(keyValue.getKey())) {
                    throw new UnexpectedEndObjectException(obj.getId(), id, FieldKey.unpackFieldName(keyValue.getKey()));
                }
                return id;
            }
            Field field = obj.getStructEntity().getField(new StructEntity.ByteArray(keyValue.getKey(), FieldKey.ID_BYTE_SIZE, keyValue.getKey().length));
            obj._setLoadedField(field.getName(), TypeConvert.unpack(field.getType(), keyValue.getValue(), field.getConverter()));
        }

        return -1;
    }
}
