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
import org.springframework.context.annotation.Primary;
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

    /**
     * direct-ip, redis-config, zk-config都未开启时，用配置的workerId和dataCenterId
     */
    @ConditionalOnProperty(prefix = "snowflake", name = {"direct-ip.enable", "redis-config.enable", "zk-config.enable"},
            havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @Primary
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateDirect(SnowflakeIdGenerateProperties properties) {
        return snowflakeIdGenerateBuilder.useDirect()
                .workerId(properties.getDirect().getWorkerId())
                .dataCenterId(properties.getDirect().getDataCenterId())
                .build();
    }

    /**
     * direct-ip开启时，用配置的ip创建，没有配置就获取本机ip
     */
    @ConditionalOnProperty(prefix = "snowflake", name = {"direct-ip.enable"}, havingValue = "true")
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateDirectIp(SnowflakeIdGenerateProperties properties) {
        String currentServerIp = properties.getDirectIp().getCurrentServerIp();
        if (currentServerIp == null) {
            currentServerIp = getLocalAddress().getHostAddress();
        }
        return snowflakeIdGenerateBuilder.useDirectIp()
                .currentServerIp(currentServerIp)
                .build();
    }

    /**
     * zk-config开启，用zk配置创建
     */
    @ConditionalOnProperty(prefix = "snowflake", name = {"zk-config.enable"}, havingValue = "true")
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateZk(SnowflakeIdGenerateProperties properties) {
        ZookeeperConfigBuilder zookeeperConfigBuilder = snowflakeIdGenerateBuilder
                .useZookeeper(properties.getZkConfig().getConnection(), null
                        , properties.getZkConfig().getConnectionTimeoutMillis(), properties.getZkConfig().getSessionTimeoutMillis());

        return doConfigBuild(zookeeperConfigBuilder, properties.getZkConfig());
    }

    /**
     * redis-config开启，如果引入了lettuce的redis客户端，用该客户端创建连接
     */
    @ConditionalOnProperty(prefix = "snowflake", name = {"redis-config.enable"}, havingValue = "true")
    @ConditionalOnClass(name = {"io.lettuce.core.RedisClient"})
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateLettuce(SnowflakeIdGenerateProperties properties) {
        RedisConfigBuilder redisConfigBuilder = snowflakeIdGenerateBuilder
                .useLettuceRedis(properties.getRedisConfig().getHost(), properties.getRedisConfig().getPort(),
                        properties.getRedisConfig().getPassword(), properties.getRedisConfig().getDatabase());

        return doConfigBuild(redisConfigBuilder, properties.getRedisConfig());
    }

    /**
     * redis-config开启，如果引入了jedis的redis客户端，用该客户端创建连接
     */
    @ConditionalOnProperty(prefix = "snowflake", name = {"redis-config.enable"}, havingValue = "true")
    @ConditionalOnClass(name = {"redis.clients.jedis.Jedis"})
    @ConditionalOnMissingClass(value = {"io.lettuce.core.RedisClient"})
    @ConditionalOnMissingBean(value = {SnowflakeIdGenerate.class})
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerateJedis(SnowflakeIdGenerateProperties properties) {
        RedisConfigBuilder redisConfigBuilder = snowflakeIdGenerateBuilder
                .useJedisRedis(properties.getRedisConfig().getHost(), properties.getRedisConfig().getPort(),
                        properties.getRedisConfig().getPassword(), properties.getRedisConfig().getDatabase());

        return doConfigBuild(redisConfigBuilder, properties.getRedisConfig());
    }

    /**
     * 创建
     *
     * @param configBuilder 建造对象抽象父类
     * @param zkAndRedis    配置抽象父类
     * @return SnowflakeIdGenerate
     */
    private SnowflakeIdGenerate doConfigBuild(AbstractConfigBuilder configBuilder, SnowflakeIdGenerateProperties.BaseZkAndRedis zkAndRedis) {
        return configBuilder.ip(getIpOrDefault(zkAndRedis))
                .port(getPortOrDefault(zkAndRedis))
                .applicationName(getApplicationNameOrDefault(zkAndRedis))
                .localFileCache(zkAndRedis.isLocalFileCache())
                .fileCachePath(zkAndRedis.getFileCachePath())
                .build();
    }

    /**
     * 获取配置的当前服务ip，没有的话获取本机ip
     *
     * @param zkAndRedis 配置
     * @return ip
     */
    private String getIpOrDefault(SnowflakeIdGenerateProperties.BaseZkAndRedis zkAndRedis) {
        if (zkAndRedis.getCurrentServerIp() == null) {
            return getLocalAddress().getHostAddress();
        }
        return zkAndRedis.getCurrentServerIp();
    }

    /**
     * 获取配置的当前服务端口，没有的话获取spring-boot的server.port属性
     *
     * @param zkAndRedis 配置
     * @return 端口
     */
    private Integer getPortOrDefault(SnowflakeIdGenerateProperties.BaseZkAndRedis zkAndRedis) {
        if (Objects.isNull(zkAndRedis.getCurrentServerIp()) || zkAndRedis.getCurrentServerPort() == 0) {
            return serverPort;
        }
        return zkAndRedis.getCurrentServerPort();
    }

    /**
     * 获取配置的当前服务应用名称，没有的话默认取spring-boot的spring.application.name配置
     *
     * @param zkAndRedis 配置
     * @return 端口
     */
    private String getApplicationNameOrDefault(SnowflakeIdGenerateProperties.BaseZkAndRedis zkAndRedis) {
        if (zkAndRedis.getApplicationName() == null) {
            return springApplicationName;
        }
        return zkAndRedis.getApplicationName();
    }

    /**
     * 获取本机ip地址
     * @return
     */
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
