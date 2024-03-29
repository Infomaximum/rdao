package com.infomaximum.rocksdb.options.columnfamily;

import com.infomaximum.database.utils.TypeConvert;
import org.rocksdb.ColumnFamilyDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ColumnFamilyConfigService {

    private Map<String, ColumnFamilyConfig> configuredColumnFamilies;

    public ColumnFamilyConfigService(Map<String, ColumnFamilyConfig> configuredColumnFamilies) {
        this.configuredColumnFamilies = configuredColumnFamilies;
    }

    private static Map<String, ColumnFamilyDescriptor> getDescriptors(List<ColumnFamilyDescriptor> columnFamilyDescriptors) {
        return columnFamilyDescriptors.stream()
                .collect(Collectors.toMap(
                        descriptor -> TypeConvert.unpackString(descriptor.getName()),
                        Function.identity(), (columnFamilyDescriptor, columnFamilyDescriptor2) -> columnFamilyDescriptor));
    }

    public void applySettings(List<ColumnFamilyDescriptor> columnFamilyDescriptors) {
        if (configuredColumnFamilies.isEmpty()) {
            return;
        }
        final Map<String, ColumnFamilyDescriptor> descriptorMap = getDescriptors(columnFamilyDescriptors);
        for (Map.Entry<String, ColumnFamilyConfig> columnFamilyConfigEntry : configuredColumnFamilies.entrySet()) {
            final String key = columnFamilyConfigEntry.getKey();
            Objects.requireNonNull(key, "column name pattern cannot be null");
            final Pattern pattern = Pattern.compile(key);
            final ColumnFamilyConfig columnFamilyConfig = columnFamilyConfigEntry.getValue();
            descriptorMap.entrySet().stream()
                    .filter(entry -> pattern.matcher(entry.getKey()).find())
                    .map(entry -> entry.getValue())
                    .map(columnFamilyDescriptor -> columnFamilyDescriptor.getOptions())
                    .forEach(columnFamilyOptions -> ColumnFamilyConfigMapper.setRocksDbOpt(columnFamilyConfig, columnFamilyOptions));
        }
    }
}