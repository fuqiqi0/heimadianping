--获取锁中的线程标识 get key
local id = redis.call("get", KEYS[1])

--比较线程标识与锁中的标识是否一致
if (id == ARGV[1]) then
    --一致，说明当前线程持有锁，执行解锁操作
    --释放锁 del key
    return redis.call("del", KEYS[1])
end
--不一致，说明当前线程没有持有锁，执行解锁失败操作
return 0
