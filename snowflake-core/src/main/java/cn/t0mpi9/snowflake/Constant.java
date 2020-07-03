package cn.t0mpi9.snowflake;

import java.util.regex.Pattern;

/**
 * <br/>
 * Created on 2020/6/18 15:20.
 *
 * @author zhubenle
 */
public class Constant {

    private Constant() {
    }

    public static final String ROOT_NAME = "snowflake-id-generate";
    public static final String PERSISTENT_NAME = "persistent";
    public static final String EPHEMERAL_NAME = "ephemeral";

    public static final String COLON = ":";
    public static final String SLASH = "/";
    public static final String STRIKE = "-";
    public static final Integer MAX_SEQUENTIAL = 1024;
    public static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    public static final Pattern PATTERN_APPLICATION_NAME = Pattern.compile("^[A-Za-z0-9\\-_]+$");

    public static final long BIT = 31L;

    public static final String REDIS_ADD_PERSISTENT_SCRIPT = ""
            + "local table_maxn = function(t)\n"
            + "  local mn = -1;\n"
            + "  for k, v in pairs(t) do\n"
            + "    local n = v + 0\n"
            + "    if mn < n then\n"
            + "      mn = n;\n"
            + "    end\n"
            + "  end\n"
            + "  return mn;\n"
            + "end\n"
            + "redis.call('HSET', KEYS[2], KEYS[3], ARGV[1]);\n"
            + "local sq = redis.call('HGET', KEYS[1], KEYS[3]);\n"
            + "if (sq) then\n"
            + "  return sq + 0;\n"
            + "end\n"
            + "local vals = redis.call('HVALS', KEYS[1]);\n"
            + "local max = table_maxn(vals);\n"
            + "local v = max + 1;\n"
            + "redis.call('HSET', KEYS[1], KEYS[3], v);\n"
            + "return v;";

    public static final String REDIS_UPDATE_EPHEMERAL_SCRIPT = ""
            + "redis.call('HSET', KEYS[2], KEYS[3], ARGV[1]);\n"
            + "redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2]);\n";
}
