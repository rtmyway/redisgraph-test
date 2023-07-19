## redisgraph-test

#### 1.Test environment
```
CPU: Intel® Core™ i7-10710U CPU @ 1.10GHz × 12
Memory: 64.0 GiB
OS: Ubuntu 22.04.2 LTS
Docker Engine: 24.0.2 Community
RedisGraph Image: redislabs/redisgraph:2.10.10
```


#### 2.RedisGraph deployment
```
sudo docker run
--name redisgraph
--restart always
-p 6379:6379
-d redislabs/redisgraph:2.10.10 redis-server --loadmodule /usr/lib/redis/modules/redisgraph.so --requirepass 123456
```


#### 3.testing by java program
```
# jdk11 + maven

# step1 replace host&&port&&password in class TestMain.java

# step2 running TestMain.java

```


#### 3.Testing of RedisGraph ran from 2023-06-13 15:40 until 2023-06-19 08:08. There were three exceptions that occurred and were automatically restarted. The error messages can be found on line 42008, 64772, and 78578 of the redis_logs.txt file (they can be quickly located by searching for the keyword "REDIS BUG REPORT START").
[redis_logs.txt](./redis_logs.txt)



#### 4.Initial problem diagnosis indicates that there is a conflict between RedisGraph's rdb operation and the "edge" creation command.
