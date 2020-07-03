package cn.t0mpi9.snowflake.builder;

import cn.t0mpi9.snowflake.Constant;
import cn.t0mpi9.snowflake.SnowflakeIdGenerate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * <br/>
 * Created on 2020/6/4 13:46.
 *
 * @author zhnle
 */
public class RedisConfigBuilder extends AbstractConfigBuilder<RedisConfigBuilder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfigBuilder.class);

    private static final String PERSISTENT_TIME_NAME = Constant.PERSISTENT_NAME + "-time";

    private RedisConn connection;

    RedisConfigBuilder() {
    }

    RedisConfigBuilder(RedisConn connection) {
        this.connection = connection;
    }

    @Override
    public void close() throws IOException {
        super.close();
        connection.close();
        LOGGER.info("snowflake redis connection closed");
    }

    private String getPersistentTimeKey() {
        return Constant.ROOT_NAME + Constant.COLON + applicationName + Constant.COLON + PERSISTENT_TIME_NAME;
    }

    private String getPersistentKey() {
        return Constant.ROOT_NAME + Constant.COLON + applicationName + Constant.COLON + Constant.PERSISTENT_NAME;
    }

    private String getEphemeralKey() {
        return Constant.ROOT_NAME + Constant.COLON + applicationName + Constant.COLON + Constant.EPHEMERAL_NAME;
    }

    @Override
    protected SnowflakeIdGenerate doBuild() {
        String ipPort = ip + Constant.COLON + port;
        long workerId;
        long dataCenterId;

        try {
            String persistentTimeKey = getPersistentTimeKey();
            String persistentKey = getPersistentKey();
            String time = connection.hget(persistentTimeKey, ipPort);
            if (time != null && System.currentTimeMillis() < Long.parseLong(time)) {
                throw new IllegalStateException("检查当前系统时间是否比上一次上传到redis时间小, 请确认系统时间");
            }
            String sequentialStr = connection.eval(Constant.REDIS_ADD_PERSISTENT_SCRIPT,
                    new String[]{persistentKey, persistentTimeKey, ipPort}, String.valueOf(System.currentTimeMillis()));
            long sequential = Integer.parseInt(sequentialStr);
            workerId = sequential & Constant.BIT;
            dataCenterId = sequential >> 5 & Constant.BIT;

            saveLocalFile(workerId, dataCenterId);

            scheduledUpdateEphemeral(ipPort);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            SnowflakeIdGenerate snowflakeIdGenerate;
            if (localFileCache && (snowflakeIdGenerate = useLocalCreate()) != null) {
                return snowflakeIdGenerate;
            }
            throw new RuntimeException("创建SnowflakeIdGenerate, redis加载dataCenterId和workerId失败", e);
        }
        return new SnowflakeIdGenerate(workerId, dataCenterId);
    }

    private void scheduledUpdateEphemeral(String ipPort) {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            String persistentTimeKey = getPersistentTimeKey();
            try {
                String ephemeralKey = getEphemeralKey() + Constant.COLON + ipPort;
                long expire = scheduleTimeUnit.convert(schedulePeriod, TimeUnit.SECONDS) + 10;
                String value = String.valueOf(System.currentTimeMillis());
                connection.eval(Constant.REDIS_UPDATE_EPHEMERAL_SCRIPT, new String[]{ephemeralKey, persistentTimeKey, ipPort},
                        value, String.valueOf(expire));
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("定时更新redis的{}的key={}的值为{}", persistentTimeKey, ipPort, value);
                }
            } catch (Exception e) {
                LOGGER.error("更新redis的{}的key={}异常: {}", persistentTimeKey, ipPort, e.getMessage());
            }
        }, 0, schedulePeriod, scheduleTimeUnit);
    }
}
