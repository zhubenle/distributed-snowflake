package cn.t0mpi9.snowflake.builder;

import cn.t0mpi9.snowflake.Constant;
import cn.t0mpi9.snowflake.SnowflakeIdGenerate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <br/>
 * Created on 2020/6/18 15:13.
 *
 * @author zhubenle
 */
public abstract class AbstractConfigBuilder<T extends AbstractConfigBuilder> implements ConfigBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigBuilder.class);

    /**
     * 用于执行定时任务
     */
    protected ScheduledExecutorService scheduledExecutorService;
    /**
     * 定时时间间隔
     */
    protected long schedulePeriod = 5;
    /**
     * 定时时间单位
     */
    protected TimeUnit scheduleTimeUnit = TimeUnit.SECONDS;

    /**
     * 应用服务内网ip
     */
    protected String ip;
    /**
     * 应用服务端口
     */
    protected Integer port;
    /**
     * 应用名称
     */
    protected String applicationName;
    /**
     * 是否开启本地文件缓存workId和dataCenterId
     */
    protected boolean localFileCache = true;
    /**
     * 开启文件缓存时，可以指定缓存的路径
     */
    protected String fileCachePath;

    public T ip(String ip) {
        this.ip = ip;
        return (T) this;
    }

    public T port(Integer port) {
        this.port = port;
        return (T) this;
    }

    public T applicationName(String applicationName) {
        this.applicationName = applicationName;
        return (T) this;
    }

    public T localFileCache(boolean localFileCache) {
        this.localFileCache = localFileCache;
        return (T) this;
    }

    public T fileCachePath(String fileCachePath) {
        this.fileCachePath = fileCachePath;
        return (T) this;
    }

    public T scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        return (T) this;
    }

    public T schedulePeriod(long schedulePeriod) {
        this.schedulePeriod = schedulePeriod;
        return (T) this;
    }

    public T scheduleTimeUnit(TimeUnit scheduleTimeUnit) {
        this.scheduleTimeUnit = scheduleTimeUnit;
        return (T) this;
    }

    /**
     * 初始化定时任务执行器
     */
    private void initScheduled() {
        if (Objects.isNull(this.scheduledExecutorService)) {
            this.scheduledExecutorService = new ScheduledThreadPoolExecutor(1, r -> {
                Thread thread = new Thread(r, "snowflake-zk-updateData");
                thread.setDaemon(true);
                return thread;
            });
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(() -> {
                        scheduledExecutorService.shutdownNow();
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("关闭scheduledExecutorService");
                        }
                    }));
        }
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(scheduledExecutorService)) {
            scheduledExecutorService.shutdownNow();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("关闭scheduledExecutorService");
            }
        }
    }

    @Override
    public SnowflakeIdGenerate build() {
        Objects.requireNonNull(ip);
        Objects.requireNonNull(port);
        Objects.requireNonNull(applicationName);
        if (!Constant.PATTERN_APPLICATION_NAME.matcher(applicationName).matches()) {
            throw new IllegalArgumentException("applicationName格式有问题");
        }
        LOGGER.info("snowflake初始化参数ip={}, port={}, applicationName={}", ip, port, applicationName);
        initScheduled();

        return doBuild();
    }

    /**
     * 子类实现的build功能
     *
     * @return SnowflakeIdGenerate
     */
    protected abstract SnowflakeIdGenerate doBuild();

    /**
     * 保存到本地文件
     *
     * @param workerId     workerId
     * @param dataCenterId dataCenterId
     */
    protected void saveLocalFile(long workerId, long dataCenterId) {
        if (localFileCache) {
            File file = getLocalFile();
            String data = dataCenterId + Constant.STRIKE + workerId;
            try {
                if (file.exists()) {
                    try (PrintWriter pw = new PrintWriter(file)) {
                        pw.print(data);
                    }
                } else {
                    File parentFile = file.getParentFile();
                    if ((parentFile.exists() || (!parentFile.exists() && parentFile.mkdirs())) && file.createNewFile()) {
                        try (PrintWriter pw = new PrintWriter(file)) {
                            pw.print(data);
                        }
                    } else {
                        LOGGER.error("文件创建失败:{}", file.getAbsolutePath());
                    }
                }

            } catch (IOException e) {
                LOGGER.error("保存dataCenterId={}, workerId={}到本地文件异常", dataCenterId, workerId, e);
            }
        }
    }

    /**
     * 使用本地文件中缓存的dataCenterId和workerId创建SnowflakeIdGenerate
     *
     * @return SnowflakeIdGenerate
     */
    protected SnowflakeIdGenerate useLocalCreate() {
        File file = getLocalFile();
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String[] srs = br.readLine().split(Constant.STRIKE);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("从本地缓存文件{}读取dataCenterId和workerId", file.getAbsolutePath());
                }
                return new SnowflakeIdGenerate(Integer.parseInt(srs[0]), Integer.parseInt(srs[1]));
            } catch (IOException e) {
                LOGGER.warn("读取文件{}配置失败", file.getAbsolutePath(), e);
            }
        } else {
            LOGGER.warn("文件{}不存在", file.getAbsolutePath());
        }
        return null;

    }

    /**
     * 获取本地缓存dataCenterId和workerId的文件
     *
     * @return File
     */
    protected File getLocalFile() {
        String fileName = Constant.ROOT_NAME + File.separator + applicationName + Constant.STRIKE + port;
        String filePath = (Constant.TMP_DIR.endsWith(File.separator) ? Constant.TMP_DIR : (Constant.TMP_DIR + File.separator))
                + fileName;
        if (fileCachePath != null) {
            filePath = fileCachePath + (fileCachePath.endsWith(Constant.SLASH) ? "" : Constant.SLASH) + fileName;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("获取本地缓存文件路径{}", filePath);
        }
        return new File(filePath);
    }
}
