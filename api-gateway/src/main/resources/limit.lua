--[[
    Redis 限流脚本（固定窗口计数器算法）

    @param KEYS[1] key - 限流的 Redis Key
    @param ARGV[1] maxCount - 允许的最大请求次数
    @param ARGV[2] expireSeconds - 计数器的过期时间（秒）
    @return number - 当前累计的请求次数。如果超过限制，返回当前的计数值；否则返回递增后的计数值。
--]]
local key = KEYS[1]
local maxCount = tonumber(ARGV[1])
local expireSeconds = tonumber(ARGV[2])

-- 获取当前计数并记录调试日志
local current = redis.call('get', key)
redis.log(redis.LOG_NOTICE, "maxCount = " ..tostring(maxCount))
redis.log(redis.LOG_NOTICE, "expireSeconds = " .. tostring(expireSeconds))
redis.log(redis.LOG_NOTICE, "key = " .. (key))
redis.log(redis.LOG_NOTICE, "current = " .. tostring(current))

-- 检查是否已达到限流阈值
if current then
    current = tonumber(current)
    if current > maxCount then
        return current
    end
end

-- 原子性增加计数并设置过期时间（仅在第一次创建时设置）
current = redis.call('incr', key)
if current == 1 then
    redis.call('expire', key, expireSeconds)
end

return current