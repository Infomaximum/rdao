package com.infomaximum.database.engine;

import com.infomaximum.database.Record;
import com.infomaximum.database.RecordIterator;
import com.infomaximum.database.domainobject.filter.RangeFilter;
import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.provider.DBDataReader;
import com.infomaximum.database.schema.dbstruct.DBField;
import com.infomaximum.database.schema.dbstruct.DBTable;

public class RangeIterator implements RecordIterator {

    public RangeIterator(DBTable table, DBField[] selectingFields, RangeFilter filter, DBDataReader dataReader) {
        // TODO realize
    }

//    @Override
//    public void reuseReturningRecord(boolean value) {
//        // TODO realize
//    }

    @Override
    public boolean hasNext() throws DatabaseException {
        // TODO realize
        return false;
    }

    @Override
    public Record next() throws DatabaseException {
        // TODO realize
        return null;
    }

    @Override
    public void close() throws DatabaseException {
        // TODO realize
    }
}