package cn.t0mpi9.springboot.snowflake.autoconfigure;

import cn.t0mpi9.snowflake.SnowflakeIdGenerate;
import cn.t0mpi9.snowflake.builder.AbstractConfigBuilder;
import cn.t0mpi9.snowflake.builder.RedisConfigBuilder;
import cn.t0mpi9.snowflake.builder.SnowflakeIdGenerateBuilder;
import cn.t0mpi9.snowflake.builder.ZookeeperConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Objects;

/**
 * <br/>
 * Created on 2020/6/19 14:11.
 *
 * @author zhubenle
 */
@Configuration
@EnableConfigurationProperties(value = {SnowflakeIdGenerateProperties.class})
public class SnowflakeIdGenerateAutoConfiguration implements EnvironmentAware, InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeIdGenerateAutoConfiguration.class);

    private Integer serverPort;
    private String springApplicationName;
    private SnowflakeIdGenerateBuilder snowflakeIdGenerateBuilder;


    @ConditionalOnProperty(prefix = "snowflake", name = {"redis-config.enable", "zk-config.enable"},
            havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateDirect(SnowflakeIdGenerateProperties properties) {
        return snowflakeIdGenerateBuilder.useDirect()
                .workerId(properties.getDirect().getWorkerId())
                .dataCenterId(properties.getDirect().getDataCenterId())
                .build();
    }

    @ConditionalOnProperty(prefix = "snowflake", name = {"zk-config.enable"}, havingValue = "true")
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateZk(SnowflakeIdGenerateProperties properties) {
        ZookeeperConfigBuilder zookeeperConfigBuilder = snowflakeIdGenerateBuilder
                .useZookeeper(properties.getZkConfig().getConnection(), null
                        , properties.getZkConfig().getConnectionTimeoutMillis(), properties.getZkConfig().getSessionTimeoutMillis());

        return doNotDirectBuild(zookeeperConfigBuilder, properties);
    }

    @ConditionalOnProperty(prefix = "snowflake", name = {"redis-config.enable"}, havingValue = "true")
    @ConditionalOnClass(name = {"io.lettuce.core.RedisClient"})
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateLettuce(SnowflakeIdGenerateProperties properties) {
        RedisConfigBuilder redisConfigBuilder = snowflakeIdGenerateBuilder
                .useLettuceRedis(properties.getRedisConfig().getHost(), properties.getRedisConfig().getPort(),
                        properties.getRedisConfig().getPassword(), properties.getRedisConfig().getDatabase());

        return doNotDirectBuild(redisConfigBuilder, properties);
    }

    @ConditionalOnProperty(prefix = "snowflake", name = {"redis-config.enable"}, havingValue = "true")
    @ConditionalOnClass(name = {"redis.clients.jedis.Jedis"})
    @ConditionalOnMissingClass(value = {"io.lettuce.core.RedisClient"})
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateJedis(SnowflakeIdGenerateProperties properties) {
        RedisConfigBuilder redisConfigBuilder = snowflakeIdGenerateBuilder
                .useJedisRedis(properties.getRedisConfig().getHost(), properties.getRedisConfig().getPort(),
                        properties.getRedisConfig().getPassword(), properties.getRedisConfig().getDatabase());

        return doNotDirectBuild(redisConfigBuilder, properties);
    }


    private SnowflakeIdGenerate doNotDirectBuild(AbstractConfigBuilder configBuilder, SnowflakeIdGenerateProperties properties) {
        return configBuilder.ip(getCurrentServerIp(properties))
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

    private Integer getCurrentServerPort(SnowflakeIdGenerateProperties properties) {
        Integer port = properties.getZkConfig().getCurrentServerPort();
        if (Objects.isNull(port) || port == 0) {
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

    @Override
    public void setEnvironment(Environment environment) {
        serverPort = environment.getProperty("server.port", Integer.class, 8080);
        springApplicationName = environment.getProperty("spring.application.name", String.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        snowflakeIdGenerateBuilder = SnowflakeIdGenerateBuilder.create();
    }

    @Override
    public void destroy() throws Exception {
        if (Objects.nonNull(snowflakeIdGenerateBuilder)) {
            snowflakeIdGenerateBuilder.close();
        }
    }
}
