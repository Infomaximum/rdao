package com.infomaximum.database.schema.dbstruct;

import com.infomaximum.database.exception.runtime.SchemaException;
import net.minidev.json.JSONObject;

import java.io.Serializable;

public class DBField extends DBObject {

    private static final String JSON_PROP_NAME = "name";
    private static final String JSON_PROP_TYPE = "type";
    private static final String JSON_PROP_FOREIGN_TABLE_ID = "foreign_table_id";

    private String name;
    private final Class<? extends Serializable> type;
    private final Integer foreignTableId;

    DBField(int id, String name, Class<? extends Serializable> type, Integer foreignTableId) {
        super(id);
        this.name = name;
        this.type = type;
        this.foreignTableId = foreignTableId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<? extends Serializable> getType() {
        return type;
    }

    public Integer getForeignTableId() {
        return foreignTableId;
    }

    public boolean isForeignKey() {
        return foreignTableId != null;
    }

    static DBField fromJson(JSONObject source) throws SchemaException {
        return new DBField(
                JsonUtils.getValue(JSON_PROP_ID, Integer.class, source),
                JsonUtils.getValue(JSON_PROP_NAME, String.class, source),
                resolve(JsonUtils.getValue(JSON_PROP_TYPE, String.class, source)),
                JsonUtils.getValueOrDefault(JSON_PROP_FOREIGN_TABLE_ID, Integer.class, source, null)
        );
    }

    @Override
    JSONObject toJson() {
        JSONObject object = new JSONObject();
        object.put(JSON_PROP_ID, getId());
        object.put(JSON_PROP_NAME, name);
        object.put(JSON_PROP_TYPE, type.getName());
        object.put(JSON_PROP_FOREIGN_TABLE_ID, foreignTableId);
        return object;
    }

    private static Class<? extends Serializable> resolve(String type) throws SchemaException {
        try {
            return (Class<? extends Serializable>) Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new SchemaException(e);
        }
    }
}
