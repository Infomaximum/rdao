package com.infomaximum.database.domainobject;

import com.infomaximum.database.core.schema.EntityField;
import com.infomaximum.database.core.schema.EntityPrefixIndex;
import com.infomaximum.database.core.schema.StructEntity;
import com.infomaximum.database.core.schema.EntityIndex;
import com.infomaximum.database.datasource.DataSource;
import com.infomaximum.database.datasource.KeyPattern;
import com.infomaximum.database.domainobject.key.FieldKey;
import com.infomaximum.database.exeption.DataSourceDatabaseException;
import com.infomaximum.database.exeption.DatabaseException;
import com.infomaximum.database.exeption.TransactionDatabaseException;
import com.infomaximum.database.utils.TypeConvert;

public class DomainObjectSource extends DataEnumerable {

    public interface Monad {

        /**
         * Реализация операции.
         * @param transaction Контекст, в котором выполняется операция.
         * @throws Exception Если во время выполнения операции возникла ошибка.
         */
        public void action(final Transaction transaction) throws Exception;
    }

    public DomainObjectSource(DataSource dataSource) {
        super(dataSource);
    }

    public void executeTransactional(final Monad operation) throws TransactionDatabaseException {
        try (Transaction transaction = buildTransaction()) {
            operation.action(transaction);
            transaction.commit();
        } catch (Exception ex) {
            throw new TransactionDatabaseException("Exception execute transaction", ex);
        }
    }

    public Transaction buildTransaction() {
        return new Transaction(dataSource);
    }

    @Override
    public <T extends Object, U extends DomainObject> T getValue(final EntityField field, U object) throws DataSourceDatabaseException {
        byte[] value = dataSource.getValue(object.getStructEntity().getName(), new FieldKey(object.getId(), field.getName()).pack());
        return (T) TypeConvert.unpack(field.getType(), value, field.getPacker());
    }

    @Override
    public long createIterator(String columnFamily, KeyPattern pattern) throws DataSourceDatabaseException {
        return dataSource.createIterator(columnFamily, pattern);
    }

    public <T extends DomainObject> void createEntity(final Class<T> clazz) throws DatabaseException {
        StructEntity entity = StructEntity.getInstance(clazz);
        dataSource.createColumnFamily(entity.getName());
        dataSource.createSequence(entity.getName());
        for (EntityIndex i : entity.getIndexes()) {
            dataSource.createColumnFamily(i.columnFamily);
        }

        for (EntityPrefixIndex i : entity.getPrefixIndexes()) {
            dataSource.createColumnFamily(i.columnFamily);
        }

        //TODO realize
    }
}
