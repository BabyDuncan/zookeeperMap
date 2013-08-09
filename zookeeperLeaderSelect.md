### 一个zookeeper 集群选举leader的算法举例
----------
假设当前集群有5个实例,对应的id分别为1,2,3,4,5.他们的都没有原始数据,那么leader的选举过程是这样的:
    
    1,server1 启动,发现2,3,4,5 都连不上,他选举自己为leader,但是没有超过半数的同志响应,所以他继续寻找leader.
    2,server2 启动,发现1可以连上,他选举自己为leader,并通知server1,server1的id没有server2大,此二人苟同,选举server为
    leader,但是还是没有超过半数的同志支持,所以,暂定server2为leader,继续寻找新leader.
    3,server3 启动,发现1,2可以连上,server选举自己为leader,并通知1,2,由于此时server3的id最大,所以1,2同意server3为
    leader,此时3人同意server3为leader,超过半数,所以定为server3 为leader,不再寻找新leader,1,2 主动变身为follower.
    4,server4 启动,发现现在的leader为server3,并且超过了半数人支持,自己选择低调,服从leader,自己变身为follower.
    5,server5 启动,与server4 相同.
    
到此,leader 选定为server3,开始正常的zookeeper集群运行.


####第二种情况,就是每个server都有一份原始数据,那么在启动之后,他们会加载自己的本地数据,并使用本地数据参与选举.假设1,
2,3具有相同的本地数据,4,具有稍微新一些的本地数据,5与4相同的本地数据,那么选举过程将会波澜起伏.

    1,2,3步骤同第一个例子.
    4,server4 启动,发现当前集群中得leader是server3,并且超过半数的人支持,但是他把自己的本地数据与其他人的数据对比之后
    ,发现自己的数据是更新的,所以他把自己的选票给1,2,3看了之后,1,2,3纷纷表示4的数据更新,所以此4人进行了新一轮的选举,
    由于4的server id 更大,并且数据更为新,所以此次选举server 4为新任leader.
    5,server5 启动,发现当前集群的leader为4,对比过本地数据之后,发现自己的数据与4一样,虽然自己的server id 更大,但是,
    已经有超过半数的人支持server4,所以自己选择低调,服从leader,把自己变成follower.
    从此,zookeeper集群开始正常的运行.
