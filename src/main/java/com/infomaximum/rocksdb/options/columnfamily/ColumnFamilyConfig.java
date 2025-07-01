package com.infomaximum.rocksdb.options.columnfamily;

import org.rocksdb.CompactionPriority;

import java.io.Serializable;
import java.util.Objects;

public class ColumnFamilyConfig implements Serializable {

    final Long writeBufferSize;
    final Integer maxWriteBufferNumber;
    final Integer minWriteBufferNumberToMerge;
    final Integer numLevels;
    final Long targetFileSizeBase;
    final Long maxBytesForLevelBase;
    final Boolean enableLevelCompactionDynamicLevelBytes;
    final Long maxCompactionBytes;
    final Long arenaBlockSize;
    final Boolean disableAutoCompactions;
    final Long maxSequentialSkipInIterations;
    final Double memtablePrefixBloomSizeRatio;
    final Long maxSuccessiveMerges;
    final Long softPendingCompactionBytesLimit;
    final Integer level0FileNumCompactionTrigger;
    final Integer level0SlowdownWritesTrigger;
    final Integer level0StopWritesTrigger;
    final Integer maxWriteBufferNumberToMaintain;
    final Integer targetFileSizeMultiplier;
    final Double maxBytesForLevelMultiplier;
    final CompactionPriority compactionPriority;

    private ColumnFamilyConfig(Builder builder) {
        writeBufferSize = builder.writeBufferSize;
        maxWriteBufferNumber = builder.maxWriteBufferNumber;
        minWriteBufferNumberToMerge = builder.minWriteBufferNumberToMerge;
        numLevels = builder.numLevels;
        targetFileSizeBase = builder.targetFileSizeBase;
        maxBytesForLevelBase = builder.maxBytesForLevelBase;
        enableLevelCompactionDynamicLevelBytes = builder.enableLevelCompactionDynamicLevelBytes;
        maxCompactionBytes = builder.maxCompactionBytes;
        arenaBlockSize = builder.arenaBlockSize;
        disableAutoCompactions = builder.disableAutoCompactions;
        maxSequentialSkipInIterations = builder.maxSequentialSkipInIterations;
        memtablePrefixBloomSizeRatio = builder.memtablePrefixBloomSizeRatio;
        maxSuccessiveMerges = builder.maxSuccessiveMerges;
        softPendingCompactionBytesLimit = builder.softPendingCompactionBytesLimit;
        level0FileNumCompactionTrigger = builder.level0FileNumCompactionTrigger;
        level0SlowdownWritesTrigger = builder.level0SlowdownWritesTrigger;
        level0StopWritesTrigger = builder.level0StopWritesTrigger;
        maxWriteBufferNumberToMaintain = builder.maxWriteBufferNumberToMaintain;
        targetFileSizeMultiplier = builder.targetFileSizeMultiplier;
        maxBytesForLevelMultiplier = builder.maxBytesForLevelMultiplier;
        compactionPriority = builder.compactionPriority;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getWriteBufferSize() {
        return writeBufferSize;
    }

    public Integer getMaxWriteBufferNumber() {
        return maxWriteBufferNumber;
    }

    public Integer getMinWriteBufferNumberToMerge() {
        return minWriteBufferNumberToMerge;
    }

    public Integer getNumLevels() {
        return numLevels;
    }

    public Long getTargetFileSizeBase() {
        return targetFileSizeBase;
    }

    public Long getMaxBytesForLevelBase() {
        return maxBytesForLevelBase;
    }

    public Boolean getEnableLevelCompactionDynamicLevelBytes() {
        return enableLevelCompactionDynamicLevelBytes;
    }

    public Long getMaxCompactionBytes() {
        return maxCompactionBytes;
    }

    public Long getArenaBlockSize() {
        return arenaBlockSize;
    }

    public Boolean getDisableAutoCompactions() {
        return disableAutoCompactions;
    }

    public Long getMaxSequentialSkipInIterations() {
        return maxSequentialSkipInIterations;
    }

    public Double getMemtablePrefixBloomSizeRatio() {
        return memtablePrefixBloomSizeRatio;
    }

    public Long getMaxSuccessiveMerges() {
        return maxSuccessiveMerges;
    }

    public Long getSoftPendingCompactionBytesLimit() {
        return softPendingCompactionBytesLimit;
    }

    public Integer getLevel0FileNumCompactionTrigger() {
        return level0FileNumCompactionTrigger;
    }

    public Integer getLevel0SlowdownWritesTrigger() {
        return level0SlowdownWritesTrigger;
    }

    public Integer getLevel0StopWritesTrigger() {
        return level0StopWritesTrigger;
    }

    public Integer getMaxWriteBufferNumberToMaintain() {
        return maxWriteBufferNumberToMaintain;
    }

    public Integer getTargetFileSizeMultiplier() {
        return targetFileSizeMultiplier;
    }

    public Double getMaxBytesForLevelMultiplier() {
        return maxBytesForLevelMultiplier;
    }

    public CompactionPriority getCompactionPriority() {
        return compactionPriority;
    }


    public boolean isContainWriteBufferSize() {
        return Objects.nonNull(writeBufferSize);
    }

    public boolean isContainMaxWriteBufferNumber() {
        return Objects.nonNull(maxWriteBufferNumber);
    }

    public boolean isContainMinWriteBufferNumberToMerge() {
        return Objects.nonNull(minWriteBufferNumberToMerge);
    }

    public boolean isContainNumLevels() {
        return Objects.nonNull(numLevels);
    }

    public boolean isContainTargetFileSizeBase() {
        return Objects.nonNull(targetFileSizeBase);
    }

    public boolean isContainMaxBytesForLevelBase() {
        return Objects.nonNull(maxBytesForLevelBase);
    }

    public boolean isContainEnableLevelCompactionDynamicLevelBytes() {
        return Objects.nonNull(enableLevelCompactionDynamicLevelBytes);
    }

    public boolean isContainMaxCompactionBytes() {
        return Objects.nonNull(maxCompactionBytes);
    }

    public boolean isContainArenaBlockSize() {
        return Objects.nonNull(arenaBlockSize);
    }

    public boolean isContainDisableAutoCompactions() {
        return Objects.nonNull(disableAutoCompactions);
    }

    public boolean isContainMaxSequentialSkipInIterations() {
        return Objects.nonNull(maxSequentialSkipInIterations);
    }

    public boolean isContainMemtablePrefixBloomSizeRatio() {
        return Objects.nonNull(memtablePrefixBloomSizeRatio);
    }

    public boolean isContainMaxSuccessiveMerges() {
        return Objects.nonNull(maxSuccessiveMerges);
    }

    public boolean isContainSoftPendingCompactionBytesLimit() {
        return Objects.nonNull(softPendingCompactionBytesLimit);
    }

    public boolean isContainLevel0FileNumCompactionTrigger() {
        return Objects.nonNull(level0FileNumCompactionTrigger);
    }

    public boolean isContainLevel0SlowdownWritesTrigger() {
        return Objects.nonNull(level0SlowdownWritesTrigger);
    }

    public boolean isContainLevel0StopWritesTrigger() {
        return Objects.nonNull(level0StopWritesTrigger);
    }

    public boolean isContainMaxWriteBufferNumberToMaintain() {
        return Objects.nonNull(maxWriteBufferNumberToMaintain);
    }

    public boolean isContainTargetFileSizeMultiplier() {
        return Objects.nonNull(targetFileSizeMultiplier);
    }

    public boolean isContainMaxBytesForLevelMultiplier() {
        return Objects.nonNull(maxBytesForLevelMultiplier);
    }

    public boolean isContainCompactionPriority() {
        return Objects.nonNull(compactionPriority);
    }


    public static final class Builder {
        private Long writeBufferSize;
        private Integer maxWriteBufferNumber;
        private Integer minWriteBufferNumberToMerge;
        private Integer numLevels;
        private Long targetFileSizeBase;
        private Long maxBytesForLevelBase;
        private Boolean enableLevelCompactionDynamicLevelBytes;
        private Long maxCompactionBytes;
        private Long arenaBlockSize;
        private Boolean disableAutoCompactions;
        private Long maxSequentialSkipInIterations;
        private Double memtablePrefixBloomSizeRatio;
        private Long maxSuccessiveMerges;
        private Long softPendingCompactionBytesLimit;
        private Integer level0FileNumCompactionTrigger;
        private Integer level0SlowdownWritesTrigger;
        private Integer level0StopWritesTrigger;
        private Integer maxWriteBufferNumberToMaintain;
        private Integer targetFileSizeMultiplier;
        private Double maxBytesForLevelMultiplier;
        private CompactionPriority compactionPriority;

        private Builder() {
        }

        public Builder withWriteBufferSize(Long writeBufferSize) {
            this.writeBufferSize = writeBufferSize;
            return this;
        }

        public Builder withMaxWriteBufferNumber(Integer maxWriteBufferNumber) {
            this.maxWriteBufferNumber = maxWriteBufferNumber;
            return this;
        }

        public Builder withMinWriteBufferNumberToMerge(Integer minWriteBufferNumberToMerge) {
            this.minWriteBufferNumberToMerge = minWriteBufferNumberToMerge;
            return this;
        }

        public Builder withNumLevels(Integer numLevels) {
            this.numLevels = numLevels;
            return this;
        }

        public Builder withTargetFileSizeBase(Long targetFileSizeBase) {
            this.targetFileSizeBase = targetFileSizeBase;
            return this;
        }

        public Builder withMaxBytesForLevelBase(Long maxBytesForLevelBase) {
            this.maxBytesForLevelBase = maxBytesForLevelBase;
            return this;
        }

        public Builder withEnableLevelCompactionDynamicLevelBytes(Boolean enableLevelCompactionDynamicLevelBytes) {
            this.enableLevelCompactionDynamicLevelBytes = enableLevelCompactionDynamicLevelBytes;
            return this;
        }

        public Builder withMaxCompactionBytes(Long maxCompactionBytes) {
            this.maxCompactionBytes = maxCompactionBytes;
            return this;
        }

        public Builder withArenaBlockSize(Long arenaBlockSize) {
            this.arenaBlockSize = arenaBlockSize;
            return this;
        }

        public Builder withDisableAutoCompactions(Boolean disableAutoCompactions) {
            this.disableAutoCompactions = disableAutoCompactions;
            return this;
        }

        public Builder withMaxSequentialSkipInIterations(Long maxSequentialSkipInIterations) {
            this.maxSequentialSkipInIterations = maxSequentialSkipInIterations;
            return this;
        }

        public Builder withMemtablePrefixBloomSizeRatio(Double memtablePrefixBloomSizeRatio) {
            this.memtablePrefixBloomSizeRatio = memtablePrefixBloomSizeRatio;
            return this;
        }

        public Builder withMaxSuccessiveMerges(Long maxSuccessiveMerges) {
            this.maxSuccessiveMerges = maxSuccessiveMerges;
            return this;
        }

        public Builder withSoftPendingCompactionBytesLimit(Long softPendingCompactionBytesLimit) {
            this.softPendingCompactionBytesLimit = softPendingCompactionBytesLimit;
            return this;
        }

        public Builder withLevel0FileNumCompactionTrigger(Integer level0FileNumCompactionTrigger) {
            this.level0FileNumCompactionTrigger = level0FileNumCompactionTrigger;
            return this;
        }

        public Builder withLevel0SlowdownWritesTrigger(Integer level0SlowdownWritesTrigger) {
            this.level0SlowdownWritesTrigger = level0SlowdownWritesTrigger;
            return this;
        }

        public Builder withLevel0StopWritesTrigger(Integer level0StopWritesTrigger) {
            this.level0StopWritesTrigger = level0StopWritesTrigger;
            return this;
        }

        public Builder withMaxWriteBufferNumberToMaintain(Integer maxWriteBufferNumberToMaintain) {
            this.maxWriteBufferNumberToMaintain = maxWriteBufferNumberToMaintain;
            return this;
        }

        public Builder withTargetFileSizeMultiplier(Integer targetFileSizeMultiplier) {
            this.targetFileSizeMultiplier = targetFileSizeMultiplier;
            return this;
        }

        public Builder withMaxBytesForLevelMultiplier(Double maxBytesForLevelMultiplier) {
            this.maxBytesForLevelMultiplier = maxBytesForLevelMultiplier;
            return this;
        }

        public Builder withCompactionPriority(CompactionPriority compactionPriority) {
            this.compactionPriority = compactionPriority;
            return this;
        }

        public ColumnFamilyConfig build() {
            return new ColumnFamilyConfig(this);
        }
    }
}