package com.infomaximum.database;

import com.infomaximum.database.domainobject.filter.*;
import com.infomaximum.database.engine.*;
import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.exception.SchemaException;
import com.infomaximum.database.provider.DBDataReader;
import com.infomaximum.database.schema.dbstruct.DBField;
import com.infomaximum.database.schema.dbstruct.DBSchema;
import com.infomaximum.database.schema.dbstruct.DBTable;

import java.util.Set;

public class DataReadCommand {

    private final DBDataReader dataReader;
    protected final DBSchema schema;

    DataReadCommand(DBDataReader dataReader, DBSchema schema) {
        this.dataReader = dataReader;
        this.schema = schema;
    }

    public DBDataReader getDBCommand() {
        return dataReader;
    }

    public RecordIterator select(String table, String namespace) throws DatabaseException {
        DBTable dbTable = schema.getTable(table, namespace);
        return new AllIterator(dbTable, dataReader);
    }

    public RecordIterator select(String table, String namespace, HashFilter filter) throws DatabaseException {
        DBTable dbTable = schema.getTable(table, namespace);
        return new HashIterator(dbTable, filter, dataReader);
    }

    public RecordIterator select(String table, String namespace, PrefixFilter filter) throws DatabaseException {
        DBTable dbTable = schema.getTable(table, namespace);
        return new PrefixIterator(dbTable, filter, dataReader);
    }

    public RecordIterator select(String table, String namespace, IntervalFilter filter) throws DatabaseException {
        DBTable dbTable = schema.getTable(table, namespace);
        return new IntervalIterator(dbTable, filter, dataReader);
    }

    public RecordIterator select(String table, String namespace, RangeFilter filter) throws DatabaseException {
        DBTable dbTable = schema.getTable(table, namespace);
        return new RangeIterator(dbTable, filter, dataReader);
    }

    public RecordIterator select(String table, String namespace, IdFilter filter) throws DatabaseException {
        DBTable dbTable = schema.getTable(table, namespace);
        return new IdIterator(dbTable, filter, dataReader);
    }

    public Record getById(String table, String namespace, long id) throws DatabaseException {
        DBTable dbTable = schema.getTable(table, namespace);
        try (IdIterator idIterator = new IdIterator(dbTable, new IdFilter(id, id), dataReader)){
            return idIterator.hasNext() ? idIterator.next() : null;
        }
    }

    private static DBField[] toFieldArray(Set<String> fieldNames, DBTable table) throws SchemaException {
        DBField[] fields = new DBField[fieldNames.size()];
        int i = 0;
        for (String name : fieldNames) {
            fields[i++] = table.getField(name);
        }
        return fields;
    }
}
