package cn.t0mpi9.springboot.snowflake.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <br/>
 * Created on 2020/6/19 14:18.
 *
 * @author zhubenle
 */
@ConfigurationProperties(prefix = "snowflake")
public class SnowflakeIdGenerateProperties {

    /**
     * 自定义配置dataCenterId和workerId
     */
    private Direct direct = new Direct();

    /**
     * redis配置
     */
    private Redis redisConfig = new Redis();

    /**
     * zookeeper配置
     */
    private Zk zkConfig = new Zk();

    public Direct getDirect() {
        return direct;
    }

    public void setDirect(Direct direct) {
        this.direct = direct;
    }

    public Redis getRedisConfig() {
        return redisConfig;
    }

    public void setRedisConfig(Redis redisConfig) {
        this.redisConfig = redisConfig;
    }

    public Zk getZkConfig() {
        return zkConfig;
    }

    public void setZkConfig(Zk zkConfig) {
        this.zkConfig = zkConfig;
    }

    public static class Direct {
        private static final long DEFAULT_ID = 0;
        /**
         * 自定义workerId, 默认0
         */
        private long workerId = DEFAULT_ID;
        /**
         * 自定义dataCenterId, 默认0
         */
        private long dataCenterId = DEFAULT_ID;

        public long getWorkerId() {
            return workerId;
        }

        public void setWorkerId(long workerId) {
            this.workerId = workerId;
        }

        public long getDataCenterId() {
            return dataCenterId;
        }

        public void setDataCenterId(long dataCenterId) {
            this.dataCenterId = dataCenterId;
        }
    }

    public static class Zk extends BaseZkAndRedis {

        private static final int DEFAULT_TIMEOUT_MILLIS = 10000;

        /**
         * 是否开启zookeeper管理配置
         */
        private boolean enable;
        /**
         * zookeeper连接地址
         */
        private String connection;
        /**
         * zookeeper连接超时时间, 默认10000毫秒
         */
        private int connectionTimeoutMillis = DEFAULT_TIMEOUT_MILLIS;
        /**
         * zookeeper session超时时间, 默认10000毫秒
         */
        private int sessionTimeoutMillis = DEFAULT_TIMEOUT_MILLIS;

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public String getConnection() {
            return connection;
        }

        public void setConnection(String connection) {
            this.connection = connection;
        }

        public int getConnectionTimeoutMillis() {
            return connectionTimeoutMillis;
        }

        public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
            this.connectionTimeoutMillis = connectionTimeoutMillis;
        }

        public int getSessionTimeoutMillis() {
            return sessionTimeoutMillis;
        }

        public void setSessionTimeoutMillis(int sessionTimeoutMillis) {
            this.sessionTimeoutMillis = sessionTimeoutMillis;
        }
    }

    public static class Redis extends BaseZkAndRedis {
        /**
         * 是否开启redis管理配置, 默认false
         */
        private boolean enable;
        /**
         * redis的host, 默认127.0.0.1
         */
        private String host = "127.0.0.1";
        /**
         * redis的port, 默认6379
         */
        private int port = 6379;
        /**
         * redis的password, 默认空
         */
        private String password;
        /**
         * redis的database, 默认0
         */
        private int database = 0;

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }
    }

    public static class BaseZkAndRedis {
        /**
         * 当前服务ip, 默认获取应用机器网卡ip
         */
        protected String currentServerIp;
        /**
         * 当前服务端口 默认获取server.port，如果server.port为空，默认8080
         */
        protected Integer currentServerPort;
        /**
         * 当前服务应用名称 默认spring.application.name
         */
        protected String applicationName;
        /**
         * 是否本地缓存dataCenterId和workerId
         */
        protected boolean localFileCache = true;
        /**
         * 指定的本地缓存dataCenterId和workerId的文件目录, 默认System.getProperty("java.io.tmpdir")
         */
        protected String fileCachePath;

        public String getCurrentServerIp() {
            return currentServerIp;
        }

        public void setCurrentServerIp(String currentServerIp) {
            this.currentServerIp = currentServerIp;
        }

        public Integer getCurrentServerPort() {
            return currentServerPort;
        }

        public void setCurrentServerPort(Integer currentServerPort) {
            this.currentServerPort = currentServerPort;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public boolean isLocalFileCache() {
            return localFileCache;
        }

        public void setLocalFileCache(boolean localFileCache) {
            this.localFileCache = localFileCache;
        }

        public String getFileCachePath() {
            return fileCachePath;
        }

        public void setFileCachePath(String fileCachePath) {
            this.fileCachePath = fileCachePath;
        }
    }
}
