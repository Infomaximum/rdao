package com.infomaximum.rocksdb;

import com.infomaximum.database.DataCommand;
import com.infomaximum.database.RecordSource;
import com.infomaximum.database.domainobject.filter.HashFilter;
import com.infomaximum.database.exception.TableAlreadyExistsException;
import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.provider.DBTransaction;
import com.infomaximum.database.schema.Schema;
import com.infomaximum.database.schema.table.TField;
import com.infomaximum.database.schema.table.THashIndex;
import com.infomaximum.database.schema.table.Table;
import com.infomaximum.domain.GeneralReadable;
import com.infomaximum.rocksdb.options.columnfamily.ColumnFamilyConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rocksdb.FlushOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.MutableDBOptions;
import org.rocksdb.Status;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

public class ColumnFamilyHistoryApiTest extends RocksDataTest {

    private static final long MIB = 1024 * 1024;
    private static final String NAMESPACE = "com.infomaximum.rocksdb";

    @BeforeClass
    public static void loadLibrary() {
        RocksDB.loadLibrary();
        Schema.resolve(GeneralReadable.class);
    }

    @Test
    public void configuredTableCommitsAfterFlushAndPreservesSchemaRecordsAndIndexes() throws Exception {
        Table table = indexedTable();
        long id;
        try (RocksDBProvider provider = new RocksDataBaseBuilder().withPath(pathDataBase).build();
             FlushOptions flush = new FlushOptions().setWaitForFlush(true)) {
            Schema schema = Schema.create(provider);
            var config = history();
            schema.createTable(table, config);
            var dbTable = schema.getDbSchema().getTable(table.getName(), NAMESPACE);
            assertOption(dbTable.getDataColumnFamily(), "max_write_buffer_size_to_maintain", 12 * MIB);
            assertOption(dbTable.getIndexColumnFamily(), "max_write_buffer_size_to_maintain", 12 * MIB);
            assertTrue(provider.containsSequence(dbTable.getDataColumnFamily()));
            schema.checkIntegrity();

            RecordSource records = new RecordSource(provider);
            try (DBTransaction transaction = provider.beginTransaction()) {
                DataCommand command = new DataCommand(transaction, schema.getDbSchema());
                id = command.insertRecord(table.getName(), NAMESPACE, new Object[]{42L});
                records.executeTransactional(outside ->
                        outside.insertRecord(table.getName(), NAMESPACE, new Object[]{84L}));
                provider.getRocksDB().flush(flush, provider.getColumnFamilyHandle(dbTable.getDataColumnFamily()));
                provider.getRocksDB().flush(flush, provider.getColumnFamilyHandle(dbTable.getIndexColumnFamily()));
                transaction.commit();
            }
            assertThrows(TableAlreadyExistsException.class, () -> schema.createTable(table, config));
        }
        try (RocksDBProvider provider = new RocksDataBaseBuilder().withPath(pathDataBase).build()) {
            Schema schema = Schema.read(provider);
            schema.checkIntegrity();
            assertTrue(table.same(schema.getTable(table.getName(), NAMESPACE)));
            RecordSource records = new RecordSource(provider);
            assertArrayEquals(new Object[]{42L}, records.getById(table.getName(), NAMESPACE, id).getValues());
            try (var iterator = records.select(table.getName(), NAMESPACE, new HashFilter(0, 42L))) {
                assertTrue(iterator.hasNext());
                assertEquals(id, iterator.next().getId());
                assertFalse(iterator.hasNext());
            }
            int count = 0;
            try (var iterator = records.select(table.getName(), NAMESPACE)) {
                while (iterator.hasNext()) {
                    iterator.next();
                    count++;
                }
            }
            assertEquals(2, count);
        }
    }

    @Test
    public void unspecifiedAndExplicitZeroPreserveDefaultCreationBehavior() throws Exception {
        try (RocksDBProvider provider = new RocksDataBaseBuilder().withPath(pathDataBase).build()) {
            Schema schema = Schema.create(provider);
            schema.createTable(table("unspecified"));
            schema.createTable(table("zero"), ColumnFamilyConfig.builder()
                    .withMaxWriteBufferSizeToMaintain(0L).build());
            for (String name : List.of("unspecified", "zero")) {
                var table = schema.getDbSchema().getTable(name, NAMESPACE);
                assertOption(table.getDataColumnFamily(), "max_write_buffer_size_to_maintain", 0);
                assertOption(table.getIndexColumnFamily(), "max_write_buffer_size_to_maintain", 0);
            }
        }
    }

    @Test
    public void historySizeUsesLongAndRejectsInvalidNegativeValues() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> ColumnFamilyConfig.builder()
                .withMaxWriteBufferSizeToMaintain(-2L));
        long size = 3 * 1024 * MIB;
        try (RocksDBProvider provider = new RocksDataBaseBuilder().withPath(pathDataBase).build()) {
            provider.createColumnFamily("large-target", ColumnFamilyConfig.builder()
                    .withMaxWriteBufferSizeToMaintain(size).build());
            assertOption("large-target", "max_write_buffer_size_to_maintain", size);
        }
    }

    @Test
    public void firstCommitToEmptyColumnFamilySurvivesWalPressure() throws Exception {
        try (RocksDBProvider provider = new RocksDataBaseBuilder().withPath(pathDataBase).build();
             FlushOptions flush = new FlushOptions().setWaitForFlush(true)) {
            provider.getRocksDB().setDBOptions(MutableDBOptions.builder().setMaxTotalWalSize(32 * 1024).build());
            provider.createColumnFamily("migration", history());
            provider.createColumnFamily("previous", history());
            provider.getRocksDB().put(provider.getColumnFamilyHandle("previous"), bytes("seed"), new byte[128 * 1024]);
            provider.getRocksDB().put(bytes("sequence"), bytes("0"));
            provider.getRocksDB().flush(flush);
            try (DBTransaction transaction = provider.beginTransaction()) {
                transaction.put("migration", bytes("target"), bytes("value"));
                byte[] payload = new byte[64 * 1024];
                for (int i = 0; i < 100; i++) {
                    provider.getRocksDB().put(bytes("sequence"), payload);
                }
                provider.getRocksDB().flush(flush);
                transaction.commit();
            }
            assertArrayEquals(bytes("value"), provider.getValue("migration", bytes("target")));
        }
    }

    @Test
    public void realConflictIsRejectedAfterFlush() throws Exception {
        try (RocksDBProvider provider = new RocksDataBaseBuilder().withPath(pathDataBase).build();
             FlushOptions flush = new FlushOptions().setWaitForFlush(true)) {
            provider.createColumnFamily("migration", history());
            var cf = provider.getColumnFamilyHandle("migration");
            provider.getRocksDB().put(cf, bytes("seed"), bytes("before"));
            try (DBTransaction transaction = provider.beginTransaction()) {
                transaction.put("migration", bytes("target"), bytes("pending"));
                transaction.put("migration", bytes("only-in-transaction"), bytes("pending"));
                provider.getRocksDB().put(cf, bytes("target"), bytes("outside"));
                provider.getRocksDB().flush(flush, cf);
                DatabaseException failure = assertThrows(DatabaseException.class, transaction::commit);
                assertEquals(Status.Code.Busy, ((RocksDBException) failure.getCause()).getStatus().getCode());
            }
            assertArrayEquals(bytes("outside"), provider.getValue("migration", bytes("target")));
            assertNull(provider.getValue("migration", bytes("only-in-transaction")));
        }
    }

    @Test
    public void defaultCreationStillReproducesMissingHistoryFailure() throws Exception {
        try (RocksDBProvider provider = new RocksDataBaseBuilder().withPath(pathDataBase).build();
             FlushOptions flush = new FlushOptions().setWaitForFlush(true)) {
            provider.createColumnFamily("migration");
            var cf = provider.getColumnFamilyHandle("migration");
            provider.getRocksDB().put(cf, bytes("seed"), bytes("before"));
            try (DBTransaction transaction = provider.beginTransaction()) {
                transaction.put("migration", bytes("target"), bytes("pending"));
                provider.getRocksDB().put(cf, bytes("unrelated"), bytes("outside"));
                provider.getRocksDB().flush(flush, cf);
                DatabaseException failure = assertThrows(DatabaseException.class, transaction::commit);
                assertEquals(Status.Code.TryAgain, ((RocksDBException) failure.getCause()).getStatus().getCode());
            }
            assertNull(provider.getValue("migration", bytes("target")));
        }
    }

    private static ColumnFamilyConfig history() {
        return ColumnFamilyConfig.builder().withWriteBufferSize(4 * MIB)
                .withMaxWriteBufferNumber(3).withMaxWriteBufferSizeToMaintain(-1L).build();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static Table table(String name) {
        return new Table(name, NAMESPACE, List.of(new TField("value", Long.class)));
    }

    private static Table indexedTable() {
        return new Table("general", NAMESPACE, List.of(new TField("value", Long.class)),
                List.of(new THashIndex("value")));
    }

    private void assertOption(String columnFamily, String property, long expected) throws Exception {
        Path latest;
        try (var files = Files.list(pathDataBase)) {
            latest = files.filter(path -> path.getFileName().toString().matches("OPTIONS-[0-9]+"))
                    .max(Comparator.comparing(path -> path.getFileName().toString())).orElseThrow();
        }
        boolean inColumnFamily = false;
        for (String line : Files.readAllLines(latest)) {
            line = line.trim();
            if (line.startsWith("[")) {
                inColumnFamily = line.equals("[CFOptions \"" + columnFamily + "\"]");
            } else if (inColumnFamily && line.startsWith(property + "=")) {
                assertEquals(columnFamily + ": " + property, expected,
                        Long.parseLong(line.substring(property.length() + 1).trim()));
                return;
            }
        }
        fail("Missing option " + property + " for " + columnFamily);
    }
}
