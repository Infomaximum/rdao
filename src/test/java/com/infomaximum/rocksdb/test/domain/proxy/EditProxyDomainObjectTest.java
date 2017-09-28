package com.infomaximum.rocksdb.test.domain.proxy;

import com.infomaximum.database.core.transaction.Transaction;
import com.infomaximum.database.core.transaction.engine.Monad;
import com.infomaximum.database.domainobject.DomainObjectSource;
import com.infomaximum.rocksdb.RocksDataBase;
import com.infomaximum.rocksdb.RocksDataBaseBuilder;
import com.infomaximum.rocksdb.RocksDataTest;
import com.infomaximum.rocksdb.core.datasource.RocksDBDataSourceImpl;
import com.infomaximum.rocksdb.domain.proxy.ProxyStoreFileEditable;
import com.infomaximum.rocksdb.domain.proxy.ProxyStoreFileReadable;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kris on 22.04.17.
 */
public class EditProxyDomainObjectTest extends RocksDataTest {

    private final static Logger log = LoggerFactory.getLogger(EditProxyDomainObjectTest.class);

    @Test
    public void run() throws Exception {
        RocksDataBase rocksDataBase = new RocksDataBaseBuilder()
                .withPath(pathDataBase)
                .build();

        DomainObjectSource domainObjectSource = new DomainObjectSource(new RocksDBDataSourceImpl(rocksDataBase));
        domainObjectSource.createEntity(ProxyStoreFileReadable.class);

        //Проверяем, что такого объекта нет в базе
        Assert.assertNull(domainObjectSource.get(ProxyStoreFileReadable.class, 1L));

        String fileName1 = "info1.json";
        String fileName2 = "info2.json";
        String contentType = "application/json";
        long size = 1000L;

        //Добавляем объект
        Transaction transaction1 = domainObjectSource.getEngineTransaction().createTransaction();
        ProxyStoreFileEditable storeFile = domainObjectSource.create(ProxyStoreFileEditable.class);
        storeFile.setFileName(fileName1);
        storeFile.setContentType(contentType);
        storeFile.setSize(size);
        storeFile.setSingle(false);
        domainObjectSource.save(storeFile, transaction1);
        transaction1.commit();


        //Загружаем сохраненый объект
        ProxyStoreFileReadable storeFileCheckSave = domainObjectSource.get(ProxyStoreFileReadable.class, 1L);
        Assert.assertNotNull(storeFileCheckSave);
        Assert.assertEquals(fileName1, storeFileCheckSave.getFileName());
        Assert.assertEquals(contentType, storeFileCheckSave.getContentType());
        Assert.assertEquals(size, storeFileCheckSave.getSize());
        Assert.assertEquals(false, storeFileCheckSave.isSingle());

        //Редактируем сохраненый объект
        domainObjectSource.getEngineTransaction().execute(new Monad() {
            @Override
            public void action(Transaction transaction) throws Exception {
                ProxyStoreFileEditable storeFile = domainObjectSource.get(ProxyStoreFileEditable.class, 1L);
                storeFile.setFileName(fileName2);
                storeFile.setSingle(true);
                domainObjectSource.save(storeFile, transaction);
            }
        });

        //Загружаем отредактированный объект
        ProxyStoreFileReadable editFileCheckSave = domainObjectSource.get(ProxyStoreFileReadable.class, 1L);
        Assert.assertNotNull(editFileCheckSave);
        Assert.assertEquals(fileName2, editFileCheckSave.getFileName());
        Assert.assertEquals(contentType, editFileCheckSave.getContentType());
        Assert.assertEquals(size, editFileCheckSave.getSize());
        Assert.assertEquals(true, editFileCheckSave.isSingle());


        //Повторно редактируем сохраненый объект
        domainObjectSource.getEngineTransaction().execute(new Monad() {
            @Override
            public void action(Transaction transaction) throws Exception {
                ProxyStoreFileEditable storeFile = domainObjectSource.get(ProxyStoreFileEditable.class, 1L);
                storeFile.setFileName(fileName1);
                storeFile.setSingle(false);
                domainObjectSource.save(storeFile, transaction);
            }
        });


        ProxyStoreFileReadable storeFileCheckSave2 = domainObjectSource.get(ProxyStoreFileReadable.class, 1L);
        Assert.assertNotNull(storeFileCheckSave2);
        Assert.assertEquals(fileName1, storeFileCheckSave2.getFileName());
        Assert.assertEquals(contentType, storeFileCheckSave2.getContentType());
        Assert.assertEquals(size, storeFileCheckSave2.getSize());
        Assert.assertEquals(false, storeFileCheckSave2.isSingle());

        rocksDataBase.close();
    }

}