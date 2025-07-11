package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.SystemConstants;
import jodd.util.StringUtil;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
//         Shop shop = cacheClient
//                 .queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
        Shop shop = cacheClient
                .queryWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class,this::getById,20L,TimeUnit.SECONDS);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);
        if(shop == null){
            return Result.fail("店铺不存在");
        }
        // 7.返回数据
        return Result.ok(shop);
    }

//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

//    public Shop queryWithLogicalExpire(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        // 1. 从redis中查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2. 判断是否存在
//        if (StrUtil.isBlank(shopJson)) {
//            // 3.存在，直接返回
//            return null;
//        }
//        // 4.命中，需要先把json反序列化成对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        // 5.判断是否过期
//        if(expireTime.isAfter(LocalDateTime.now())){
//            // 5.1 未过期，直接返回店铺信息
//            return shop;
//        }
//        // 5.2 已过期，需要缓存重建
//        // 6.缓存重建
//        // 6.1 获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        // 6.2 判断是否获取锁成功
//        if(isLock) {
//            // 6.3 成功，开启独立线程，实现缓存重建
//            CACHE_REBUILD_EXECUTOR.submit(()->{
//                try {
//                    // 6.3 缓存重建
//                    this.saveShop2Redis(id,20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    // 6.4 释放互斥锁
//                    unlock(lockKey);
//                }
//            });
//        }
//        // 6.4 返回过期的商铺信息
//        return shop;
//    }

//    public Shop queryWithMutex(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        // 1. 从redis中查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2. 判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            // 3.存在，直接返回缓存
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        // 判断是否命中空值缓存
//        if(shopJson != null){
//            // 返回一个错误信息
//            return null;
//        }
//
//        // 4.实现缓存重建
//        // 4.1. 尝试获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            // 4.2. 判断是否获取成功
//            if(!isLock){
//                // 4.3. 如果失败，则休眠并重试
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            // 4.4. 如果成功，则根据id查询数据库
//            shop = getById(id);
//            // 模拟重建的延时
//            Thread.sleep(200);
//            // 5.不存在，返回错误
//            if (shop == null) {
//                // 将空值写入redis中，防止缓存穿透
//                stringRedisTemplate.opsForValue().set(key, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
//                // 返回错误信息
//                return null;
//            }
//            // 6.存在，将数据缓存到redis中
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            // 7.释放互斥锁
//            unlock(lockKey);
//        }
//        // 8.返回数据
//        return shop;
//    }

//    public Shop queryWithPassThrouth(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        // 1. 从redis中查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2. 判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            // 3.存在，直接返回缓存
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        // 判断是否命中空值缓存
//        if(shopJson != null){
//            // 返回一个错误信息
//            return null;
//        }
//        // 4.不存在，根据id查询数据库
//        Shop shop = getById(id);
//        // 5.不存在，返回错误
//        if (shop == null) {
//            // 将空值写入redis中，防止缓存穿透
//            stringRedisTemplate.opsForValue().set(key, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            // 返回错误信息
//            return null;
//        }
//        // 6.存在，将数据缓存到redis中
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        // 7.返回数据
//        return shop;
//    }

//    private boolean tryLock(String key) {
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, " 1", 10, TimeUnit.MINUTES);
//        return BooleanUtil.isTrue(flag);
//    }
//
//    private void unlock(String key) {
//        stringRedisTemplate.delete(key);
//    }

//    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
//        // 1. 从数据库查询数据
//        Shop shop = getById(id);
//        Thread.sleep(200);
//        // 2. 封装成逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        // 3. 缓存到redis中
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1. 更新数据库
        updateById(shop);
        // 2. 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());

        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //1. 判断是否需要根据坐标查询
        if(x == null || y == null){
            //2. 不需要，直接通过数据库查询
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        // 2. 计算分页参数
        int start = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        // 3.查询Redis,按照距离排序、分页。结果：shopId,距离
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
        // 4.判断是否非空
        if(results == null){
            return Result.ok(Collections.emptyList());
        }
        // 5. 解析出id
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if(list.size() <= start){
            return Result.ok(Collections.emptyList());
        }
        // 6. 截取 start ~ end的结果
        List<Long> ids = new ArrayList<>(list.size());
        Map<String,Double> distanceMap = new HashMap<>(list.size());
        list.stream().skip(start).forEach(result->{
            // 6.1 解析出shopId
            String shopIdStr = result.getContent().getName();
            ids.add(Long.parseLong(shopIdStr));
            // 6.2 获取距离
            Double distance = result.getDistance().getValue();
            System.out.println(shopIdStr + " : " + distance);
            distanceMap.put(shopIdStr,distance);
        });
        // 7.根据id查询店铺
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id, " + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()));
        }
        return Result.ok(shops);
    }
}
