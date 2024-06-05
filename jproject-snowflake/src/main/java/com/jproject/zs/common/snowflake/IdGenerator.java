package com.jproject.zs.common.snowflake;

import lombok.extern.slf4j.Slf4j;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/12/23
 */
@Slf4j
public class IdGenerator {

    private final long twepoch = 1288834974657L;

    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;

    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long workerIdBits;
    private long datacenterIdBits;
    private long maxWorkerId;
    private long maxDatacenterId;
    private long datacenterIdShift;
    private long timestampLeftShift;

    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public IdGenerator(long workerId, long datacenterId, long workerIdBits, long datacenterIdBits) {

        this.workerIdBits = workerIdBits;
        this.datacenterIdBits = datacenterIdBits;
        this.maxWorkerId = -1L ^ (-1L << workerIdBits);
        this.maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
        this.datacenterIdShift = sequenceBits + workerIdBits;
        this.timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }


    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            log.error(String.format(
                    "Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
			/*throw new RuntimeException(String.format(
					"Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));*/
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift) | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }


}
