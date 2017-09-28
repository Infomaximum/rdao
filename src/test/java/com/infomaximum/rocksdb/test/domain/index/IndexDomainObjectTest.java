package com.infomaximum.rocksdb.test.domain.index;

import com.infomaximum.database.core.iterator.IteratorEntity;
import com.infomaximum.database.domainobject.DomainObjectSource;
import com.infomaximum.rocksdb.RocksDataTest;
import com.infomaximum.rocksdb.RocksDataBaseBuilder;
import com.infomaximum.rocksdb.core.datasource.RocksDBDataSourceImpl;
import com.infomaximum.rocksdb.domain.StoreFileEditable;
import com.infomaximum.rocksdb.domain.StoreFileReadable;
import com.infomaximum.rocksdb.RocksDataBase;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Created by kris on 22.04.17.
 */
public class IndexDomainObjectTest extends RocksDataTest {

    private final static Logger log = LoggerFactory.getLogger(IndexDomainObjectTest.class);

    @Test
    public void run() throws Exception {
        RocksDataBase rocksDataBase = new RocksDataBaseBuilder()
                .withPath(pathDataBase)
                .build();

        DomainObjectSource domainObjectSource = new DomainObjectSource(new RocksDBDataSourceImpl(rocksDataBase));
        domainObjectSource.createEntity(StoreFileReadable.class);

        //Проверяем, что таких объектов нет в базе
        for (long i=1; i<=100; i++) {
            HashMap<String, Object> filter = new HashMap<>();
            filter.put("size", i);
            Assert.assertNull(domainObjectSource.get(StoreFileReadable.class, null, i));
            Assert.assertFalse(domainObjectSource.find(StoreFileReadable.class, null, filter).hasNext());
        }

        //Добавляем объекты
        domainObjectSource.executeTransactional(transaction -> {
                for (int i=1; i<=100; i++) {
                    StoreFileEditable storeFile = transaction.create(StoreFileEditable.class);
                    storeFile.setSize(i);
                    transaction.save(storeFile);
                }
        });

        //Проверяем что файлы сохранены
        for (long id=1; id<=100; id++) {
            Assert.assertNotNull(domainObjectSource.get(StoreFileReadable.class, null, id));
        }

        //Ищем объекты по size
        for (long size=1; size<=100; size++) {
            HashMap<String, Object> filter = new HashMap<>();
            filter.put("size", size);
            try (IteratorEntity<StoreFileReadable> i = domainObjectSource.find(StoreFileReadable.class, null, filter)) {
                StoreFileReadable storeFile = i.next();
                Assert.assertNotNull(storeFile);
                Assert.assertEquals(size, storeFile.getSize());
                Assert.assertFalse(i.hasNext());
            }
        }

        rocksDataBase.close();
    }
}
