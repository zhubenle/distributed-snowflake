package cn.t0mpi9.snowflake.builder;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

import java.io.IOException;

/**
 * <br/>
 * Created on 2020/6/22 13:24.
 *
 * @author zhubenle
 */
class RedisLettuceClusterConn implements RedisConn {

    private final StatefulRedisClusterConnection<String, String> connection;

    RedisLettuceClusterConn(StatefulRedisClusterConnection<String, String> connection) {
        this.connection = connection;
    }

    @Override
    public String eval(String script, String[] keys, String... values) {
        return connection.sync().eval(script, ScriptOutputType.VALUE, keys, values);
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
