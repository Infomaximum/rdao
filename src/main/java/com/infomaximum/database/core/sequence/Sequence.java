package com.infomaximum.database.core.sequence;

import com.infomaximum.database.utils.TypeConvert;
import com.infomaximum.rocksdb.struct.RocksDataBase;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kris on 22.04.17.
 */
public class Sequence {

    private final static int SIZE_CACHE=10;

    private final RocksDataBase rocksDataBase;
    private final ColumnFamilyHandle columnFamilyHandle;
    private final byte[] sequenceName;

    private final AtomicLong increment;
    private long maxCacheIncrement;

    public Sequence(RocksDataBase rocksDataBase, ColumnFamilyHandle columnFamilyHandle, byte[] sequenceName) throws RocksDBException {
        this.rocksDataBase=rocksDataBase;
        this.columnFamilyHandle = columnFamilyHandle;
        this.sequenceName = sequenceName;

        increment = new AtomicLong(0);
        byte[] value = rocksDataBase.getRocksDB().get(columnFamilyHandle, sequenceName);
        if (value==null) {
            increment.set(0);
            maxCacheIncrement=0;
        } else {
            long lValue = TypeConvert.getLong(value);
            increment.set(lValue);
            maxCacheIncrement=lValue;
        }
        tryRefillCacheIncrement();
    }

    private synchronized void tryRefillCacheIncrement() throws RocksDBException {
        if (maxCacheIncrement - increment.get()>SIZE_CACHE) return;
        maxCacheIncrement += SIZE_CACHE;
        rocksDataBase.getRocksDB().put(columnFamilyHandle, sequenceName, TypeConvert.pack(maxCacheIncrement));
    }

    public long next() throws RocksDBException {
        long value;
        do {
            value = increment.get();
            if (value>=maxCacheIncrement) {
                //Кеш закончился-берем еще
                tryRefillCacheIncrement();
            }
        } while (!increment.compareAndSet(value, value + 1));
        return value + 1;
    }
}