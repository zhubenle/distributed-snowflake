package cn.t0mpi9.snowflake.builder;

import cn.t0mpi9.snowflake.SnowflakeIdGenerate;

import java.io.Closeable;

/**
 * <br/>
 * Created on 2020/6/8 13:50.
 *
 * @author zhubenle
 */
public interface ConfigBuilder extends Closeable {
    /**
     * build
     * @return
     */
    SnowflakeIdGenerate build();
}
