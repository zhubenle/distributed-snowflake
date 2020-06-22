package cn.t0mpi9.snowflake.test;

import cn.t0mpi9.snowflake.SnowflakeIdGenerate;
import cn.t0mpi9.snowflake.builder.SnowflakeIdGenerateBuilder;
import org.junit.Test;

import java.util.regex.Pattern;

/**
 * <br/>
 * Created on 2020/6/4 16:13.
 *
 * @author zhubenle
 */
public class SnowflakeIdGenerateTest {

    @Test
    public void testUtil() {
        int sq = 1023;
        System.out.println(sq & 31);
        System.out.println(sq >> 5 & 31);
        System.out.println(System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void testPattern() {
        System.out.println(Pattern.compile("^[A-Za-z0-9\\-_]+$").matcher("iautos-common").matches());
    }

    @Test
    public void testSnowflakeIdGenerateZk() throws Exception {
        SnowflakeIdGenerate snowflakeIdGenerate = SnowflakeIdGenerateBuilder.create()
                .useZookeeper("127.0.0.1:2181")
                .applicationName("sgw")
                .ip("192.168.1.105")
                .port(17000)
                .build();

        long id = snowflakeIdGenerate.nextId();
        System.out.println(SnowflakeIdGenerate.parseId(id));

        id = snowflakeIdGenerate.nextId();
        System.out.println(SnowflakeIdGenerate.parseId(id));

        Thread.sleep(1000 * 60);
    }

    @Test
    public void testSnowflakeIdGenerateRedis() throws Exception {
        SnowflakeIdGenerateBuilder snowflakeIdGenerateBuilder = SnowflakeIdGenerateBuilder.create();
        SnowflakeIdGenerate snowflakeIdGenerate = snowflakeIdGenerateBuilder
                .useLettuceRedis("127.0.0.1", 6379, "", 0)
                .applicationName("sgw")
                .ip("192.168.1.105")
                .port(17000)
                .build();

        long id = snowflakeIdGenerate.nextId();
        System.out.println(SnowflakeIdGenerate.parseId(id));

        id = snowflakeIdGenerate.nextId();
        System.out.println(SnowflakeIdGenerate.parseId(id));

        Thread.sleep(1000 * 60);
        snowflakeIdGenerateBuilder.close();
    }
}
