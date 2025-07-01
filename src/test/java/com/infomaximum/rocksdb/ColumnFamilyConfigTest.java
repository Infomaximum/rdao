package com.infomaximum.rocksdb;

import com.infomaximum.database.utils.TypeConvert;
import com.infomaximum.rocksdb.options.columnfamily.ColumnFamilyConfig;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.util.SizeUnit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ColumnFamilyConfigTest {

    protected Path pathDataBase;

    @BeforeEach
    public void init() throws Exception {
        pathDataBase = Files.createTempDirectory("rocksdb");
        pathDataBase.toAbsolutePath().toFile().deleteOnExit();
    }

    @AfterEach
    public void destroy() throws Exception {
        FileUtils.deleteDirectory(pathDataBase.toAbsolutePath().toFile());
    }


    @Test
    @DisplayName("Тест без указания опций семействам колонок, возвращает значения по умолчанию")
    public void emptyConfiguredColumnTest() throws RocksDBException {

        final String firstColumnName = "com.infomaximum.subsystem.test";
        final String secondColumnName = "com.infomaximum.subsystem.test1";
        final String thirdColumnName = "com.infomaximum.subsystem.test2_postfixName";
        try (RocksDBProvider rocksDBProvider = new RocksDataBaseBuilder().withPath(pathDataBase).build()) {
            rocksDBProvider.createColumnFamily(firstColumnName);
            rocksDBProvider.createColumnFamily(secondColumnName);
            rocksDBProvider.createColumnFamily(thirdColumnName);
        }

        try (RocksDBProvider rocksDBProvider = new RocksDataBaseBuilder()
                .withPath(pathDataBase)
                .build()) {

            final ColumnFamilyHandle firstColumnFamilyHandle = rocksDBProvider.getColumnFamilyHandle(firstColumnName);
            final ColumnFamilyDescriptor firstColumnDescriptor = firstColumnFamilyHandle.getDescriptor();
            Assertions.assertThat(TypeConvert.unpackString(firstColumnDescriptor.getName())).isEqualTo(firstColumnName);
            Assertions.assertThat(firstColumnDescriptor.getOptions().writeBufferSize()).isEqualTo(64L * SizeUnit.MB);

            final ColumnFamilyHandle secondColumnFamilyHandle = rocksDBProvider.getColumnFamilyHandle(secondColumnName);
            final ColumnFamilyDescriptor secondColumnDescriptor = secondColumnFamilyHandle.getDescriptor();
            Assertions.assertThat(TypeConvert.unpackString(secondColumnDescriptor.getName())).isEqualTo(secondColumnName);
            Assertions.assertThat(secondColumnDescriptor.getOptions().writeBufferSize()).isEqualTo(64L * SizeUnit.MB);

            final ColumnFamilyHandle thirdColumnFamilyHandle = rocksDBProvider.getColumnFamilyHandle(thirdColumnName);
            final ColumnFamilyDescriptor thirdColumnDescriptor = thirdColumnFamilyHandle.getDescriptor();
            Assertions.assertThat(TypeConvert.unpackString(thirdColumnDescriptor.getName())).isEqualTo(thirdColumnName);
            Assertions.assertThat(thirdColumnDescriptor.getOptions().writeBufferSize()).isEqualTo(64L * SizeUnit.MB);

        }
    }


    @Test
    @DisplayName("Тест указания опций конкретному семейству колонок, для настроенной колонки возвращает установленно значение, для остальных возвращает значения по умолчанию")
    public void notCorrectConfiguredColumnTest() throws RocksDBException {

        final String firstColumnName = "com.infomaximum.subsystem.test";
        final String secondColumnName = "com.infomaximum.subsystem.test1";
        final String thirdColumnName = "com.infomaximum.subsystem.test2_postfixName";
        try (RocksDBProvider rocksDBProvider = new RocksDataBaseBuilder().withPath(pathDataBase).build()) {
            rocksDBProvider.createColumnFamily(firstColumnName);
            rocksDBProvider.createColumnFamily(secondColumnName);
            rocksDBProvider.createColumnFamily(thirdColumnName);
        }


        Assertions.assertThatThrownBy(() -> {
                    try (RocksDBProvider rocksDBProvider = new RocksDataBaseBuilder()
                            .withPath(pathDataBase)
                            .withConfigColumnFamilies(
                                    new HashMap<String, ColumnFamilyConfig>() {{
                                        put(null, null);
                                    }})
                            .build()) {
                    }
                }).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("column name pattern cannot be null");

        //-------------------------------------------------------------------------------

        Assertions.assertThatThrownBy(() -> {
                    try (RocksDBProvider rocksDBProvider = new RocksDataBaseBuilder()
                            .withPath(pathDataBase)
                            .withConfigColumnFamilies(
                                    new HashMap<String, ColumnFamilyConfig>() {{
                                        put("com.infomaximum.subsystem.test", null);
                                    }})
                            .build()) {
                    }
                }).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("column family config cannot be null");
    }


    @Test
    @DisplayName("Тест указания опций конкретному семейству колонок, для настроенной колонки возвращает установленно значение, для остальных возвращает значения по умолчанию")
    public void specificNameConfiguredColumnTest() throws RocksDBException {

        final String firstColumnName = "com.infomaximum.subsystem.test";
        final String secondColumnName = "com.infomaximum.subsystem.test1";
        final String thirdColumnName = "com.infomaximum.subsystem.test2_postfixName";
        try (RocksDBProvider rocksDBProvider = new RocksDataBaseBuilder().withPath(pathDataBase).build()) {
            rocksDBProvider.createColumnFamily(firstColumnName);
            rocksDBProvider.createColumnFamily(secondColumnName);
            rocksDBProvider.createColumnFamily(thirdColumnName);
        }


        final long writeBufferSize = 4L * SizeUnit.MB;
        Map<String, ColumnFamilyConfig> configuredColumnFamilies = new HashMap<String, ColumnFamilyConfig>() {{
            put("^com\\.infomaximum\\.subsystem\\.test$", ColumnFamilyConfig.builder().withWriteBufferSize(writeBufferSize).build());
        }};

        try (RocksDBProvider rocksDBProvider = new RocksDataBaseBuilder()
                .withPath(pathDataBase)
                .withConfigColumnFamilies(configuredColumnFamilies)
                .build()) {

            final ColumnFamilyHandle firstColumnFamilyHandle = rocksDBProvider.getColumnFamilyHandle(firstColumnName);
            final ColumnFamilyDescriptor firstColumnDescriptor = firstColumnFamilyHandle.getDescriptor();
            Assertions.assertThat(TypeConvert.unpackString(firstColumnDescriptor.getName())).isEqualTo(firstColumnName);
            Assertions.assertThat(firstColumnDescriptor.getOptions().writeBufferSize()).isEqualTo(writeBufferSize);


            final ColumnFamilyHandle secondColumnFamilyHandle = rocksDBProvider.getColumnFamilyHandle(secondColumnName);
            final ColumnFamilyDescriptor secondColumnDescriptor = secondColumnFamilyHandle.getDescriptor();
            Assertions.assertThat(TypeConvert.unpackString(secondColumnDescriptor.getName())).isEqualTo(secondColumnName);
            Assertions.assertThat(secondColumnDescriptor.getOptions().writeBufferSize()).isEqualTo(64L * SizeUnit.MB);

            final ColumnFamilyHandle thirdColumnFamilyHandle = rocksDBProvider.getColumnFamilyHandle(thirdColumnName);
            final ColumnFamilyDescriptor thirdColumnDescriptor = thirdColumnFamilyHandle.getDescriptor();
            Assertions.assertThat(TypeConvert.unpackString(thirdColumnDescriptor.getName())).isEqualTo(thirdColumnName);
            Assertions.assertThat(thirdColumnDescriptor.getOptions().writeBufferSize()).isEqualTo(64L * SizeUnit.MB);

        }
    }

    @Test
    @DisplayName("Тест указания опций набору семейств колонок, для настроенных колонки возвращает установленно значение, для остальных возвращает значения по умолчанию")
    public void maskedNameConfiguredColumnTest() throws RocksDBException {

        final String firstColumnName = "com.infomaximum.subsystem.test";
        final String secondColumnName = "com.infomaximum.subsystem.test1";
        final String thirdColumnName = "com.infomaximum.subsystem.test2_postfixName";
        final String fourthColumnName = "com.infomaximum.subsystem.otherName";
        try (RocksDBProvider rocksDBProvider = new RocksDataBaseBuilder().withPath(pathDataBase).build()) {
            rocksDBProvider.createColumnFamily(firstColumnName);
            rocksDBProvider.createColumnFamily(secondColumnName);
            rocksDBProvider.createColumnFamily(thirdColumnName);
            rocksDBProvider.createColumnFamily(fourthColumnName);
        }


        final long writeBufferSize = 4L * SizeUnit.MB;
        Map<String, ColumnFamilyConfig> configuredColumnFamilies = new HashMap<String, ColumnFamilyConfig>() {{
            put("^com\\.infomaximum\\.subsystem\\.test.*$", ColumnFamilyConfig.builder().withWriteBufferSize(writeBufferSize).build());
        }};

        try (RocksDBProvider rocksDBProvider = new RocksDataBaseBuilder()
                .withPath(pathDataBase)
                .withConfigColumnFamilies(configuredColumnFamilies)
                .build()) {

            final ColumnFamilyHandle firstColumnFamilyHandle = rocksDBProvider.getColumnFamilyHandle(firstColumnName);
            final ColumnFamilyDescriptor firstColumnDescriptor = firstColumnFamilyHandle.getDescriptor();
            Assertions.assertThat(TypeConvert.unpackString(firstColumnDescriptor.getName())).isEqualTo(firstColumnName);
            Assertions.assertThat(firstColumnDescriptor.getOptions().writeBufferSize()).isEqualTo(writeBufferSize);


            final ColumnFamilyHandle secondColumnFamilyHandle = rocksDBProvider.getColumnFamilyHandle(secondColumnName);
            final ColumnFamilyDescriptor secondColumnDescriptor = secondColumnFamilyHandle.getDescriptor();
            Assertions.assertThat(TypeConvert.unpackString(secondColumnDescriptor.getName())).isEqualTo(secondColumnName);
            Assertions.assertThat(secondColumnDescriptor.getOptions().writeBufferSize()).isEqualTo(writeBufferSize);

            final ColumnFamilyHandle thirdColumnFamilyHandle = rocksDBProvider.getColumnFamilyHandle(thirdColumnName);
            final ColumnFamilyDescriptor thirdColumnDescriptor = thirdColumnFamilyHandle.getDescriptor();
            Assertions.assertThat(TypeConvert.unpackString(thirdColumnDescriptor.getName())).isEqualTo(thirdColumnName);
            Assertions.assertThat(thirdColumnDescriptor.getOptions().writeBufferSize()).isEqualTo(writeBufferSize);

            final ColumnFamilyHandle fourthColumnFamilyHandle = rocksDBProvider.getColumnFamilyHandle(fourthColumnName);
            final ColumnFamilyDescriptor fourthColumnDescriptor = fourthColumnFamilyHandle.getDescriptor();
            Assertions.assertThat(TypeConvert.unpackString(fourthColumnDescriptor.getName())).isEqualTo(fourthColumnName);
            Assertions.assertThat(fourthColumnDescriptor.getOptions().writeBufferSize()).isEqualTo(64L * SizeUnit.MB);
        }
    }
}
