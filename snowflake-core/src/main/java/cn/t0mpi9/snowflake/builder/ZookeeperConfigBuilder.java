package cn.t0mpi9.snowflake.builder;

import cn.t0mpi9.snowflake.Constant;
import cn.t0mpi9.snowflake.SnowflakeIdGenerate;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <br/>
 * Created on 2020/6/4 13:45.
 *
 * @author zhubenle
 */
public class ZookeeperConfigBuilder extends AbstractConfigBuilder<ZookeeperConfigBuilder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperConfigBuilder.class);

    private CuratorFramework curator;

    ZookeeperConfigBuilder() {
    }

    ZookeeperConfigBuilder(CuratorFramework curator) {
        Objects.requireNonNull(curator);
        if (CuratorFrameworkState.LATENT.equals(curator.getState())) {
            curator.start();
        }
        this.curator = curator;
    }

    @Override
    public void close() throws IOException {
        super.close();
        curator.close();
        LOGGER.info("snowflake zk connection closed");
    }

    @Override
    protected SnowflakeIdGenerate doBuild() {
        String ipPort = ip + Constant.COLON + port;
        long workerId;
        long dataCenterId;
        try {
            createEphemeralNode(ipPort);
            curator.getConnectionStateListenable().addListener((client, newState) -> {
                if (ConnectionState.RECONNECTED.equals(newState)) {
                    try {
                        createEphemeralNode(ipPort);
                    } catch (Exception e) {
                        LOGGER.error("重连后创建临时节点异常", e);
                    }
                }
            });

            String persistentPath = getPersistentPath();
            //检查根节点是否存在
            Stat stat = curator.checkExists().forPath(persistentPath);
            String nodePath;
            if (Objects.isNull(stat)) {
                //根节点不存在，第一次连接
                nodePath = createPersistentNode(ipPort);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("根节点不存在，第一次连接创建{}", nodePath);
                }
            } else {
                List<String> childrenPathList = curator.getChildren().forPath(persistentPath);
                if (childrenPathList.size() > Constant.MAX_SEQUENTIAL) {
                    throw new IllegalStateException("SnowflakeIdGenerate创建时，zookeeper中顺序节点数超过" + Constant.MAX_SEQUENTIAL);
                }
                Map<String, String> stringStringMap = childrenPathList.stream()
                        .collect(Collectors.toMap(s -> s.split(Constant.STRIKE)[0], s -> s));
                String nodeChildrenPath = stringStringMap.get(ipPort);
                if (nodeChildrenPath != null) {
                    nodePath = persistentPath + Constant.SLASH + nodeChildrenPath;
                    long preTimeStamp = Long.parseLong(new String(curator.getData().forPath(nodePath)));
                    if (System.currentTimeMillis() < preTimeStamp) {
                        //检查当前系统时间是否比上一次上传到zk时间小
                        throw new IllegalStateException("检查当前系统时间是否比上一次上传到zk时间小, 请确认系统时间");
                    }
                } else {
                    //当前服务第一次连接
                    nodePath = createPersistentNode(ipPort);
                }
            }
            //192.168.105:105-0000000000
            String ipPortSequential = nodePath.substring(nodePath.lastIndexOf(Constant.SLASH) + 1);
            //0000000000
            int sequential = Integer.parseInt(ipPortSequential.split(Constant.STRIKE)[1]);
            workerId = sequential & Constant.BIT;
            dataCenterId = sequential >> 5 & Constant.BIT;
            saveLocalFile(workerId, dataCenterId);
            scheduledUpdateData(nodePath);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            SnowflakeIdGenerate snowflakeIdGenerate;
            if (localFileCache && (snowflakeIdGenerate = useLocalCreate()) != null) {
                return snowflakeIdGenerate;
            }
            throw new RuntimeException("创建SnowflakeIdGenerate, zookeeper加载dataCenterId和workerId失败", e);
        }
        return new SnowflakeIdGenerate(workerId, dataCenterId);
    }

    private String getPersistentPath() {
        return Constant.SLASH + Constant.ROOT_NAME + Constant.SLASH + applicationName + Constant.SLASH + Constant.PERSISTENT_NAME;
    }

    private String getEphemeralPath() {
        return Constant.SLASH + Constant.ROOT_NAME + Constant.SLASH + applicationName + Constant.SLASH + Constant.EPHEMERAL_NAME;
    }

    /**
     * 创建ipPort的持久节点保存当前系统时间戳
     */
    private String createPersistentNode(String ipPort) throws Exception {
        String data = String.valueOf(System.currentTimeMillis());
        return curator.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT_SEQUENTIAL)
                .forPath(getPersistentPath() + Constant.SLASH + ipPort + Constant.STRIKE, data.getBytes());
    }

    /**
     * 添加定时任务，执行更新节点数据为当前时间戳
     */
    private void scheduledUpdateData(String nodePath) {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                String data = String.valueOf(System.currentTimeMillis());
                curator.setData().forPath(nodePath, data.getBytes());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("定时更新zk节点{}的值为{}", nodePath, data);
                }
            } catch (Exception e) {
                LOGGER.error("更新节点路径数据{}异常: {}", nodePath, e.getMessage());
            }
        }, 0, schedulePeriod, scheduleTimeUnit);
    }

    /**
     * 创建ipPort的临时节点，用于查看当前哪些服务连接到zk
     * @param ipPort ip:port
     * @return zk 路径
     * @throws Exception 异常
     */
    private String createEphemeralNode(String ipPort) throws Exception {
        return curator.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(getEphemeralPath() + Constant.SLASH + ipPort, "".getBytes());
    }
}
