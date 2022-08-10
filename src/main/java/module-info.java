module com.infomaximum.rdao {

    requires org.rocksdb.rocksdbjni;
    requires com.google.guava.guava;
    requires org.slf4j;
    requires net.minidev.jsonsmart;

    exports com.infomaximum.database.domainobject;
    exports com.infomaximum.database.exception;
    exports com.infomaximum.database.anotation;
    exports com.infomaximum.database.maintenance;
    exports com.infomaximum.database.provider;
    exports com.infomaximum.database.schema;
    exports com.infomaximum.rocksdb;
    exports com.infomaximum.database.utils;
    exports com.infomaximum.database.domainobject.filter;
    exports com.infomaximum.database.domainobject.iterator;
    exports com.infomaximum.database.schema.dbstruct;
    exports com.infomaximum.database.schema.table;
    exports com.infomaximum.database;
}