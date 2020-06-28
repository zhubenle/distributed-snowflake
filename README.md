# 基于snowflake的分布式ID生成方案解决

snowflake是Twitter开源的一种分布式ID生成算法, 基于64位数实现，第1个bit不使用, 然后41个bit是时间戳，然后10个bit是工作机器ID，也有将10bit拆分成5个bit的dataCenterId和5个bit的workerId，当前项目就是，最后12个bit是递增序列号。本项目解决了分布式项目中，多实例部署时，dataCenterId和workerId的自动配置问题。
<br>

本项目基于两种方式实现dataCenterId和workerId配置的管理，分别是zookeeper和redis

#### 1.基于zookeeper

服务连接到zookeeper后会创建以下目录结构

```
/snowflake-id-generate
  /persistent #保存持久递增节点
    /sgw #配置的服务名称 applicationName
      /192.168.1.250:8000-0000000000 #服务实例ip和port，-后面是zk递增序列，也是当前实例的dataCenterId和workderId。并且该路径data是该实例定时上报的时间戳
      /192.168.1.105:8000-0000000001
  /ephemeral #保存临时会话，用于和persistent下的路径对比查看哪些服务实例下线或者永久下线
    /sgw
      /192.168.1.250:8000 #表示这个实例正在连接着zk，该路径的data是该实例首次连到zk的时间戳

```

#### 1.基于redis

服务连接到redis后，会创建2个hash数据结构和一个包含过期时间的string数据结构。<br>
- 一个hash的key是snowflake-id-generate:sgw:persistent (sgw是配置的applicationName)。该hash数据结构存储的是每个实例的ip:port和对应该实例的用于生成dataCenterId和workerId的递增序列
- 另一个hash的可以是snowflake-id-generate:sgw:persistent-time。该hash数据结构存储的是每个实例的ip:port和该实例定时上传的时间戳
- string数据结构的key是snowflake-id-generate:sgw:persistent:192.168.1.250:8000。作用和上面zk的临时节点相同

## 用法
### snowflake-core模块
生成ID的类是`SnowflakeIdGenerate`，该类有个实例方法`public synchronized long nextId()`返回`Long`类型的ID。还有个静态方法`parseId(long snowflakeId)`可以解析ID的组成。

`SnowflakeIdGenerate`的对象创建可以直接通过构造方法生成，如果要使用zookeeper或redis来统一配置dataCenterId和workerId，需要使用`SnowflakeIdGenerateBuilder`类。
`SnowflakeIdGenerateBuilder.create()`静态方法创建`SnowflakeIdGenerateBuilder`对象，该对象包含方法如下:
- `public DirectConfigBuilder useDirect()`方法，该方法返回一个`DirectConfigBuilder`对象，该对象两个方法`dataCenterId(long dataCenterId)`和`workerId(long workerId)`
分别设置dataCenterId和workerId，然后通过`DirectConfigBuilder`的`build()`方法创建`SnowflakeIdGenerate`对象。
- `public ZookeeperConfigBuilder useZookeeper(CuratorFramework curator)`方法，该方法返回`ZookeeperConfigBuilder`对象, 参数是zookeeper客户端工具CuratorFramework的对象。
该对象父类中有几个方法用于配置必要参数，分别为`ip(String ip)`设置当前服务实例的ip地址，`port(Integer port)`设置当前服务实例的端口，`applicationName(String applicationName)`
设置当前应用项目名称。最后也是通过父类方法`build()`方法创建`SnowflakeIdGenerate`对象。
- `public RedisConfigBuilder useLettuceRedis(RedisClient redisClient)`该方法返回`RedisConfigBuilder`对象, 参数是redis客户端lettuce。其他同上
- `public RedisConfigBuilder useJedisRedis(Jedis jedis)`该方法同上，只是参数是redis客户端jedis

### snowflake-spring-boot-starter模块
该模块提供的是spring-boot自动化配置`SnowflakeIdGenerate`对象。`ip`配置默认会获取网卡ip，`port`默认获取`server.port`参数，`applicationName`默认获取`spring.application.name`参数，
所以在没配置`port`和`applicationName`时，这两个spring参数`server.port`和`spring.application.name`需要配置

## 实际应用
项目上应用有两种方式:
- 使用该依赖创建个单独的服务用于对外提供主键生成接口
- 将该依赖添加到项目中配置使用

因为在项目中配置使用时，需要配置当前服务的ip和port，为了避免手动配置，项目上运行时，ip配置是获取当前服务运行机器的网卡ip，示例代码如下:
```java
public class NetUtils {
    public static InetAddress getLocalAddress() {
        InetAddress candidateAddress = null;
        try {
            // 遍历所有的网络接口
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                // 在所有的接口下再遍历IP
                Enumeration<InetAddress> inetAddrs = iface.getInetAddresses();
                while (inetAddrs.hasMoreElements()) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        // 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            candidateAddress = InetAddress.getLocalHost();
        } catch (Exception e) {
            LOGGER.error("获取网卡IP异常: {}", e.getMessage());
        }
        return candidateAddress;
    }
}
```
端口配置，如果是spring boot项目好获取，直接server.port属性就行。如果是普通spring的tomcat项目可以通过如下方式获取当前服务port:
```java
@Configuration
public class DemoConfiguration implements InitializingBean, DisposableBean {
    @Bean
    public SnowflakeIdGenerate snowflakeIdGenerate(@Value("${zookeeper.connection}") String connectionStr,
                                                   WebApplicationContext webApplicationContext) {
        Integer port = null;
        try {
            //暂时只判断了tomcat容器，其他容器会失败
            if (webApplicationContext.getServletContext() instanceof ApplicationContextFacade) {
                //获取ServletContext
                ApplicationContextFacade contextFacade = (ApplicationContextFacade) webApplicationContext.getServletContext();
                Field field = ApplicationContextFacade.class.getDeclaredField("context");
                field.setAccessible(true);
                ApplicationContext catalinaApplicationContext = (ApplicationContext) field.get(contextFacade);
                field = ApplicationContext.class.getDeclaredField("service");
                field.setAccessible(true);
                StandardService standardService = (StandardService) field.get(catalinaApplicationContext);
                for (Connector connector : standardService.findConnectors()) {
                    if (connector.getProtocol().toLowerCase().contains("http")) {
                        port = connector.getPort();
                        //如果tomcat配置了多个Connector,只取第一个
                        break;
                    }
                }
            } else if (webApplicationContext.getServletContext() instanceof MockServletContext) {
                //如果是本地Junit测试，设置为端口为0
                port = 0;
            }

        } catch (Exception e) {
            LOGGER.error("获取当前tomcat的http端口失败", e);
        }
        String ip = NetUtils.getLocalAddress().getHostAddress();
        return snowflakeIdGenerateBuilder.useZookeeper(connectionStr)
                .ip(ip)
                .port(port)
                .applicationName("sgw")
                .build();
    }

    private SnowflakeIdGenerateBuilder snowflakeIdGenerateBuilder;

    @Override
    public void afterPropertiesSet() throws Exception {
        snowflakeIdGenerateBuilder = SnowflakeIdGenerateBuilder.create();
    }

    @Override
    public void destroy() throws Exception {
        snowflakeIdGenerateBuilder.close();
    }
}
```
主要思路就是获取到ServletContext对象，然后获取到tomcat的Connector对象，并获取到配置的第一个连接器配置的端口。注意上面的方式还需要引用以下包，version根据运行的tomcat版本选择:<br>
- gradle
```
providedCompile group: 'org.apache.tomcat', name: 'tomcat-catalina', version: '8.5.56'
```
- maven 
```
<dependency>
    <groupId>org.apache.tomcat</groupId>
    <artifactId>tomcat-catalina</artifactId>
    <version>8.5.56</version>
    <scope>provided</scope>
</dependency>
```

<br>
~~有问题麻烦提出，觉得还行的就给个star~~