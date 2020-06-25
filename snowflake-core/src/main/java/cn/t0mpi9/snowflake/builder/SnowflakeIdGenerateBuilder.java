package cn.t0mpi9.snowflake.builder;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * <br/>
 * Created on 2020/6/4 10:50.
 *
 * @author zhubenle
 */
public class SnowflakeIdGenerateBuilder implements Closeable {

    private ConfigBuilder configBuilder;

    private SnowflakeIdGenerateBuilder() {
    }

    public static SnowflakeIdGenerateBuilder create() {
        return new SnowflakeIdGenerateBuilder();
    }

    /**
     * 自定义workId和dataCenterId
     *
     * @return DirectConfigBuilder
     */
    public DirectConfigBuilder useDirect() {
        return new DirectConfigBuilder();
    }

    public <T extends ConfigBuilder> T use(T t) {
        configBuilder = t;
        return t;
    }

    /**
     * 传入redis服务配置信息，创建使用redisClient连接
     *
     * @param redisHost redis的ip
     * @param redisPort redis的端口
     * @param password  密码
     * @param database  数据库索引
     * @return RedisConfigBuilder
     */
    public RedisConfigBuilder useLettuceRedis(String redisHost, int redisPort, String password, int database) {
        RedisURI redisURI = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort)
                .withPassword(Objects.isNull(password) ? "" : password)
                .withDatabase(database)
                .build();
        RedisClient redisClient = RedisClient.create(redisURI);
        return useLettuceRedis(redisClient);
    }

    /**
     * 使用redisClient
     *
     * @param redisClient RedisClient对象
     * @return RedisConfigBuilder
     */
    public RedisConfigBuilder useLettuceRedis(RedisClient redisClient) {
        Objects.requireNonNull(redisClient);
        configBuilder = new RedisConfigBuilder(new RedisLettuceConn(redisClient.connect()));
        return (RedisConfigBuilder) configBuilder;
    }

    /**
     * 使用redisClusterClient
     *
     * @param redisClusterClient RedisClusterClient对象
     * @return RedisConfigBuilder
     */
    public RedisConfigBuilder useLettuceRedis(RedisClusterClient redisClusterClient) {
        Objects.requireNonNull(redisClusterClient);
        configBuilder = new RedisConfigBuilder(new RedisLettuceClusterConn(redisClusterClient.connect()));
        return (RedisConfigBuilder) configBuilder;
    }

    /**
     * 传入redis服务配置信息，创建使用Jedis连接
     *
     * @param redisHost redis的ip
     * @param redisPort redis的端口
     * @param password  密码
     * @param database  数据库索引
     * @return RedisConfigBuilder
     */
    public RedisConfigBuilder useJedisRedis(String redisHost, int redisPort, String password, int database) {
        Jedis jedis = new Jedis(redisHost, redisPort);
        if (password != null && !"".equals(password)) {
            jedis.auth(password);
        }
        jedis.select(database);
        return useJedisRedis(jedis);
    }

    /**
     * 使用Jedis
     *
     * @param jedis Jedis对象
     * @return RedisConfigBuilder
     */
    public RedisConfigBuilder useJedisRedis(Jedis jedis) {
        Objects.requireNonNull(jedis);
        configBuilder = new RedisConfigBuilder(new RedisJedisConn(jedis));
        return (RedisConfigBuilder) configBuilder;
    }

    /**
     * 使用JedisCluster
     *
     * @param jedisCluster JedisCluster对象
     * @return RedisConfigBuilder
     */
    public RedisConfigBuilder useJedisRedis(JedisCluster jedisCluster) {
        Objects.requireNonNull(jedisCluster);
        configBuilder = new RedisConfigBuilder(new RedisJedisClusterConn(jedisCluster));
        return (RedisConfigBuilder) configBuilder;
    }

    /**
     * 使用zookeeper的客户端对象CuratorFramework
     *
     * @param curator CuratorFramework对象
     * @return ZookeeperConfigBuilder
     */
    public ZookeeperConfigBuilder useZookeeper(CuratorFramework curator) {
        Objects.requireNonNull(curator);
        configBuilder = new ZookeeperConfigBuilder(curator);
        return (ZookeeperConfigBuilder) configBuilder;
    }

    /**
     * 使用zookeeper的连接地址
     *
     * @param zookeeperConnStr zookeeper的连接地址
     * @return ZookeeperConfigBuilder
     */
    public ZookeeperConfigBuilder useZookeeper(String zookeeperConnStr) {
        return useZookeeper(zookeeperConnStr, new RetryNTimes(3, 5000),
                10000, 10000);
    }

    /**
     * 使用zookeeper的连接地址和自定义配置
     *
     * @param zookeeperConnStr    zookeeper的连接地址
     * @param retryPolicy         重试策略
     * @param connectionTimeoutMs 连接超时时间 毫秒
     * @param sessionTimeoutMs    会话超时时间 毫秒
     * @return ZookeeperConfigBuilder
     */
    public ZookeeperConfigBuilder useZookeeper(String zookeeperConnStr, RetryPolicy retryPolicy,
                                               int connectionTimeoutMs, int sessionTimeoutMs) {
        CuratorFramework curator = CuratorFrameworkFactory.builder().connectString(zookeeperConnStr)
                .retryPolicy(Objects.nonNull(retryPolicy) ? retryPolicy : new RetryNTimes(3,5000))
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .build();
        curator.start();
        return useZookeeper(curator);
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(configBuilder)) {
            configBuilder.close();
        }
    }
}
