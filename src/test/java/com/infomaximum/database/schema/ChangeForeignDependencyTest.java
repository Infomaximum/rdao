package com.infomaximum.database.schema;

import com.google.common.collect.Sets;
import com.infomaximum.database.domainobject.DomainObjectSource;
import com.infomaximum.database.domainobject.filter.HashFilter;
import com.infomaximum.database.domainobject.iterator.IteratorEntity;
import com.infomaximum.database.exception.*;
import com.infomaximum.database.schema.table.TField;
import com.infomaximum.database.schema.table.THashIndex;
import com.infomaximum.database.schema.table.Table;
import com.infomaximum.database.schema.table.TableReference;
import com.infomaximum.domain.*;
import com.infomaximum.domain.type.FormatType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

public class ChangeForeignDependencyTest extends DomainDataJ5Test {

    private Schema schema;

    private static ArrayList<THashIndex> getHashIndexByField(List<THashIndex> hashIndexes, String... fieldNames) {
        ArrayList<THashIndex> result = new ArrayList<>();
        for (THashIndex hashIndex : hashIndexes) {
            final String[] fields = hashIndex.getFields();
            if (new HashSet(Arrays.asList(fields)).equals(new HashSet(Arrays.asList(fieldNames)))) {
                result.add(hashIndex);
            }
        }
        return result;
    }

    @Override
    public void createSchema() {
        schema = Schema.create(rocksDBProvider);
        StructEntity general = new StructEntity(GeneralReadable.class);
        schema.createTable(general);
        StructEntity exchangeFolder = new StructEntity(ExchangeFolderReadable.class);
        schema.createTable(exchangeFolder);
        StructEntity storeFile = new StructEntity(StoreFileReadable.class);
        schema.createTable(storeFile);
    }

    @Test
    @DisplayName("При удалении foreign dependency для поля, схема больше не содержит ссылку на TableReference, но индекс существует.")
    void dropForeignDependencySuccessfully() throws DatabaseException {
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        final Optional<TField> folderIdOptional = storeFileTable.getFields()
                .stream()
                .filter(tField -> tField.getName().equals("folder_id"))
                .findFirst();

        Assertions.assertThat(folderIdOptional.isPresent()).isTrue();

        final TField folderIdField = folderIdOptional.get();

        Assertions.assertThat(folderIdField)
                .returns("folder_id", TField::getName)
                .returns(Long.class, TField::getType)
                .returns("ExchangeFolder", tField -> tField.getForeignTable().getName())
                .returns("com.infomaximum.exchange", tField -> tField.getForeignTable().getNamespace());

        final List<THashIndex> hashIndexes = storeFileTable.getHashIndexes();

        final ArrayList<THashIndex> hashIndexByField = getHashIndexByField(hashIndexes, "folder_id");

        Assertions.assertThat(hashIndexByField)
                .hasSize(1);

        Assertions.assertThat(hashIndexByField.get(0))
                .returns(new String[]{"folder_id"}, THashIndex::getFields);


        schema.dropForeignKey(new TField("folder_id", Long.class), storeFileTable);

        final Table storeFileTableCorrect = schema.getTable("StoreFile", "com.infomaximum.store");

        final Optional<TField> folderIdCorrectOptional = storeFileTableCorrect.getFields()
                .stream()
                .filter(tField -> tField.getName().equals("folder_id"))
                .findFirst();

        Assertions.assertThat(folderIdCorrectOptional.isPresent()).isTrue();

        Assertions.assertThat(folderIdCorrectOptional.get())
                .returns("folder_id", TField::getName)
                .returns(Long.class, TField::getType)
                .returns(null, tField -> tField.getForeignTable());


        final ArrayList<THashIndex> correctHashIndexByField = getHashIndexByField(storeFileTableCorrect.getHashIndexes(), "folder_id");

        Assertions.assertThat(correctHashIndexByField)
                .hasSize(1);

        Assertions.assertThat(correctHashIndexByField.get(0))
                .returns(new String[]{"folder_id"}, THashIndex::getFields);
    }

    @Test
    @DisplayName("При удалении foreign dependency, без удаления индекса, проверка целостности выдает ошибку.")
    void dropForeignDependencyWithoutIndexIntegrityException() throws DatabaseException {
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        schema.dropForeignKey(new TField("folder_id", Long.class), storeFileTable);


        Assertions.assertThatThrownBy(
                        () -> schema.checkSubsystemIntegrity(
                                Sets.newHashSet(new StructEntity(StoreFileReadable.class)),
                                "com.infomaximum.store"
                        ))
                .isInstanceOf(InconsistentTableException.class);
    }

    @Test
    @DisplayName("При удалении foreign dependency, и удалении индекса проверка целостности проходит успешно.")
    void dropForeignDependencyWithIndexIntegritySuccessfully() throws DatabaseException {
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        schema.dropForeignKey(new TField("folder_id", Long.class), storeFileTable);
        schema.dropIndex(new THashIndex("folder_id"), storeFileTable.getName(), storeFileTable.getNamespace());
        final Table correctStoreFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        final ArrayList<THashIndex> hashIndexByField = getHashIndexByField(correctStoreFileTable.getHashIndexes(), "folder_id");
        Assertions.assertThat(hashIndexByField)
                .isEmpty();
    }

    @Test
    @DisplayName("При удалении foreign dependency для не существующего поля в таблице")
    void dropForeignDependencyOnNotExistField() throws DatabaseException {
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        Assertions.assertThatThrownBy(
                () -> {
                    schema.dropForeignKey(new TField("not_exist_field_name", Long.class), storeFileTable);
                }
        ).isInstanceOf(FieldNotFoundException.class);
    }

    @Test
    @DisplayName("При удалении foreign dependency для поля у которого нет foreign dependency генерируется исключение.")
    void dropForeignDependencyOnNotForeignDependencyField() throws DatabaseException {
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        Assertions.assertThatThrownBy(
                () -> {
                    schema.dropForeignKey(new TField("size", Long.class), storeFileTable);
                }
        ).isInstanceOf(ForeignDependencyNotFoundException.class);
    }

    @Test
    @DisplayName("При добавлении foreign dependency для несуществующего поля генерируется исключение.")
    void appendForeignKeyOnNotExistField() throws DatabaseException {
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        Assertions.assertThatThrownBy(
                () -> {
                    schema.appendForeignKey(new TField("not_exist_field", new TableReference("ExchangeFolder", "com.infomaximum.exchange")), storeFileTable);
                }
        ).isInstanceOf(FieldNotFoundException.class);
    }

    @Test
    @DisplayName("При добавлении foreign dependency для поля с типом отличным от Long.class генерируется исключение.")
    void appendForeignKeyOnInvalidFieldType() throws DatabaseException {
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        Assertions.assertThatThrownBy(
                () -> {
                    schema.appendForeignKey(new TField("single", new TableReference("ExchangeFolder", "com.infomaximum.exchange")), storeFileTable);
                }
        ).isInstanceOf(IllegalTypeException.class);
    }

    @Test
    @DisplayName("При добавлении foreign dependency, полю у которого уже есть foreign dependency генерируется исключение.")
    void appendForeignKeyOnExistingForeignKey() throws DatabaseException {
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        Assertions.assertThatThrownBy(
                () -> {
                    schema.appendForeignKey(new TField("folder_id", new TableReference("ExchangeFolder", "com.infomaximum.exchange")), storeFileTable);
                }
        ).isInstanceOf(ForeignDependencyAlreadyExistException.class);
    }

    @Test
    @DisplayName("При добавлении foreign dependency, полю на не существующую таблицу, генерируется исключение.")
    void appendForeignKeyOnNotExistingReferenceTable() throws DatabaseException {
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        Assertions.assertThatThrownBy(
                () -> {
                    schema.appendForeignKey(new TField("size", new TableReference("NotExistTableName", "com.infomaximum.exchange")), storeFileTable);
                }
        ).isInstanceOf(TableNotFoundException.class);
    }

    @Test
    @DisplayName("При добавлении foreign dependency, полю на пустых таблицах, в схеме у поля добавляется foreign_table_id и создается индекс")
    void appendForeignKeyOnEmptyTables() throws DatabaseException {
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        final String sizeFieldName = "size";
        Assertions.assertThatCode(
                () -> {
                    schema.appendForeignKey(new TField(sizeFieldName, new TableReference("ExchangeFolder", "com.infomaximum.exchange")), storeFileTable);
                }
        ).doesNotThrowAnyException();

        final Table correctStoreFileTable = schema.getTable("StoreFile", "com.infomaximum.store");


        final Optional<TField> sizeCorrectOptional = correctStoreFileTable.getFields()
                .stream()
                .filter(tField -> tField.getName().equals(sizeFieldName))
                .findFirst();

        Assertions.assertThat(sizeCorrectOptional.isPresent()).isTrue();

        Assertions.assertThat(sizeCorrectOptional.get())
                .returns(sizeFieldName, TField::getName)
                .returns(Long.class, TField::getType)
                .returns("ExchangeFolder", tField -> tField.getForeignTable().getName())
                .returns("com.infomaximum.exchange", tField -> tField.getForeignTable().getNamespace());

        final ArrayList<THashIndex> correctHashIndexByField = getHashIndexByField(correctStoreFileTable.getHashIndexes(), sizeFieldName);

        Assertions.assertThat(correctHashIndexByField)
                .hasSize(1);

        Assertions.assertThat(correctHashIndexByField.get(0))
                .returns(new String[]{sizeFieldName}, THashIndex::getFields);
    }

    @Test
    @DisplayName("При добавлении foreign dependency, полю на таблицах с корректными данными, в схеме у поля добавляется foreign_table_id и создается индекс"
            + " После создания делается попытка удалить объект на который установлена ссылка.")
    void appendForeignKeyOnTablesWithDataSuccessfully() throws Exception {
        domainObjectSource = new DomainObjectSource(rocksDBProvider, true);

        initAndFillStoreFiles(domainObjectSource, 10, false);
        initAndFillExchangeFolder(domainObjectSource, 10);

        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        final String sizeFieldName = "size";
        Assertions.assertThatCode(
                () -> {
                    schema.appendForeignKey(new TField(sizeFieldName, new TableReference("ExchangeFolder", "com.infomaximum.exchange")), storeFileTable);
                }
        ).doesNotThrowAnyException();

        final Table correctStoreFileTable = schema.getTable("StoreFile", "com.infomaximum.store");


        final Optional<TField> sizeCorrectOptional = correctStoreFileTable.getFields()
                .stream()
                .filter(tField -> tField.getName().equals(sizeFieldName))
                .findFirst();

        Assertions.assertThat(sizeCorrectOptional.isPresent()).isTrue();

        Assertions.assertThat(sizeCorrectOptional.get())
                .returns(sizeFieldName, TField::getName)
                .returns(Long.class, TField::getType)
                .returns("ExchangeFolder", tField -> tField.getForeignTable().getName())
                .returns("com.infomaximum.exchange", tField -> tField.getForeignTable().getNamespace());

        final ArrayList<THashIndex> correctHashIndexByField = getHashIndexByField(correctStoreFileTable.getHashIndexes(), sizeFieldName);

        Assertions.assertThat(correctHashIndexByField)
                .hasSize(1);

        Assertions.assertThat(correctHashIndexByField.get(0))
                .returns(new String[]{sizeFieldName}, THashIndex::getFields);

        try (IteratorEntity<StoreFileReadable> iterator
                     = domainObjectSource.find(StoreFileReadable.class, new HashFilter(StoreFileReadable.FIELD_SIZE, 1L))) {
            while (iterator.hasNext()) {
                final StoreFileReadable next = iterator.next();
                Assertions.assertThat(next)
                        .returns(1L, storeFileReadable -> storeFileReadable.getSize())
                        .returns("name", storeFileReadable -> storeFileReadable.getFileName())
                        .returns(true, storeFileReadable -> storeFileReadable.isSingle())
                        .returns(FormatType.B, storeFileReadable -> storeFileReadable.getFormat());
            }
        }
    }

    @Test
    @DisplayName("При добавлении foreign dependency, полю в таблице с данными, где значение foreign dependency null ")
    @Disabled("NPE не вылетает после этих изменений hash_commit:72df4ec5e0d1c82dc6ecbbc38ac938475831bc25")
    void appendForeignKeyOnTablesWithDataNullReference() throws Exception {
        domainObjectSource = new DomainObjectSource(rocksDBProvider, true);
        initAndFillStoreFiles(domainObjectSource, 10, true);
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        final String sizeFieldName = "size";
        Assertions.assertThatThrownBy(
                () -> {
                    schema.appendForeignKey(new TField(sizeFieldName, new TableReference("ExchangeFolder", "com.infomaximum.exchange")), storeFileTable);
                }
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("При добавлении foreign dependency, полю в таблице с данными, где есть значения, которых нет в reference table, генерирует исключение")
    void appendForeignKeyOnTablesWithDataNotExistReferenceRecords() throws Exception {
        domainObjectSource = new DomainObjectSource(rocksDBProvider, true);
        initAndFillStoreFiles(domainObjectSource, 10, false);
        initAndFillExchangeFolder(domainObjectSource, 5);
        final Table storeFileTable = schema.getTable("StoreFile", "com.infomaximum.store");
        final String sizeFieldName = "size";
        Assertions.assertThatThrownBy(
                () -> {
                    schema.appendForeignKey(new TField(sizeFieldName, new TableReference("ExchangeFolder", "com.infomaximum.exchange")), storeFileTable);
                }
        ).isInstanceOf(ForeignDependencyException.class);
    }

    private void initAndFillStoreFiles(DomainObjectSource domainObjectSource, int recordCount, boolean isNullReference) throws Exception {
        domainObjectSource.executeTransactional(transaction -> {
            for (int i = 0; i < recordCount; i++) {
                StoreFileEditable obj = transaction.create(StoreFileEditable.class);
                if (!isNullReference) {
                    obj.setSize(i + 1);
                }
                obj.setFileName("name");
                obj.setContentType("type");
                obj.setSingle(true);
                obj.setFormat(FormatType.B);
                transaction.save(obj);
            }
        });
    }


    private void initAndFillExchangeFolder(DomainObjectSource domainObjectSource, int recordCount) throws Exception {
        domainObjectSource.executeTransactional(transaction -> {
            for (int i = 0; i < recordCount; i++) {
                ExchangeFolderEditable obj = transaction.create(ExchangeFolderEditable.class);
                obj.setUuid(UUID.randomUUID().toString());
                obj.setUserEmail("name");
                transaction.save(obj);
            }
        });
    }
}
