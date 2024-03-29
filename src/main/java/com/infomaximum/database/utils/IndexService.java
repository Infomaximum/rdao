package com.infomaximum.database.utils;

import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectSource;
import com.infomaximum.database.domainobject.filter.EmptyFilter;
import com.infomaximum.database.domainobject.iterator.IteratorEntity;
import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.exception.SchemaException;
import com.infomaximum.database.provider.DBProvider;
import com.infomaximum.database.provider.DBTransaction;
import com.infomaximum.database.schema.*;
import com.infomaximum.database.schema.dbstruct.*;
import com.infomaximum.database.utils.key.HashIndexKey;
import com.infomaximum.database.utils.key.IntervalIndexKey;
import com.infomaximum.database.utils.key.RangeIndexKey;

import java.util.*;
import java.util.stream.Collectors;

public class IndexService {

    @FunctionalInterface
    private interface ModifierCreator {
        void apply(final DomainObject obj, DBTransaction transaction) throws DatabaseException;
    }

    //todo check existed indexes and throw exception
    public static void doIndex(HashIndex index, StructEntity table, DBProvider dbProvider) throws DatabaseException {
        final Set<Integer> indexingFields = index.sortedFields.stream().map(Field::getNumber).collect(Collectors.toSet());
        final HashIndexKey indexKey = new HashIndexKey(0, index);

        indexData(indexingFields, table, dbProvider, (obj, transaction) -> {
            indexKey.setId(obj.getId());
            HashIndexUtils.setHashValues(index.sortedFields, obj, indexKey.getFieldValues());

            transaction.put(index.columnFamily, indexKey.pack(), TypeConvert.EMPTY_BYTE_ARRAY);
        });
    }

    public static void doIndex(DBHashIndex index, DBTable table, DBProvider dbProvider) throws DatabaseException {
        final DBField[] dbFields = IndexUtils.getFieldsByIds(table.getSortedFields(), index.getFieldIds());
        final StructEntity structEntity = getStructEntity(table);
        ArrayList<Field> structEntityFields = getStructEntityFields(dbFields, structEntity);
        final HashIndexKey indexKey = new HashIndexKey(0, index);
        indexData(getLoadedFields(structEntityFields), table, dbProvider, (obj, transaction) -> {
            indexKey.setId(obj.getId());
            HashIndexUtils.setHashValues(structEntityFields, obj, indexKey.getFieldValues());
            transaction.put(table.getIndexColumnFamily(), indexKey.pack(), TypeConvert.EMPTY_BYTE_ARRAY);
        });
    }

    public static void doPrefixIndex(PrefixIndex index, StructEntity table, DBProvider dbProvider) throws DatabaseException {
        final Set<Integer> indexingFields = index.sortedFields.stream().map(Field::getNumber).collect(Collectors.toSet());
        final SortedSet<String> lexemes = PrefixIndexUtils.buildSortedSet();

        indexData(indexingFields, table, dbProvider, (obj, transaction) -> {
            lexemes.clear();
            for (Field field : index.sortedFields) {
                PrefixIndexUtils.splitIndexingTextIntoLexemes(obj.get(field.getNumber()), lexemes);
            }
            PrefixIndexUtils.insertIndexedLexemes(index, obj.getId(), lexemes, transaction);
        });
    }

    public static void doPrefixIndex(DBPrefixIndex index, DBTable table, DBProvider dbProvider) throws DatabaseException {
        final DBField[] dbFields = IndexUtils.getFieldsByIds(table.getSortedFields(), index.getFieldIds());
        final StructEntity structEntity = getStructEntity(table);
        ArrayList<Field> structEntityFields = getStructEntityFields(dbFields, structEntity);
        final SortedSet<String> lexemes = PrefixIndexUtils.buildSortedSet();
        indexData(getLoadedFields(structEntityFields), table, dbProvider, (obj, transaction) -> {
            lexemes.clear();
            for (Field field : structEntityFields) {
                PrefixIndexUtils.splitIndexingTextIntoLexemes(obj.get(field.getNumber()), lexemes);
            }
            PrefixIndexUtils.insertIndexedLexemes(index, obj.getId(), lexemes, table.getIndexColumnFamily(), transaction);
        });
    }

    public static void doIntervalIndex(IntervalIndex index, StructEntity table, DBProvider dbProvider) throws DatabaseException {
        final Set<Integer> indexingFields = index.sortedFields.stream().map(Field::getNumber).collect(Collectors.toSet());
        final List<Field> hashedFields = index.getHashedFields();
        final Field indexedField = index.getIndexedField();
        final IntervalIndexKey indexKey = new IntervalIndexKey(0, new long[hashedFields.size()], index);

        indexData(indexingFields, table, dbProvider, (obj, transaction) -> {
            indexKey.setId(obj.getId());
            HashIndexUtils.setHashValues(hashedFields, obj, indexKey.getHashedValues());
            indexKey.setIndexedValue(obj.get(indexedField.getNumber()));

            transaction.put(index.columnFamily, indexKey.pack(), TypeConvert.EMPTY_BYTE_ARRAY);
        });
    }

    public static void doIntervalIndex(DBIntervalIndex index, DBTable table, DBProvider dbProvider) throws DatabaseException {
        final DBField[] dbFields = IndexUtils.getFieldsByIds(table.getSortedFields(), index.getFieldIds());
        final StructEntity structEntity = getStructEntity(table);
        final ArrayList<Field> structEntityFields = getStructEntityFields(dbFields, structEntity);
        final DBField[] dbHashedFields = IndexUtils.getFieldsByIds(table.getSortedFields(), index.getHashFieldIds());
        ArrayList<Field> hashIndexFields = getStructEntityFields(dbHashedFields, structEntity);
        final DBField indexedField = IndexUtils.getFieldsByIds(table.getSortedFields(), index.getIndexedFieldId());
        final IntervalIndexKey indexKey = new IntervalIndexKey(0, new long[dbHashedFields.length], index);

        indexData(getLoadedFields(structEntityFields), table, dbProvider, (obj, transaction) -> {
            indexKey.setId(obj.getId());
            HashIndexUtils.setHashValues(hashIndexFields, obj, indexKey.getHashedValues());
            indexKey.setIndexedValue(obj.get(indexedField.getId()));

            transaction.put(table.getIndexColumnFamily(), indexKey.pack(), TypeConvert.EMPTY_BYTE_ARRAY);
        });
    }

    public static void doRangeIndex(RangeIndex index, StructEntity table, DBProvider dbProvider) throws DatabaseException {
        final Set<Integer> indexingFields = index.sortedFields.stream().map(Field::getNumber).collect(Collectors.toSet());
        final List<Field> hashedFields = index.getHashedFields();
        final RangeIndexKey indexKey = new RangeIndexKey(0, new long[hashedFields.size()], index);

        indexData(indexingFields, table, dbProvider, (obj, transaction) -> {
            indexKey.setId(obj.getId());
            HashIndexUtils.setHashValues(hashedFields, obj, indexKey.getHashedValues());
            RangeIndexUtils.insertIndexedRange(index, indexKey,
                    obj.get(index.getBeginIndexedField().getNumber()),
                    obj.get(index.getEndIndexedField().getNumber()),
                    transaction);
        });
    }

    public static void doRangeIndex(DBRangeIndex index, DBTable table, DBProvider dbProvider) throws DatabaseException {
        final DBField[] allFields = IndexUtils.getFieldsByIds(table.getSortedFields(), index.getFieldIds());
        final StructEntity structEntity = getStructEntity(table);
        final ArrayList<Field> structEntityFields = getStructEntityFields(allFields, structEntity);
        final DBField[] dbHashedFields = IndexUtils.getFieldsByIds(table.getSortedFields(), index.getHashFieldIds());
        ArrayList<Field> hashIndexFields = getStructEntityFields(dbHashedFields, structEntity);

        final RangeIndexKey indexKey = new RangeIndexKey(0, new long[hashIndexFields.size()], index);
        final Field beginField = getStructEntityField(IndexUtils.getFieldsByIds(table.getSortedFields(), index.getBeginFieldId()), structEntity);
        final Field endField = getStructEntityField(IndexUtils.getFieldsByIds(table.getSortedFields(), index.getEndFieldId()), structEntity);

        indexData(getLoadedFields(structEntityFields), table, dbProvider, (obj, transaction) -> {
            indexKey.setId(obj.getId());
            HashIndexUtils.setHashValues(hashIndexFields, obj, indexKey.getHashedValues());
            RangeIndexUtils.insertIndexedRange(index,
                    indexKey,
                    obj.get(beginField.getNumber()),
                    obj.get(endField.getNumber()),
                    table.getIndexColumnFamily(),
                    transaction);
        });
    }


    private static void indexData(Set<Integer> loadingFields, StructEntity table, DBProvider dbProvider, ModifierCreator recordCreator) throws DatabaseException {
        DomainObjectSource domainObjectSource = new DomainObjectSource(dbProvider, true);
        try (DBTransaction transaction = dbProvider.beginTransaction();
             IteratorEntity<? extends DomainObject> iter = domainObjectSource.find(table.getObjectClass(), EmptyFilter.INSTANCE, loadingFields)) {
            while (iter.hasNext()) {
                recordCreator.apply(iter.next(), transaction);
            }
            transaction.commit();
        }
    }

    private static void indexData(Set<Integer> loadingFields, DBTable table, DBProvider dbProvider, ModifierCreator recordCreator) throws DatabaseException {
        DomainObjectSource domainObjectSource = new DomainObjectSource(dbProvider, true);
        try (DBTransaction transaction = dbProvider.beginTransaction();
             IteratorEntity<? extends DomainObject> iter = domainObjectSource.find(Schema.getTableClass(table.getName(), table.getNamespace()),
                     EmptyFilter.INSTANCE,
                     loadingFields)) {
            while (iter.hasNext()) {
                recordCreator.apply(iter.next(), transaction);
            }
            transaction.commit();
        }
    }

    private static StructEntity getStructEntity(DBTable table) {
        final Class<? extends DomainObject> tableClass = Schema.getTableClass(table.getName(), table.getNamespace());
        return new StructEntity(StructEntity.getAnnotationClass(tableClass));
    }

    private static ArrayList<Field> getStructEntityFields(DBField[] dbFields, StructEntity structEntity) {
        ArrayList<Field> fields = new ArrayList<>();
        for (DBField dbField : dbFields) {
            fields.add(getStructEntityField(dbField, structEntity));
        }
        return fields;
    }

    private static Field getStructEntityField(DBField dbFields, StructEntity structEntity) {
        final Field[] structEntityFields = structEntity.getFields();
        for (Field structEntityField : structEntityFields) {
            if (structEntityField.getName().equals(dbFields.getName())) {
                return structEntityField;
            }
        }
        throw new SchemaException("Required field:" + dbFields.getName() + "from schema doesn't found in StructEntity");
    }

    private static HashSet<Integer> getLoadedFields(ArrayList<Field> fields) {
        return fields.stream()
                .map(Field::getNumber)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
