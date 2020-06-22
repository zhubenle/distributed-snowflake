package cn.t0mpi9.snowflake.builder;

import redis.clients.jedis.JedisCluster;

import java.io.IOException;
import java.util.Arrays;

/**
 * <br/>
 * Created on 2020/6/22 13:24.
 *
 * @author zhubenle
 */
class RedisJedisClusterConn implements RedisConn {

    private final JedisCluster jedisCluster;

    RedisJedisClusterConn(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    @Override
    public String eval(String script, String[] keys, String... values) {
        return String.valueOf(jedisCluster.eval(script, Arrays.asList(keys), Arrays.asList(values)));
    }

    @Override
    public String hget(String key, String field) {
        return jedisCluster.hget(key, field);
    }

    @Override
    public void close() throws IOException {
        jedisCluster.close();
    }
}
