package com.infomaximum.rocksdb.test.domain.index;

import com.infomaximum.rocksdb.RocksDataTest;
import com.infomaximum.rocksdb.builder.RocksdbBuilder;
import com.infomaximum.rocksdb.core.datasource.DataSourceImpl;
import com.infomaximum.rocksdb.core.objectsource.DomainObjectSource;
import com.infomaximum.rocksdb.domain.ExchangeFolder;
import com.infomaximum.rocksdb.struct.RocksDataBase;
import com.infomaximum.rocksdb.transaction.Transaction;
import com.infomaximum.rocksdb.transaction.engine.Monad;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Created by kris on 22.04.17.
 */
public class ComboIndexDomainObjectTest extends RocksDataTest {

    private final static Logger log = LoggerFactory.getLogger(ComboIndexDomainObjectTest.class);

    @Test
    public void run() throws Exception {
        RocksDataBase rocksDataBase = new RocksdbBuilder()
                .withPath(pathDataBase)
                .build();

        DomainObjectSource domainObjectSource = new DomainObjectSource(new DataSourceImpl(rocksDataBase));

        String uuid = "AQMkAGYzOGZhMGRlLTk0ZmQtNGU4Mi05YzMyLWU1YmMyODgAMzA1MzkALgAAA2q7G9o/e25DjV2GPrKtaxsBAOVhxnfq2u5Gj3QIHLYcQRoAAAIBDQAAAA==";
        String userEmail = "test1@infomaximum.onmicrosoft.com";

        //Добавляем объект
        domainObjectSource.getEngineTransaction().execute(new Monad() {
            @Override
            public void action(Transaction transaction) throws Exception {
                ExchangeFolder exchangeFolder = domainObjectSource.create(transaction, ExchangeFolder.class);
                exchangeFolder.setUuid(uuid);
                exchangeFolder.setUserEmail(userEmail);
                exchangeFolder.save();
            }
        });


        //Ищем объект
        ExchangeFolder exchangeFolder = domainObjectSource.find(ExchangeFolder.class, new HashMap<String, Object>(){{
            put("uuid", uuid);
            put("userEmail", userEmail);
        }});
        Assert.assertNotNull(exchangeFolder);
        Assert.assertEquals(uuid, exchangeFolder.getUuid());
        Assert.assertEquals(userEmail, exchangeFolder.getUserEmail());

        rocksDataBase.destroy();
    }

}