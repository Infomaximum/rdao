package com.infomaximum.database.schema;

import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.exception.*;
import com.infomaximum.database.exception.runtime.IllegalTypeException;
import com.infomaximum.database.provider.*;
import com.infomaximum.database.schema.dbstruct.*;
import com.infomaximum.database.schema.table.*;
import com.infomaximum.database.utils.IndexService;
import com.infomaximum.database.utils.TypeConvert;
import com.infomaximum.database.utils.key.FieldKey;
import jdk.nashorn.internal.runtime.regexp.joni.exception.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/**
 * Все методы по изменению схемы не транзакционны.
 * Если возникнет ошибка при изменении схемы, то объектное описание схемы в памяти,
 * ее описание на диске и структура данных будут несогласованны
 */
public class Schema {

    private final static Logger log = LoggerFactory.getLogger(Schema.class);

    public static Set<Class<? extends Serializable>> SUPPORTED_FIELD_TYPES = new HashSet<>(Arrays.asList(
            String.class, byte[].class,
            Long.class, Integer.class, Boolean.class, Double.class,
            Instant.class, LocalDateTime.class
    ));

    static final String CURRENT_VERSION = "1.0.0";

    public static final String SERVICE_COLUMN_FAMILY = "service";
    static final byte[] VERSION_KEY = TypeConvert.pack("version");
    static final byte[] SCHEMA_KEY = TypeConvert.pack("schema");

    private final DBProvider dbProvider;
    private final DBSchema dbSchema;
    private final static ConcurrentMap<Class<? extends DomainObject>, StructEntity> objTables = new ConcurrentHashMap<>();

    private Schema(DBProvider dbProvider, DBSchema schema) {
        this.dbProvider = dbProvider;
        this.dbSchema = schema;
    }

    public static Schema create(DBProvider dbProvider) throws DatabaseException {
        return new Schema(dbProvider, createSchema(dbProvider));
    }

    public static Schema read(DBProvider dbProvider) throws DatabaseException {
        return new Schema(dbProvider, readSchema(dbProvider));
    }

    private static DBSchema createSchema(DBProvider dbProvider) throws DatabaseException {
        dbProvider.createColumnFamily(SERVICE_COLUMN_FAMILY);

        String version = TypeConvert.unpackString(dbProvider.getValue(SERVICE_COLUMN_FAMILY, VERSION_KEY));
        String schemaJson = TypeConvert.unpackString(dbProvider.getValue(SERVICE_COLUMN_FAMILY, SCHEMA_KEY));
        if (version != null || schemaJson != null) {
            throw new SchemaException("Schema already exists");
        }

        DBSchema newSchema = DBSchema.fromStrings(CURRENT_VERSION, "[]");
        saveSchema(newSchema, dbProvider);
        return newSchema;
    }

    public static boolean exists(DBProvider dbProvider) throws DatabaseException {
        return dbProvider.containsColumnFamily(SERVICE_COLUMN_FAMILY);
    }

    private static DBSchema readSchema(DBProvider dbProvider) throws DatabaseException {
        String version = TypeConvert.unpackString(dbProvider.getValue(SERVICE_COLUMN_FAMILY, VERSION_KEY));
        String schemaJson = TypeConvert.unpackString(dbProvider.getValue(SERVICE_COLUMN_FAMILY, SCHEMA_KEY));
        validateSchema(version, schemaJson);
        return DBSchema.fromStrings(version, schemaJson);
    }

    private static void validateSchema(String version, String schemaJson) throws DatabaseException {
        if (version == null) {
            if (schemaJson == null) {
                throw new SchemaException("Schema not found");
            }
            throw new CorruptedException("Key 'version' not found");
        } else if (schemaJson == null) {
            throw new CorruptedException("Key 'schema' not found");
        }

        if (!CURRENT_VERSION.equals(version)) {
            throw new SchemaException("Incorrect version of the database (" + version + "). Current version is " + CURRENT_VERSION + ".");
        }
    }

    private static void saveSchema(DBSchema schema, DBProvider dbProvider) throws DatabaseException {
        try (DBTransaction transaction = dbProvider.beginTransaction()) {
            transaction.put(SERVICE_COLUMN_FAMILY, VERSION_KEY, TypeConvert.pack(schema.getVersion()));
            transaction.put(SERVICE_COLUMN_FAMILY, SCHEMA_KEY, TypeConvert.pack(schema.toTablesJsonString()));
            transaction.commit();
        }
    }

    public DBProvider getDbProvider() {
        return dbProvider;
    }

    public DBSchema getDbSchema() {
        return dbSchema;
    }

    private static <T extends DomainObject> Constructor<T> getConstructor(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(long.class, StructEntity.class);
            constructor.setAccessible(true);
            return constructor;
        } catch (ReflectiveOperationException e) {
            throw new IllegalTypeException(e);
        }
    }

    public boolean existTable(StructEntity table) {
        return dbSchema.findTableIndex(table.getName(), table.getNamespace()) != -1;
    }

    public void createTable(Table table) throws DatabaseException {
        Schema.resolve(table.getObjectClass());
        int tableIndex = dbSchema.findTableIndex(table.getName(), table.getNamespace());
        DBTable dbTable;
        if (tableIndex == -1) {
            dbTable = dbSchema.newTable(table.getName(), table.getNamespace(), new ArrayList<>());

            dbProvider.createColumnFamily(dbTable.getDataColumnFamily());
            dbProvider.createColumnFamily(dbTable.getIndexColumnFamily());
            dbProvider.createSequence(dbTable.getDataColumnFamily());
        } else {
            throw new TableAlreadyExistsException(dbSchema.getTables().get(tableIndex));
        }

        for (TField tableField : table.getFields()) {
            createField(tableField, dbTable);
        }

        for (THashIndex index : table.getHashIndexes()) {
            createIndex(index, dbTable);
        }

        for (TPrefixIndex index : table.getPrefixIndexes()) {
            createIndex(index, dbTable);
        }

        for (TIntervalIndex index : table.getIntervalIndexes()) {
            createIndex(index, dbTable);
        }

        for (TRangeIndex index : table.getRangeIndexes()) {
            createIndex(index, dbTable);
        }
        saveSchema();
    }

    public boolean dropTable(String name, String namespace) throws DatabaseException {
        int i = dbSchema.findTableIndex(name, namespace);
        if (i == -1) {
            return false;
        }

        DBTable table = dbSchema.getTables().remove(i);
        dbProvider.dropColumnFamily(table.getDataColumnFamily());
        dbProvider.dropColumnFamily(table.getIndexColumnFamily());
        dbProvider.dropSequence(table.getName());

        removeObjTable(name);

        saveSchema();
        return true;
    }

    public static StructEntity getEntity(Class<? extends DomainObject> clazz) {
        StructEntity entity = objTables.get(clazz);
        if (entity == null) {
            entity = objTables.get(StructEntity.getAnnotationClass(clazz));
            if (entity == null) {
                throw new InternalException("can't find entity: " + clazz);
            }
            objTables.putIfAbsent(clazz, entity);
        }
        return entity;
    }

    public static <T extends DomainObject> StructEntity resolve(Class<T> objClass) throws SchemaException {
        Class<? extends DomainObject> annotationClass = StructEntity.getAnnotationClass(objClass);
        StructEntity entity = objTables.get(annotationClass);
        if (entity == null) {
            entity = new StructEntity(annotationClass);
            objTables.put(annotationClass, entity);
        }
        return entity;
    }

    private static <T extends DomainObject> StructEntity buildObjTable(Class<T> objClass) throws SchemaException {
        return new StructEntity(objClass);
    }

    public Collection<StructEntity> getDomains() {
        return objTables.values();
    }

    public void checkIntegrity() throws DatabaseException {
        dbSchema.checkIntegrity();

        for (DBTable table : dbSchema.getTables()) {
            if (!dbProvider.containsColumnFamily(table.getDataColumnFamily())) {
                throw new SchemaException("ColumnFamily '" + table.getDataColumnFamily() + "' not found, table='" + table.getName() + "'");
            }

            if (!dbProvider.containsColumnFamily(table.getIndexColumnFamily())) {
                throw new SchemaException("ColumnFamily '" + table.getIndexColumnFamily() + "' not found, table='" + table.getName() + "'");
            }
            if (!dbProvider.containsSequence(table.getDataColumnFamily())) {
                throw new SequenceNotFoundException(table.getDataColumnFamily());
            }
        }

    }

    //todo check it
    public void renameTable(String oldName, String newName, String namespace) throws DatabaseException {
        dbSchema.getTable(oldName, namespace).setName(newName);
        saveSchema();
    }


    private DBField createField(TField tableField, DBTable dbTable) throws DatabaseException {
        int i = dbTable.findFieldIndex(tableField.getName());
        if (i != -1) {
            throw new FieldAlreadyExistsException(tableField.getName(), dbTable.getName());
        }

        Integer fTableId = tableField.getForeignTable() != null
                ? dbSchema.getTable(tableField.getForeignTable().getName(), tableField.getForeignTable().getNamespace()).getId()
                : null;
        DBField newField = dbTable.newField(tableField.getName(), tableField.getType(), fTableId);
        if (newField.isForeignKey()) {
            createIndex(new THashIndex(tableField.getName()), dbTable);
        }
        saveSchema();
        return newField;
    }

    public boolean dropField(String fieldName, String tableName, String namespace) throws DatabaseException {
        DBTable table = dbSchema.getTable(tableName, namespace);
        int i = table.findFieldIndex(fieldName);
        if (i == -1) {
            return false;
        }

        DBField field = table.getFields().get(i);
        dropIndexesByField(field, table.getHashIndexes(), table);
        dropIndexesByField(field, table.getPrefixIndexes(), table);
        dropIndexesByField(field, table.getIntervalIndexes(), table);
        dropIndexesByField(field, table.getRangeIndexes(), table);

        dropFieldData(field, table);

        table.getFields().remove(i);
        removeObjTable(tableName);

        saveSchema();
        return true;
    }

    private <T extends DBIndex> void dropIndexesByField(DBField field, List<T> indexes, DBTable table) throws DatabaseException {
        for (int i = indexes.size() - 1; i > -1; --i) {
            T index = indexes.get(i);
            if (index.fieldContains(field.getId())) {
                dropIndexData(index, table);
                indexes.remove(i);
            }
        }
    }

    public void renameField(String oldName, String newName, String tableName, String namespace) throws DatabaseException {
        dbSchema.getTable(tableName, namespace).getField(oldName).setName(newName);
        saveSchema();
    }

    private void createIndex(THashIndex index, DBTable dbTable) throws DatabaseException {
        DBHashIndex dbIndex = DBTableUtils.buildIndex(index, dbTable);
        if (dbTable.getHashIndexes().stream().noneMatch(dbIndex::fieldsEquals)) {
            dbTable.attachIndex(dbIndex);
            IndexService.doIndex(dbIndex, dbTable, dbProvider);
            saveSchema();
        } else if (index.getFields().length != 1 || !dbTable.getField(index.getFields()[0]).isForeignKey()) {
            throw new IndexAlreadyExistsException(index);
        }
    }

    private void createIndex(TPrefixIndex index, DBTable dbTable) throws DatabaseException {
        DBPrefixIndex dbIndex = DBTableUtils.buildIndex(index, dbTable);
        if (dbTable.getPrefixIndexes().stream().noneMatch(dbIndex::fieldsEquals)) {
            dbTable.attachIndex(dbIndex);
            IndexService.doPrefixIndex(dbIndex, dbTable, dbProvider);
            saveSchema();
        } else {
            throw new IndexAlreadyExistsException(index);
        }
    }

    private void createIndex(TIntervalIndex index, DBTable dbTable) throws DatabaseException {
        DBIntervalIndex dbIndex = DBTableUtils.buildIndex(index, dbTable);
        if (dbTable.getIntervalIndexes().stream().noneMatch(dbIndex::fieldsEquals)) {
            dbTable.attachIndex(dbIndex);
            IndexService.doIntervalIndex(dbIndex, dbTable, dbProvider);
            saveSchema();
        } else {
            throw new IndexAlreadyExistsException(index);
        }
    }

    private void createIndex(TRangeIndex index, DBTable dbTable) throws DatabaseException {
        DBRangeIndex dbIndex = DBTableUtils.buildIndex(index, dbTable);
        if (dbTable.getRangeIndexes().stream().noneMatch(dbIndex::fieldsEquals)) {
            dbTable.attachIndex(dbIndex);
            IndexService.doIntervalIndex(dbIndex, dbTable, dbProvider);
            saveSchema();
        } else {
            throw new IndexAlreadyExistsException(index);
        }
    }

    public boolean dropIndex(THashIndex index, String tableName, String namespace) throws DatabaseException {
        DBTable table = dbSchema.getTable(tableName, namespace);
        if (index.getFields().length == 1 && table.getField(index.getFields()[0]).isForeignKey()) {
            return true;
        }

        DBHashIndex targetIndex = DBTableUtils.buildIndex(index, table);
        return dropIndex(table.getHashIndexes(), targetIndex::fieldsEquals, table);
    }

    private <T extends DBIndex> boolean dropIndex(List<T> indexes, Predicate<T> predicate, DBTable table) throws DatabaseException {
        for (int i = 0; i < indexes.size(); ++i) {
            T dbIndex = indexes.get(i);
            if (predicate.test(dbIndex)) {
                dropIndexData(dbIndex, table);
                indexes.remove(i);

                removeObjTable(table.getName());

                saveSchema();
                return true;
            }
        }
        return false;
    }

    public boolean dropIndex(TPrefixIndex index, String tableName, String namespace) throws DatabaseException {
        DBTable table = dbSchema.getTable(tableName, namespace);
        DBPrefixIndex targetIndex = DBTableUtils.buildIndex(index, table);
        return dropIndex(table.getPrefixIndexes(), targetIndex::fieldsEquals, table);
    }

    public boolean dropIndex(TIntervalIndex index, String tableName, String namespace) throws DatabaseException {
        DBTable table = dbSchema.getTable(tableName, namespace);
        DBIntervalIndex targetIndex = DBTableUtils.buildIndex(index, table);
        return dropIndex(table.getIntervalIndexes(), targetIndex::fieldsEquals, table);
    }

    public boolean dropIndex(TRangeIndex index, String tableName, String namespace) throws DatabaseException {
        DBTable table = dbSchema.getTable(tableName, namespace);
        DBRangeIndex targetIndex = DBTableUtils.buildIndex(index, table);
        return dropIndex(table.getRangeIndexes(), targetIndex::fieldsEquals, table);
    }

    private void saveSchema() throws DatabaseException {
        saveSchema(dbSchema, dbProvider);
    }

    private void dropFieldData(DBField field, DBTable table) throws DatabaseException {
        try (DBTransaction transaction = dbProvider.beginTransaction()) {
            try (DBIterator i = transaction.createIterator(table.getDataColumnFamily())) {
                KeyPattern pattern = new KeyPattern(new KeyPattern.Postfix[] {
                        new KeyPattern.Postfix(FieldKey.ID_BYTE_SIZE, TypeConvert.pack(field.getId()))
                });

                for (KeyValue keyValue = i.seek(pattern); keyValue != null; keyValue = i.next()) {
                    transaction.singleDelete(table.getDataColumnFamily(), keyValue.getKey());
                }
            }

            transaction.commit();
        }
    }

    private void dropIndexData(DBIndex index, DBTable table) throws DatabaseException {
        try (DBTransaction transaction = dbProvider.beginTransaction()) {
            byte[] beginKey = TypeConvert.pack(index.getId());
            byte[] endKey = TypeConvert.pack(index.getId() + 1);

            transaction.singleDeleteRange(table.getIndexColumnFamily(), beginKey, endKey);
            transaction.commit();
        }
    }

    private void removeObjTable(String tableName) {
        Iterator<Map.Entry<Class<? extends DomainObject>, StructEntity>> i = objTables.entrySet().iterator();
        while (i.hasNext()) {
            StructEntity table = i.next().getValue();
            if (table.getName().equals(tableName)) {
                i.remove();
            }
        }
    }
}
