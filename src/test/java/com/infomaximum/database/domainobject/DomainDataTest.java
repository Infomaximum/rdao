package com.infomaximum.database.domainobject;

import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.exception.runtime.FieldValueNotFoundException;
import com.infomaximum.database.maintenance.ChangeMode;
import com.infomaximum.database.maintenance.DomainService;
import com.infomaximum.database.schema.EntityField;
import com.infomaximum.database.schema.Schema;
import com.infomaximum.rocksdb.RocksDBProvider;
import com.infomaximum.rocksdb.RocksDataBaseBuilder;
import com.infomaximum.rocksdb.RocksDataTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.util.Set;

public abstract class DomainDataTest extends RocksDataTest {

    protected RocksDBProvider rocksDBProvider;

    protected DomainObjectSource domainObjectSource;

    @Before
    public void init() throws Exception {
        super.init();

        rocksDBProvider = new RocksDataBaseBuilder().withPath(pathDataBase).build();
        domainObjectSource = new DomainObjectSource(rocksDBProvider);
    }

    @After
    public void destroy() throws Exception {
        rocksDBProvider.close();

        super.destroy();
    }

    protected void createDomain(Class<? extends DomainObject> clazz) throws DatabaseException {
        new Schema.Builder().withDomain(clazz).build();
        new DomainService(rocksDBProvider)
                .setChangeMode(ChangeMode.CREATION)
                .setValidationMode(true)
                .setDomain(Schema.getEntity(clazz))
                .execute();
    }

    protected static void checkLoadedState(DomainObject target, Set<String> loadingFields) throws DatabaseException {
        for (String field : loadingFields) {
            target.get(field);
        }

        for (EntityField field : target.getStructEntity().getFields()) {
            if (loadingFields.contains(field.getName())) {
                continue;
            }
            try {
                target.get(field.getName());
                Assert.fail();
            } catch (FieldValueNotFoundException e) {
                Assert.assertTrue(true);
            }
        }
    }
}
