package com.infomaximum.rocksdb.test.domain.index;

import com.infomaximum.rocksdb.RocksDataTest;
import com.infomaximum.rocksdb.builder.RocksdbBuilder;
import com.infomaximum.rocksdb.core.datasource.DataSourceImpl;
import com.infomaximum.rocksdb.core.objectsource.DomainObjectSource;
import com.infomaximum.rocksdb.domain.Department;
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
public class ComboIndexDependenceDomainObjectTest extends RocksDataTest {

    private final static Logger log = LoggerFactory.getLogger(ComboIndexDependenceDomainObjectTest.class);

    @Test
    public void run() throws Exception {
        RocksDataBase rocksDataBase = new RocksdbBuilder()
                .withPath(pathDataBase)
                .build();

        DomainObjectSource domainObjectSource = new DomainObjectSource(new DataSourceImpl(rocksDataBase));

        //Добавляем объекты
        domainObjectSource.getEngineTransaction().execute(new Monad() {
            @Override
            public void action(Transaction transaction) throws Exception {
                //Создали первый объект
                Department department1 = domainObjectSource.create(transaction, Department.class);
                department1.setName("department1");
                department1.save();

                Department department2 = domainObjectSource.create(transaction, Department.class);
                department2.setParent(department1);
                department2.setName("department2");
                department2.save();

                Department department3 = domainObjectSource.create(transaction, Department.class);
                department3.setParent(department1);
                department3.setName("department3");
                department3.save();
            }
        });

        //Редактируем 2-й объект
        domainObjectSource.getEngineTransaction().execute(new Monad() {
            @Override
            public void action(Transaction transaction) throws Exception {
                Department department2 = domainObjectSource.edit(transaction, Department.class, 2);
                department2.setParent(null);
                department2.save();
            }
        });


        //Ищем объекты по родителю
        Department department = domainObjectSource.find(Department.class, new HashMap<String, Object>(){{
            put("parent", domainObjectSource.get(Department.class, 1));
            put("name", "department3");
        }});
        Assert.assertNotNull(department);
        Assert.assertEquals("department3", department.getName());
        Assert.assertNotNull(department.getParent());
        Assert.assertEquals(1, department.getParent().getId());

        rocksDataBase.destroy();
    }

}