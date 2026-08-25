package com.zading.todoapi.model;

/**
 * 用户角色。
 *
 * <p>角色名称保存到数据库时使用枚举名称，例如 USER、ADMIN；
 * 交给 Spring Security 时再转换成 ROLE_USER、ROLE_ADMIN。</p>
 */
public enum UserRole {
    USER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
