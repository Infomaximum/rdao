package com.infomaximum.rocksdb.maintenance;

import com.infomaximum.database.core.iterator.IteratorEntity;
import com.infomaximum.database.core.schema.Schema;
import com.infomaximum.database.core.schema.StructEntity;
import com.infomaximum.database.domainobject.filter.IndexFilter;
import com.infomaximum.database.domainobject.filter.PrefixIndexFilter;
import com.infomaximum.database.exeption.DatabaseException;
import com.infomaximum.database.exeption.InconsistentDatabaseException;
import com.infomaximum.database.exeption.runtime.ColumnFamilyNotFoundException;
import com.infomaximum.database.maintenance.DomainService;
import com.infomaximum.rocksdb.domain.StoreFileEditable;
import com.infomaximum.rocksdb.domain.StoreFileReadable;
import com.infomaximum.rocksdb.domain.type.FormatType;
import com.infomaximum.rocksdb.test.DomainDataTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DomainServiceTest extends DomainDataTest {

    @Before
    public void init() throws Exception {
        super.init();

        new Schema.Builder().withDomain(StoreFileReadable.class).build();
    }

    @Test
    public void createAll() throws Exception {
        testNotWorking();

        new DomainService(dataSource)
                .setCreationMode(true)
                .setDomain(Schema.getEntity(StoreFileReadable.class))
                .execute();

        testWorking();
    }

    @Test
    public void createPartial() throws Exception {
        StructEntity entity = Schema.getEntity(StoreFileReadable.class);

        new DomainService(dataSource).setCreationMode(true).setDomain(entity).execute();
        rocksDataBase.dropColumnFamily(entity.getColumnFamily());
        testNotWorking();

        new DomainService(dataSource).setCreationMode(true).setDomain(entity).execute();
        testWorking();
    }

    @Test
    public void createIndexAndIndexingData() throws Exception {
        StructEntity entity = Schema.getEntity(StoreFileReadable.class);
        new DomainService(dataSource).setCreationMode(true).setDomain(entity).execute();

        domainObjectSource.executeTransactional(transaction -> {
            for (long i = 0; i < 100; ++i) {
                StoreFileEditable obj = transaction.create(StoreFileEditable.class);
                obj.setFileName("Test");
                obj.setSize(i);
                transaction.save(obj);
            }
        });

        rocksDataBase.dropColumnFamily("com.infomaximum.store.StoreFile.prefixtextindex.file_name");
        rocksDataBase.dropColumnFamily("com.infomaximum.store.StoreFile.index.size:java.lang.Long");

        new DomainService(dataSource).setCreationMode(true).setDomain(entity).execute();

        try (IteratorEntity iter = domainObjectSource.find(StoreFileReadable.class, new IndexFilter(StoreFileReadable.FIELD_SIZE, 10L))) {
            Assert.assertNotNull(iter.next());
        }

        try (IteratorEntity iter = domainObjectSource.find(StoreFileReadable.class, new PrefixIndexFilter(StoreFileReadable.FIELD_FILE_NAME, "tes"))) {
            Assert.assertNotNull(iter.next());
        }
    }

    @Test
    public void validateUnknownColumnFamily() throws Exception {
        createDomain(StoreFileReadable.class);

        rocksDataBase.createColumnFamily("com.infomaximum.store.StoreFile.some_prefix");

        try {
            new DomainService(dataSource).setCreationMode(false).setDomain(Schema.getEntity(StoreFileReadable.class)).execute();
            Assert.fail();
        } catch (InconsistentDatabaseException e) {
            Assert.assertTrue(true);
        }
    }

    private void testNotWorking() throws Exception {
        try {
            testWorking();
            Assert.fail();
        } catch (DatabaseException | ColumnFamilyNotFoundException ignoring) {
            Assert.assertTrue(true);
        }
    }

    private void testWorking() throws Exception {
        domainObjectSource.executeTransactional(transaction -> {
            StoreFileEditable obj = transaction.create(StoreFileEditable.class);
            obj.setFileName("Test");
            obj.setSize(100);
            obj.setFormat(FormatType.B);
            obj.setSingle(false);
            obj.setContentType("content");
            transaction.save(obj);
        });
    }
}
