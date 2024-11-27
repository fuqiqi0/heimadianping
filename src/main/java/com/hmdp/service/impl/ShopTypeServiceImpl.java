package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = CACHE_SHOP_TYPE_KEY;
        // 1. 查询redis缓存
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopTypeJson)) {
            // 3. 存在，直接返回缓存
            List<ShopType> shopTypeList = JSONUtil.toList(shopTypeJson, ShopType.class);
            shopTypeList.sort(Comparator.comparingInt(ShopType::getSort));
            return Result.ok(shopTypeList);
        }
        // 4. 不存在，查询数据库
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        // 5. 不存在。返回错误
        if (shopTypeList == null || shopTypeList.size() == 0) {
            return Result.fail("未查询到数据");
        }
        // 6. 存在。将数据缓存到redis中
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypeList));
        // 7. 返回数据
        shopTypeList.sort(Comparator.comparingInt(ShopType::getSort));
        return Result.ok(shopTypeList);
    }
}
