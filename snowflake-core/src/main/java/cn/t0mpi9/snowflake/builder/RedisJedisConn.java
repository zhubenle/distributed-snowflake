package cn.t0mpi9.snowflake.builder;

import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Arrays;

/**
 * <br/>
 * Created on 2020/6/22 13:24.
 *
 * @author zhubenle
 */
class RedisJedisConn implements RedisConn {

    private final Jedis jedis;

    RedisJedisConn(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public String eval(String script, String[] keys, String... values) {
        return String.valueOf(jedis.eval(script, Arrays.asList(keys), Arrays.asList(values)));
    }

    @Override
    public String hget(String key, String field) {
        return jedis.hget(key, field);
    }

    @Override
    public void close() throws IOException {
        jedis.close();
    }
}
