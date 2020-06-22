package cn.t0mpi9.snowflake.builder;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;

import java.io.IOException;

/**
 * <br/>
 * Created on 2020/6/22 13:24.
 *
 * @author zhubenle
 */
class RedisLettuceConn implements RedisConn {

    private final StatefulRedisConnection<String, String> connection;

    RedisLettuceConn(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    @Override
    public String eval(String script, String[] keys, String... values) {
        return String.valueOf((Long) connection.sync().eval(script, ScriptOutputType.INTEGER, keys, values));
    }

    @Override
    public String hget(String key, String field) {
        return connection.sync().hget(key, field);
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}
