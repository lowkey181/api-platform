local key = KEYS[1]
local maxCount = tonumber(ARGV[1])
local expireSeconds = tonumber(ARGV[2])

local current = redis.call('get', key)
redis.log(redis.LOG_NOTICE, "maxCount = " ..tostring(maxCount))
redis.log(redis.LOG_NOTICE, "expireSeconds = " .. tostring(expireSeconds))
redis.log(redis.LOG_NOTICE, "key = " .. (key))
redis.log(redis.LOG_NOTICE, "current = " .. tostring(current))
if current then
    current = tonumber(current)
    if current > maxCount then
        return current
    end
end

current = redis.call('incr', key)
if current == 1 then
    redis.call('expire', key, expireSeconds)
end

return current