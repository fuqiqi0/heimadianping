-- 1。参数列表
-- 1.1 优惠券id
local voucherId = ARGV[1]
-- 1.2 用户id
local userId = ARGV[2]

-- 2. 数据key
-- 2.1 库存key
local stockKey = "seckill:stock:".. voucherId
-- 2.2 订单key
local orderKey = "seckill:order:".. voucherId

-- 3.脚本业务
-- 3.1 判断库存是否充足get stockKey
if(tonumber(redis.call("get", stockKey)) <= "0") then
    -- 3.2 库存不足，返回0
    return 1
end

-- 3.3 判断用户是否下单 SISMEMBER orderKey userId
if(redis.call("SISMEMBER", orderKey, userId)) then
    -- 3.4 用户已下单，返回0
    return 2
end

-- 3.5 扣减库存
redis.call("incrby", stockKey,-1)

-- 3.6 下订单
redis.call("SADD", orderKey, userId)

-- 3.7 返回0
return 0
