package cn.t0mpi9.snowflake.builder;

import cn.t0mpi9.snowflake.Constant;
import cn.t0mpi9.snowflake.SnowflakeIdGenerate;

import java.io.IOException;
import java.util.Objects;

/**
 * <br/>
 * Created on 2020/6/4 13:46.
 *
 * @author zhubenle
 */
public class DirectIpConfigBuilder implements ConfigBuilder {

    private String currentServerIp;

    DirectIpConfigBuilder() {
    }

    public DirectIpConfigBuilder currentServerIp(String currentServerIp) {
        this.currentServerIp = currentServerIp;
        return this;
    }

    @Override
    public SnowflakeIdGenerate build() {
        Objects.requireNonNull(currentServerIp);
        long sqe = Long.parseLong(currentServerIp.substring(currentServerIp.lastIndexOf(".") + 1));
        long workerId = sqe & Constant.BIT;
        long dataCenterId = sqe >> 5 & Constant.BIT;
        return new SnowflakeIdGenerate(workerId, dataCenterId);
    }

    @Override
    public void close() throws IOException {
    }
}
