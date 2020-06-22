package cn.t0mpi9.snowflake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author twitter
 */
public class SnowflakeIdGenerate {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeIdGenerate.class);

    private final long workerId;
    private final long dataCenterId;

    private static final long twepoch = 1288834974657L;
    private static final long workerIdBits = 5L;
    private static final long dataCenterIdBits = 5L;
    private static final long maxWorkerId = ~(-1L << workerIdBits);
    private static final long maxDataCenterId = ~(-1L << dataCenterIdBits);
    private static final long sequenceBits = 12L;

    private static final long workerIdShift = sequenceBits;
    private static final long dataCenterIdShift = sequenceBits + workerIdBits;
    private static final long timestampLeftShift = sequenceBits + workerIdBits + dataCenterIdBits;
    private static final long sequenceMask = ~(-1L << sequenceBits);

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * @param workerId
     * @param dataCenterId
     */
    public SnowflakeIdGenerate(long workerId, long dataCenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("workerId不能大于%d或小于0", maxWorkerId));
        }
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            throw new IllegalArgumentException(String.format("dataCenterId不能大于%d或小于0", maxDataCenterId));
        }
        if (twepoch > timeGen()) {
            throw new IllegalArgumentException("twepoch不能大于当前时间");
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
        LOGGER.info("SnowflakeIdGenerate初始化, dataCenterId={}, workerId={}", dataCenterId, workerId);
    }

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            try {
                long moveTime = lastTimestamp - timestamp;
                long waitTime = moveTime << 1;
                LOGGER.error("系统时钟后移{}ms, lastTimestamp={}, timestamp={}, l, 线程将wait({})",
                        moveTime, lastTimestamp, timestamp, waitTime);
                wait(waitTime);
                timestamp = timeGen();
                if (timestamp < lastTimestamp) {
                    throw new RuntimeException(String.format("时钟出现后移, %dms内拒绝生成ID ", lastTimestamp - timestamp));
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
        return ((timestamp - twepoch) << timestampLeftShift) | (dataCenterId << dataCenterIdShift) | (workerId << workerIdShift) | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }


    public static SnowflakeId parseId(long snowflakeId) {
        SnowflakeId s = new SnowflakeId();
        s.timeStamp = (snowflakeId >> timestampLeftShift) + twepoch;
        s.dataCenterId = (snowflakeId >> dataCenterIdShift) & maxDataCenterId;
        s.workerId = (snowflakeId >> sequenceBits) & maxWorkerId;
        s.sequence = snowflakeId & sequenceMask;
        return s;
    }

    public static class SnowflakeId {
        private long timeStamp;
        private long workerId;
        private long dataCenterId;
        private long sequence;

        public long getTimeStamp() {
            return timeStamp;
        }

        public long getWorkerId() {
            return workerId;
        }

        public long getDataCenterId() {
            return dataCenterId;
        }

        public long getSequence() {
            return sequence;
        }

        @Override
        public String toString() {
            return "SnowflakeId{"
                    + "timeStamp=" + timeStamp
                    + ", workerId=" + workerId
                    + ", dataCenterId=" + dataCenterId
                    + ", sequence=" + sequence
                    + '}';
        }
    }
}
