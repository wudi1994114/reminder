package com.example.reminder.utils;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Component
@Slf4j
public class RedisUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     * @return 是否成功
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("设置缓存过期时间失败：key={}, time={}", key, time, e);
            return false;
        }
    }

    /**
     * 根据key获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("判断key是否存在失败：key={}", key, e);
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList(key));
            }
        }
    }

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取指定类型对象
     *
     * @param key   键
     * @param clazz 返回对象类型
     * @return 值
     */
    public <T> T getObj(String key, Class<T> clazz) {
        Object obj = get(key);
        if (obj == null) {
            return null;
        }
        
        // 使用JacksonUtils进行对象转换
        if (obj instanceof String) {
            return JacksonUtils.fromJson((String) obj, clazz);
        } else {
            // 如果不是字符串，先转为JSON字符串，再转为目标类型
            String jsonStr = JacksonUtils.toJson(obj);
            return JacksonUtils.fromJson(jsonStr, clazz);
        }
    }

    /**
     * 获取复杂类型对象
     *
     * @param key   键
     * @param typeReference 返回对象类型引用
     * @return 值
     */
    public <T> T getObj(String key, TypeReference<T> typeReference) {
        Object obj = get(key);
        if (obj == null) {
            return null;
        }
        
        // 使用JacksonUtils进行对象转换
        if (obj instanceof String) {
            return JacksonUtils.fromJson((String) obj, typeReference);
        } else {
            // 如果不是字符串，先转为JSON字符串，再转为目标类型
            String jsonStr = JacksonUtils.toJson(obj);
            return JacksonUtils.fromJson(jsonStr, typeReference);
        }
    }

    /**
     * 批量获取
     *
     * @param keys 键集合
     * @return 值集合
     */
    public List<Object> multiGet(Collection<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * 根据前缀获取所有key
     *
     * @param prefix 键前缀
     * @return 匹配的key集合
     */
    public Set<String> keys(String prefix) {
        return redisTemplate.keys(prefix + "*");
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("设置缓存失败：key={}", key, e);
            return false;
        }
    }

    /**
     * 将对象序列化为JSON后放入缓存
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean setJson(String key, Object value) {
        try {
            String jsonString = JacksonUtils.toJson(value);
            redisTemplate.opsForValue().set(key, jsonString);
            return true;
        } catch (Exception e) {
            log.error("设置JSON缓存失败：key={}", key, e);
            return false;
        }
    }

    /**
     * 将对象序列化为JSON后放入缓存并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false失败
     */
    public boolean setJson(String key, Object value, long time) {
        try {
            String jsonString = JacksonUtils.toJson(value);
            if (time > 0) {
                redisTemplate.opsForValue().set(key, jsonString, time, TimeUnit.SECONDS);
            } else {
                set(key, jsonString);
            }
            return true;
        } catch (Exception e) {
            log.error("设置JSON缓存失败：key={}, time={}", key, time, e);
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("设置缓存失败：key={}, time={}", key, time, e);
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return 增加后的值
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(小于0)
     * @return 减少后的值
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    //=====================Map======================

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * HashGet并转换为指定类型
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @param clazz 返回对象类型
     * @return 值
     */
    public <T> T hget(String key, String item, Class<T> clazz) {
        Object obj = hget(key, item);
        if (obj == null) {
            return null;
        }
        
        // 使用JacksonUtils转换
        if (obj instanceof String) {
            return JacksonUtils.fromJson((String) obj, clazz);
        } else {
            String jsonStr = JacksonUtils.toJson(obj);
            return JacksonUtils.fromJson(jsonStr, clazz);
        }
    }

    /**
     * HashGet并转换为复杂类型
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @param typeReference 返回对象类型引用
     * @return 值
     */
    public <T> T hget(String key, String item, TypeReference<T> typeReference) {
        Object obj = hget(key, item);
        if (obj == null) {
            return null;
        }
        
        // 使用JacksonUtils转换
        if (obj instanceof String) {
            return JacksonUtils.fromJson((String) obj, typeReference);
        } else {
            String jsonStr = JacksonUtils.toJson(obj);
            return JacksonUtils.fromJson(jsonStr, typeReference);
        }
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 将Map序列化为JSON后存入Hash表
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmsetJson(String key, Map<String, Object> map) {
        try {
            Map<String, String> jsonMap = CollectionUtils.newLinkedHashMap(map.size());
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jsonMap.put(entry.getKey(), JacksonUtils.toJson(entry.getValue()));
            }
            redisTemplate.opsForHash().putAll(key, jsonMap);
            return true;
        } catch (Exception e) {
            log.error("设置Hash JSON缓存失败：key={}", key, e);
            return false;
        }
    }

    /**
     * 将Map序列化为JSON后存入Hash表并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
public boolean hmsetJson(String key, Map<String, Object> map, long time) {
        try {
            Map<String, String> jsonMap = CollectionUtils.newLinkedHashMap(map.size());
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jsonMap.put(entry.getKey(), JacksonUtils.toJson(entry.getValue()));
            }
            redisTemplate.opsForHash().putAll(key, jsonMap);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("设置Hash JSON缓存失败：key={}, time={}", key, time, e);
            return false;
        }
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("设置Hash缓存失败：key={}", key, e);
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("设置Hash缓存失败：key={}, time={}", key, time, e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            log.error("设置Hash项缓存失败：key={}, item={}", key, item, e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("设置Hash项缓存失败：key={}, item={}, time={}", key, item, time, e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入JSON序列化后的数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hsetJson(String key, String item, Object value) {
        try {
            String jsonString = JacksonUtils.toJson(value);
            redisTemplate.opsForHash().put(key, item, jsonString);
            return true;
        } catch (Exception e) {
            log.error("设置Hash项JSON缓存失败：key={}, item={}", key, item, e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入JSON序列化后的数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hsetJson(String key, String item, Object value, long time) {
        try {
            String jsonString = JacksonUtils.toJson(value);
            redisTemplate.opsForHash().put(key, item, jsonString);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("设置Hash项JSON缓存失败：key={}, item={}, time={}", key, item, time, e);
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return 新增后的值
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash递减
     *
     * @param key  键
     * @param item 项
     * @param by   要减少几(小于0)
     * @return 减少后的值
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    //============================Set=============================

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return Set中的所有值
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("获取Set缓存失败：key={}", key, e);
            return null;
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key   键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.error("检查Set中值是否存在失败：key={}, value={}", key, value, e);
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("设置Set缓存失败：key={}", key, e);
            return 0;
        }
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            log.error("设置Set缓存失败：key={}, time={}", key, time, e);
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            log.error("获取Set大小失败：key={}", key, e);
            return 0;
        }
    }

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            log.error("移除Set值失败：key={}", key, e);
            return 0;
        }
    }

    /**
     * 将JSON序列化后的数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetJson(String key, Object... values) {
        try {
            String[] jsonValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                jsonValues[i] = JacksonUtils.toJson(values[i]);
            }
            return redisTemplate.opsForSet().add(key, (Object[]) jsonValues);
        } catch (Exception e) {
            log.error("设置Set JSON缓存失败：key={}", key, e);
            return 0;
        }
    }

    /**
     * 将JSON序列化后的数据放入set缓存并设置时间
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetJsonAndTime(String key, long time, Object... values) {
        try {
            String[] jsonValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                jsonValues[i] = JacksonUtils.toJson(values[i]);
            }
            Long count = redisTemplate.opsForSet().add(key, (Object[]) jsonValues);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            log.error("设置Set JSON缓存失败：key={}, time={}", key, time, e);
            return 0;
        }
    }

    /**
     * 从Set中获取指定类型的对象
     *
     * @param key 键
     * @param clazz 返回对象类型
     * @return Set中的所有值转换为指定类型
     */
    public <T> Set<T> sGetObj(String key, Class<T> clazz) {
        try {
            Set<Object> set = redisTemplate.opsForSet().members(key);
            if (set == null || set.isEmpty()) {
                return Collections.emptySet();
            }
            Set<T> result = new LinkedHashSet<>(set.size());
            for (Object obj : set) {
                if (obj instanceof String) {
                    result.add(JacksonUtils.fromJson((String) obj, clazz));
                } else {
                    String jsonStr = JacksonUtils.toJson(obj);
                    result.add(JacksonUtils.fromJson(jsonStr, clazz));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("获取Set并转换类型失败：key={}, class={}", key, clazz.getName(), e);
            return Collections.emptySet();
        }
    }

    //===============================List=================================

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束 0 到 -1代表所有值
     * @return List内容
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("获取List缓存失败：key={}, start={}, end={}", key, start, end, e);
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error("获取List长度失败：key={}", key, e);
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return 值
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.error("获取List索引值失败：key={}, index={}", key, index, e);
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("设置List缓存失败：key={}", key, e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return 是否成功
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("设置List缓存失败：key={}, time={}", key, time, e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.error("设置List缓存失败：key={}", key, e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return 是否成功
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("设置List缓存失败：key={}, time={}", key, time, e);
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return 是否成功
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            log.error("修改List索引值失败：key={}, index={}, value={}", key, index, value, e);
            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Exception e) {
            log.error("移除List值失败：key={}, count={}, value={}", key, count, value, e);
            return 0;
        }
    }

    /**
     * 将JSON序列化后的对象放入list缓存
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lSetJson(String key, Object value) {
        try {
            String jsonString = JacksonUtils.toJson(value);
            redisTemplate.opsForList().rightPush(key, jsonString);
            return true;
        } catch (Exception e) {
            log.error("设置List JSON缓存失败：key={}", key, e);
            return false;
        }
    }

    /**
     * 将JSON序列化后的对象放入list缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return 是否成功
     */
    public boolean lSetJson(String key, Object value, long time) {
        try {
            String jsonString = JacksonUtils.toJson(value);
            redisTemplate.opsForList().rightPush(key, jsonString);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("设置List JSON缓存失败：key={}, time={}", key, time, e);
            return false;
        }
    }

    /**
     * 将JSON序列化后的对象列表放入缓存
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lSetAllJson(String key, List<?> value) {
        try {
            List<String> jsonList = new ArrayList<>(value.size());
            for (Object item : value) {
                jsonList.add(JacksonUtils.toJson(item));
            }
            redisTemplate.opsForList().rightPushAll(key, jsonList);
            return true;
        } catch (Exception e) {
            log.error("设置List JSON缓存失败：key={}", key, e);
            return false;
        }
    }

    /**
     * 将JSON序列化后的对象列表放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return 是否成功
     */
    public boolean lSetAllJson(String key, List<?> value, long time) {
        try {
            List<String> jsonList = new ArrayList<>(value.size());
            for (Object item : value) {
                jsonList.add(JacksonUtils.toJson(item));
            }
            redisTemplate.opsForList().rightPushAll(key, jsonList);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("设置List JSON缓存失败：key={}, time={}", key, time, e);
            return false;
        }
    }

    /**
     * 获取list缓存的内容并转换为指定类型
     *
     * @param key   键
     * @param start 开始
     * @param end   结束 0 到 -1代表所有值
     * @param clazz 返回对象类型
     * @return List内容
     */
    public <T> List<T> lGetObj(String key, long start, long end, Class<T> clazz) {
        try {
            List<Object> list = redisTemplate.opsForList().range(key, start, end);
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            List<T> result = new ArrayList<>(list.size());
            for (Object obj : list) {
                if (obj instanceof String) {
                    result.add(JacksonUtils.fromJson((String) obj, clazz));
                } else {
                    String jsonStr = JacksonUtils.toJson(obj);
                    result.add(JacksonUtils.fromJson(jsonStr, clazz));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("获取List并转换类型失败：key={}, start={}, end={}, class={}", key, start, end, clazz.getName(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 通过索引获取list中的值并转换为指定类型
     *
     * @param key   键
     * @param index 索引
     * @param clazz 返回对象类型
     * @return 值
     */
    public <T> T lGetIndexObj(String key, long index, Class<T> clazz) {
        try {
            Object obj = redisTemplate.opsForList().index(key, index);
            if (obj == null) {
                return null;
            }
            if (obj instanceof String) {
                return JacksonUtils.fromJson((String) obj, clazz);
            } else {
                String jsonStr = JacksonUtils.toJson(obj);
                return JacksonUtils.fromJson(jsonStr, clazz);
            }
        } catch (Exception e) {
            log.error("获取List索引值并转换类型失败：key={}, index={}, class={}", key, index, clazz.getName(), e);
            return null;
        }
    }
} 