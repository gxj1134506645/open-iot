package com.openiot.common.security.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.Data;

/**
 * 租户上下文
 * 用于在请求链路中传递租户信息
 */
public class TenantContext {

    /**
     * 使用 TransmittableThreadLocal 支持线程池场景
     */
    private static final TransmittableThreadLocal<TenantInfo> CONTEXT = new TransmittableThreadLocal<>();

    /**
     * 设置租户信息
     */
    public static void setTenant(TenantInfo tenant) {
        CONTEXT.set(tenant);
    }

    /**
     * 获取租户信息
     */
    public static TenantInfo getTenant() {
        return CONTEXT.get();
    }

    /**
     * 获取租户 ID
     */
    public static String getTenantId() {
        TenantInfo tenant = CONTEXT.get();
        return tenant != null ? tenant.getTenantId() : null;
    }

    /**
     * 获取用户 ID
     */
    public static String getUserId() {
        TenantInfo tenant = CONTEXT.get();
        return tenant != null ? tenant.getUserId() : null;
    }

    /**
     * 获取用户角色
     */
    public static String getRole() {
        TenantInfo tenant = CONTEXT.get();
        return tenant != null ? tenant.getRole() : null;
    }

    /**
     * 清除租户信息
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 判断是否为平台管理员
     */
    public static boolean isPlatformAdmin() {
        TenantInfo tenant = CONTEXT.get();
        return tenant != null && "ADMIN".equals(tenant.getRole());
    }

    /**
     * 判断是否为租户管理员
     */
    public static boolean isTenantAdmin() {
        TenantInfo tenant = CONTEXT.get();
        return tenant != null && "TENANT_ADMIN".equals(tenant.getRole());
    }

    /**
     * 租户信息
     */
    @Data
    public static class TenantInfo {
        /**
         * 租户 ID
         */
        private String tenantId;

        /**
         * 用户 ID
         */
        private String userId;

        /**
         * 用户名
         */
        private String username;

        /**
         * 角色
         * ADMIN - 平台管理员
         * TENANT_ADMIN - 租户管理员
         */
        private String role;
    }
}
