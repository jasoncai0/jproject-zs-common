# quick start

首先说明下，目前该工程并没有覆盖redisson的所有场景，仅仅针对云游戏项目中使用到的接口场景，主要为了迁移用；

注意注意注意，由于使用了kryo作为序列化工具， pojo必须是serializable！ pojo必须是serializable！ pojo必须是serializable！

后续如果作为多活的组件，由于该项目相对而言还是比较健壮的，可以直接扩展进行覆盖；

如何快速的使用可以参考 BaseBroadcastRedissonTest 里面的测试用例

```java


```