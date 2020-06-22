package cn.t0mpi9.springboot.snowflake.autoconfigure;

import cn.t0mpi9.snowflake.SnowflakeIdGenerate;
import cn.t0mpi9.snowflake.builder.RedisConfigBuilder;
import cn.t0mpi9.snowflake.builder.SnowflakeIdGenerateBuilder;
import cn.t0mpi9.snowflake.builder.ZookeeperConfigBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.retry.RetryOneTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * <br/>
 * Created on 2020/6/19 14:11.
 *
 * @author zhubenle
 */
@Configuration
@EnableConfigurationProperties(value = {SnowflakeIdGenerateProperties.class})
public class SnowflakeIdGenerateAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeIdGenerateAutoConfiguration.class);

    @Value("${server.port}")
    private Integer serverPort;
    @Value("${spring.application.name}")
    private String springApplicationName;

    @ConditionalOnProperty(name = {"snowflake.redis-config.enable", "snowflake.zk-config.enable"}, havingValue = "false")
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateDirect(SnowflakeIdGenerateProperties properties) {
        return SnowflakeIdGenerateBuilder.create()
                .useDirect()
                .workerId(properties.getCustom().getWorkerId())
                .dataCenterId(properties.getCustom().getDataCenterId())
                .build();
    }

    @ConditionalOnProperty(value = {"snowflake.zk-config.enable"}, havingValue = "true")
    @ConditionalOnMissingBean(value = {CuratorFramework.class})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateZk(SnowflakeIdGenerateProperties properties) {

        ZookeeperConfigBuilder zookeeperConfigBuilder = SnowflakeIdGenerateBuilder.create()
                .useZookeeper(properties.getZkConfig().getConnection(), new RetryOneTime(10000)
                        , properties.getZkConfig().getConnectionTimeoutMillis(), properties.getZkConfig().getSessionTimeoutMillis());

        return zookeeperConfigBuilder.ip(getCurrentServerIp(properties))
                .port(getCurrentServerPort(properties))
                .applicationName(getApplicationName(properties))
                .localFileCache(properties.getZkConfig().isLocalFileCache())
                .fileCachePath(properties.getZkConfig().getFileCachePath())
                .build();
    }

    @ConditionalOnProperty(value = {"snowflake.zk-config.enable"}, havingValue = "true")
    @ConditionalOnBean(value = {CuratorFramework.class})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateUseCuratorFramework(SnowflakeIdGenerateProperties properties, CuratorFramework client) {

        ZookeeperConfigBuilder zookeeperConfigBuilder = SnowflakeIdGenerateBuilder.create()
                .useZookeeper(client);

        return zookeeperConfigBuilder.ip(getCurrentServerIp(properties))
                .port(getCurrentServerPort(properties))
                .applicationName(getApplicationName(properties))
                .localFileCache(properties.getZkConfig().isLocalFileCache())
                .fileCachePath(properties.getZkConfig().getFileCachePath())
                .build();
    }

    @ConditionalOnProperty(value = {"snowflake.redis-config.enable"}, havingValue = "true")
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @ConditionalOnClass(name = {"io.lettuce.core.RedisClient"})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateLettuce(SnowflakeIdGenerateProperties properties) {
        RedisConfigBuilder redisConfigBuilder = SnowflakeIdGenerateBuilder.create()
                .useLettuceRedis(properties.getRedisConfig().getHost(), properties.getRedisConfig().getPort(),
                        properties.getRedisConfig().getPassword(), properties.getRedisConfig().getDatabase());

        return redisConfigBuilder.ip(getCurrentServerIp(properties))
                .port(getCurrentServerPort(properties))
                .applicationName(getApplicationName(properties))
                .localFileCache(properties.getRedisConfig().isLocalFileCache())
                .fileCachePath(properties.getRedisConfig().getFileCachePath())
                .build();
    }

    @ConditionalOnProperty(value = {"snowflake.redis-config.enable"}, havingValue = "true")
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @ConditionalOnClass(name = {"redis.clients.jedis.Jedis"})
    @ConditionalOnMissingClass(value = {"io.lettuce.core.RedisClient"})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateJedis(SnowflakeIdGenerateProperties properties) {
        RedisConfigBuilder redisConfigBuilder = SnowflakeIdGenerateBuilder.create()
                .useLettuceRedis(properties.getRedisConfig().getHost(), properties.getRedisConfig().getPort(),
                        properties.getRedisConfig().getPassword(), properties.getRedisConfig().getDatabase());

        return redisConfigBuilder.ip(getCurrentServerIp(properties))
                .port(getCurrentServerPort(properties))
                .applicationName(getApplicationName(properties))
                .localFileCache(properties.getRedisConfig().isLocalFileCache())
                .fileCachePath(properties.getRedisConfig().getFileCachePath())
                .build();
    }

    private String getCurrentServerIp(SnowflakeIdGenerateProperties properties) {
        String ip = properties.getZkConfig().getCurrentServerIp();
        if (ip == null) {
            ip = getLocalAddress().getHostAddress();
        }
        return ip;
    }

    private int getCurrentServerPort(SnowflakeIdGenerateProperties properties) {
        int port = properties.getZkConfig().getCurrentServerPort();
        if (port == 0) {
            port = serverPort;
        }
        return port;
    }

    private String getApplicationName(SnowflakeIdGenerateProperties properties) {
        String applicationName = properties.getZkConfig().getApplicationName();
        if (applicationName == null) {
            applicationName = springApplicationName;
        }
        return applicationName;
    }

    public static InetAddress getLocalAddress() {
        InetAddress candidateAddress = null;
        try {
            // 遍历所有的网络接口
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                // 在所有的接口下再遍历IP
                Enumeration<InetAddress> inetAddrs = iface.getInetAddresses();
                while (inetAddrs.hasMoreElements()) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        // 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            candidateAddress = InetAddress.getLocalHost();
        } catch (Exception e) {
            LOGGER.error("获取网卡IP异常: {}", e.getMessage());
        }
        return candidateAddress;
    }
}
