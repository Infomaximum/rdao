package com.infomaximum.database.domainobject;

import com.infomaximum.database.domainobject.iterator.IteratorEntity;
import com.infomaximum.database.domainobject.filter.EmptyFilter;
import com.infomaximum.database.exception.ForeignDependencyException;
import com.infomaximum.domain.ExchangeFolderEditable;
import com.infomaximum.domain.ExchangeFolderReadable;
import com.infomaximum.domain.StoreFileEditable;
import com.infomaximum.domain.StoreFileReadable;
import com.infomaximum.database.domainobject.StoreFileDataTest;
import com.infomaximum.domain.type.FormatType;
import org.junit.Assert;
import org.junit.Test;

public class TransactionTest extends StoreFileDataTest {

    @Test
    public void optimisticTransactionLazyTest() throws Exception {
        String fileName = "aaa.txt";
        long size = 15L;

        domainObjectSource.executeTransactional(transaction -> {
            StoreFileEditable storeFile1 = transaction.create(StoreFileEditable.class);
            storeFile1.setFileName(fileName);
            storeFile1.setSize(size);
            transaction.save(storeFile1);

            try (IteratorEntity<StoreFileReadable> ie = transaction.find(StoreFileReadable.class, EmptyFilter.INSTANCE)) {
                StoreFileReadable storeFile2 = ie.next();

                Assert.assertEquals(fileName, storeFile2.getFileName());
                Assert.assertEquals(size, storeFile2.getSize());
            }
        });
    }

    @Test
    public void create() throws Exception {
        //Проверяем, что такого объекта нет в базе
        Assert.assertNull(domainObjectSource.get(StoreFileReadable.class, 1L));

        String fileName="application/json";
        String contentType="info.json";
        long size=1000L;
        FormatType format = FormatType.B;

        //Добавляем объект
        domainObjectSource.executeTransactional(transaction -> {
            StoreFileEditable storeFile = transaction.create(StoreFileEditable.class);
            storeFile.setContentType(contentType);
            storeFile.setFileName(fileName);
            storeFile.setSize(size);
            storeFile.setFormat(format);
            transaction.save(storeFile);
        });

        //Загружаем сохраненый объект
        StoreFileReadable storeFileCheckSave = domainObjectSource.get(StoreFileReadable.class, 1L);
        Assert.assertNotNull(storeFileCheckSave);
        Assert.assertEquals(fileName, storeFileCheckSave.getFileName());
        Assert.assertEquals(contentType, storeFileCheckSave.getContentType());
        Assert.assertEquals(size, storeFileCheckSave.getSize());
        Assert.assertEquals(format, storeFileCheckSave.getFormat());
    }

    @Test
    public void save() throws Exception {
        //Проверяем, что такого объекта нет в базе
        Assert.assertNull(domainObjectSource.get(StoreFileReadable.class, 1L));

        String fileName1 = "info1.json";
        String fileName2 = "info2.json";
        String contentType = "application/json";
        long size = 1000L;

        //Добавляем объект
        try (Transaction transaction = domainObjectSource.buildTransaction()) {
            StoreFileEditable storeFile = transaction.create(StoreFileEditable.class);
            storeFile.setFileName(fileName1);
            storeFile.setContentType(contentType);
            storeFile.setSize(size);
            storeFile.setSingle(false);
            transaction.save(storeFile);
            transaction.commit();
        }

        //Загружаем сохраненый объект
        StoreFileReadable storeFileCheckSave = domainObjectSource.get(StoreFileReadable.class, 1L);
        Assert.assertNotNull(storeFileCheckSave);
        Assert.assertEquals(fileName1, storeFileCheckSave.getFileName());
        Assert.assertEquals(contentType, storeFileCheckSave.getContentType());
        Assert.assertEquals(size, storeFileCheckSave.getSize());
        Assert.assertEquals(false, storeFileCheckSave.isSingle());

        //Редактируем сохраненый объект
        domainObjectSource.executeTransactional(transaction -> {
            StoreFileEditable obj = domainObjectSource.get(StoreFileEditable.class, 1L);
            obj.setFileName(fileName2);
            obj.setSingle(true);
            transaction.save(obj);
        });

        //Загружаем отредактированный объект
        StoreFileReadable editFileCheckSave = domainObjectSource.get(StoreFileReadable.class, 1L);
        Assert.assertNotNull(editFileCheckSave);
        Assert.assertEquals(fileName2, editFileCheckSave.getFileName());
        Assert.assertEquals(contentType, editFileCheckSave.getContentType());
        Assert.assertEquals(size, editFileCheckSave.getSize());
        Assert.assertEquals(true, editFileCheckSave.isSingle());


        //Повторно редактируем сохраненый объект
        domainObjectSource.executeTransactional(transaction -> {
            StoreFileEditable obj = domainObjectSource.get(StoreFileEditable.class, 1L);
            obj.setFileName(fileName1);
            obj.setSingle(false);
            transaction.save(obj);
        });


        StoreFileReadable storeFileCheckSave2 = domainObjectSource.get(StoreFileReadable.class, 1L);
        Assert.assertNotNull(storeFileCheckSave2);
        Assert.assertEquals(fileName1, storeFileCheckSave2.getFileName());
        Assert.assertEquals(contentType, storeFileCheckSave2.getContentType());
        Assert.assertEquals(size, storeFileCheckSave2.getSize());
        Assert.assertEquals(false, storeFileCheckSave2.isSingle());
    }

    @Test
    public void updateByNonExistenceObject() throws Exception {
        createDomain(ExchangeFolderReadable.class);

        try {
            domainObjectSource.executeTransactional(transaction -> {
                StoreFileEditable file = transaction.create(StoreFileEditable.class);
                file.setFolderId(256);
                transaction.save(file);
            });
            Assert.fail();
        } catch (ForeignDependencyException ex) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void updateValueStringEmptyThenNull() throws Exception {
        final long objectId = 1;
        final String emptyFileName = "";
        final String contentType = "info.json";

        //Добавляем объект
        domainObjectSource.executeTransactional(transaction -> {
            StoreFileEditable storeFile = transaction.create(StoreFileEditable.class);
            storeFile.setContentType(contentType);
            storeFile.setFileName(emptyFileName);
            transaction.save(storeFile);
        });

        //Загружаем сохраненый объект
        StoreFileReadable storeFileCheckSave = domainObjectSource.get(StoreFileReadable.class, objectId);
        Assert.assertNotNull(storeFileCheckSave);
        Assert.assertEquals(emptyFileName, storeFileCheckSave.getFileName());
        Assert.assertEquals(contentType, storeFileCheckSave.getContentType());

        //Редактируем сохраненый объект
        domainObjectSource.executeTransactional(transaction -> {
            StoreFileEditable obj = domainObjectSource.get(StoreFileEditable.class, objectId);
            obj.setContentType(null);
            transaction.save(obj);
        });

        //Загружаем сохраненый объект
        StoreFileReadable storeFileCheckEdit = domainObjectSource.get(StoreFileReadable.class, objectId);
        Assert.assertNotNull(storeFileCheckEdit);
        Assert.assertEquals(emptyFileName, storeFileCheckEdit.getFileName());
        Assert.assertNull(storeFileCheckEdit.getContentType());
    }

    @Test
    public void saveEmptyDomainObject() throws Exception {
        final long objectId = 1;
        final String emptyFileName = "";
        final String contentType = "info.json";

        //Добавляем объект
        domainObjectSource.executeTransactional(transaction -> {
            StoreFileEditable storeFile = transaction.create(StoreFileEditable.class);
            storeFile.setContentType(contentType);
            storeFile.setFileName(emptyFileName);
            transaction.save(storeFile);
            transaction.save(storeFile);
        });

        //Загружаем сохраненый объект и сразу без редактирования полей вызываем сохранение
        domainObjectSource.executeTransactional(transaction -> {
            StoreFileEditable obj = transaction.get(StoreFileEditable.class, objectId);
            transaction.save(obj);
        });
    }

    @Test
    public void removeOneObject() throws Exception {
        //Проверяем, что таких объектов нет в базе
        Assert.assertNull(domainObjectSource.get(StoreFileReadable.class, 1L));
        Assert.assertNull(domainObjectSource.get(StoreFileReadable.class, 2L));
        Assert.assertNull(domainObjectSource.get(StoreFileReadable.class, 3L));

        //Добавляем объект
        domainObjectSource.executeTransactional(transaction -> {
            transaction.save(transaction.create(StoreFileEditable.class));
            transaction.save(transaction.create(StoreFileEditable.class));
            transaction.save(transaction.create(StoreFileEditable.class));
        });

        //Проверяем что файлы сохранены
        Assert.assertNotNull(domainObjectSource.get(StoreFileReadable.class, 1L));
        Assert.assertNotNull(domainObjectSource.get(StoreFileReadable.class, 2L));
        Assert.assertNotNull(domainObjectSource.get(StoreFileReadable.class, 3L));

        //Удяляем 2-й объект
        domainObjectSource.executeTransactional(transaction -> {
            transaction.remove(transaction.get(StoreFileEditable.class, 2L));
        });

        //Проверяем, корректность удаления
        Assert.assertNotNull(domainObjectSource.get(StoreFileReadable.class, 1L));
        Assert.assertNull(domainObjectSource.get(StoreFileReadable.class, 2L));
        Assert.assertNotNull(domainObjectSource.get(StoreFileReadable.class, 3L));
    }

    @Test
    public void removeReferencedObject() throws Exception {
        createDomain(ExchangeFolderReadable.class);

        domainObjectSource.executeTransactional(transaction -> {
            ExchangeFolderEditable folder = transaction.create(ExchangeFolderEditable.class);
            transaction.save(folder);

            StoreFileEditable file = transaction.create(StoreFileEditable.class);
            file.setFolderId(folder.getId());
            transaction.save(file);
        });

        try {
            domainObjectSource.executeTransactional(transaction -> {
                transaction.remove(transaction.get(ExchangeFolderEditable.class, 1));
            });
            Assert.fail();
        } catch (ForeignDependencyException ex) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void removeReferencingObjects() throws Exception {
        createDomain(ExchangeFolderReadable.class);

        domainObjectSource.executeTransactional(transaction -> {
            ExchangeFolderEditable folder = transaction.create(ExchangeFolderEditable.class);
            transaction.save(folder);

            StoreFileEditable file = transaction.create(StoreFileEditable.class);
            file.setFolderId(folder.getId());
            transaction.save(file);
        });

        domainObjectSource.executeTransactional(transaction -> {
            transaction.remove(transaction.get(StoreFileEditable.class, 1));
            transaction.remove(transaction.get(ExchangeFolderEditable.class, 1));
        });
    }
}