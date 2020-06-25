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

~~有问题麻烦提出，觉得还行的就给个star~~