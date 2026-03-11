package com.api.apiadmin.config;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统一返回结果
 * 所有接口都用这个返回
 */
@Data
@Accessors(chain = true)
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    private Result(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "操作成功");
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>(200, "操作成功");
        result.data = data;
        return result;
    }

    public static <T> Result<T> error() {
        return new Result<>(500, "操作失败");
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg);
    }

    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg);
    }

    public Result<T> setCode(int code) {
        this.code = code;
        return this;
    }

    public Result<T> setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }
}
