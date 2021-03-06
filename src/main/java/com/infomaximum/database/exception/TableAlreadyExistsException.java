package com.infomaximum.database.exception;

import com.infomaximum.database.schema.dbstruct.DBTable;

public class TableAlreadyExistsException extends SchemaException {

    public TableAlreadyExistsException(DBTable table) {
        super("Table already exists, table=" + table.getName());
    }
}
