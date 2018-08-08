package com.infomaximum.rocksdb;

import com.infomaximum.database.anotation.Entity;
import com.infomaximum.database.anotation.Field;
import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.exception.DatabaseException;

@Entity(
        namespace = "com.infomaximum.store",
        name = "rocksdb.record",
        fields = {
                @Field(number = RecordReadable.FIELD_STRING_1, name = "str1", type = String.class),
                @Field(number = RecordReadable.FIELD_LONG_1, name = "lng1", type = Long.class),
                @Field(number = RecordReadable.FIELD_INT_1, name = "int1", type = Integer.class),
                @Field(number = RecordReadable.FIELD_BOOLEAN_1, name = "bool1", type = Boolean.class)
        }
)
public class RecordReadable  extends DomainObject {

    public final static int FIELD_STRING_1 = 0;
    public final static int FIELD_LONG_1 = 1;
    public final static int FIELD_INT_1 = 2;
    public final static int FIELD_BOOLEAN_1 = 3;

    public RecordReadable(long id) {
        super(id);
    }

    public String getString1() throws DatabaseException {
        return getString(FIELD_STRING_1);
    }

    public Long getLong1() throws DatabaseException {
        return getLong(FIELD_LONG_1);
    }

    public Boolean getBoolean1() throws DatabaseException {
        return getBoolean(FIELD_BOOLEAN_1);
    }

    public Integer getInt1() throws DatabaseException {
        return getInteger(FIELD_INT_1);
    }
}

