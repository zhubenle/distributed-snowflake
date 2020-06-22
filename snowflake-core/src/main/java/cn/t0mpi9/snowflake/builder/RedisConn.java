package cn.t0mpi9.snowflake.builder;

import java.io.Closeable;

/**
 * <br/>
 * Created on 2020/6/22 13:19.
 *
 * @author zhubenle
 */
interface RedisConn extends Closeable {

    String eval(String script, String[] keys, String... values);

    String hget(String key, String field);
}
