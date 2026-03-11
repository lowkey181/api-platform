package com.api.apiadmin;

import cn.hutool.crypto.digest.BCrypt;

public class pw {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw("123456", BCrypt.gensalt()));
    }
}
