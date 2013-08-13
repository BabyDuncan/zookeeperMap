zookeeperMap
============

一个zookeeepr的客户端,把对zookeeper的操作封装成对map的操作.

zookeeper的结构是树形结构,那么一个zookeeper的目录,可以想象成一个map,zookeeper的路径就是map的key,而zookeeper当前路径的值就是map的value.

我们把zookeeper的某一个路径下得一些子节点映射到本地map,并且在zookeeper中注册watcher,当zookeeper的数据发生了变化,通过watcher来更新本地map,而应用通过获取本地map就可以获取zookeeper中的信息了.本地map相当于localcache,效率非常高.

#### 使用方法

请阅读项目中的单元测试模块.
