package cn.t0mpi9.snowflake.builder;

import cn.t0mpi9.snowflake.SnowflakeIdGenerate;

import java.io.IOException;

/**
 * <br/>
 * Created on 2020/6/4 13:46.
 *
 * @author zhubenle
 */
public class DirectConfigBuilder implements ConfigBuilder {

    private long workerId;
    private long dataCenterId;

    DirectConfigBuilder() {
    }

    public DirectConfigBuilder workerId(long workerId) {
        this.workerId = workerId;
        return this;
    }

    public DirectConfigBuilder dataCenterId(long dataCenterId) {
        this.dataCenterId = dataCenterId;
        return this;
    }

    @Override
    public SnowflakeIdGenerate build() {
        return new SnowflakeIdGenerate(workerId, dataCenterId);
    }

    @Override
    public void close() throws IOException {
    }
}
