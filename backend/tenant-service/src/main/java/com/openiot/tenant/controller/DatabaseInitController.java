package com.openiot.tenant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.tenant.entity.SysUser;
import com.openiot.tenant.entity.Tenant;
import com.openiot.tenant.mapper.SysUserMapper;
import com.openiot.tenant.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 数据库初始化控制器（临时）
 * 仅用于开发和测试环境
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/init")
@RequiredArgsConstructor
public class DatabaseInitController {

    private final SysUserMapper sysUserMapper;
    private final TenantMapper tenantMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 初始化测试数据
     */
    @PostMapping("/test-data")
    public ApiResponse<String> initTestData() {
        log.info("开始初始化测试数据...");

        try {
            // 1. 检查是否已存在平台管理员
            LambdaQueryWrapper<SysUser> adminWrapper = new LambdaQueryWrapper<>();
            adminWrapper.eq(SysUser::getUsername, "admin");
            SysUser existingAdmin = sysUserMapper.selectOne(adminWrapper);

            if (existingAdmin == null) {
                // 创建平台管理员
                SysUser admin = new SysUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRealName("平台管理员");
                admin.setRole("ADMIN");
                admin.setStatus("1");
                admin.setDeleteFlag("0");
                sysUserMapper.insert(admin);
                log.info("创建平台管理员成功");
            } else {
                // 更新密码
                existingAdmin.setPassword(passwordEncoder.encode("admin123"));
                sysUserMapper.updateById(existingAdmin);
                log.info("更新平台管理员密码成功");
            }

            // 2. 检查是否已存在默认租户
            LambdaQueryWrapper<Tenant> tenantWrapper = new LambdaQueryWrapper<>();
            tenantWrapper.eq(Tenant::getTenantCode, "default");
            Tenant existingTenant = tenantMapper.selectOne(tenantWrapper);

            if (existingTenant == null) {
                // 创建默认租户
                Tenant tenant = new Tenant();
                tenant.setTenantCode("default");
                tenant.setTenantName("默认租户");
                tenant.setContactEmail("admin@default.com");
                tenant.setStatus("1");
                tenant.setDeleteFlag("0");
                tenantMapper.insert(tenant);
                log.info("创建默认租户成功");
                existingTenant = tenant;
            }

            // 3. 创建租户管理员
            if (existingTenant != null) {
                LambdaQueryWrapper<SysUser> tenantAdminWrapper = new LambdaQueryWrapper<>();
                tenantAdminWrapper.eq(SysUser::getUsername, "tenant_admin");
                SysUser existingTenantAdmin = sysUserMapper.selectOne(tenantAdminWrapper);

                if (existingTenantAdmin == null) {
                    SysUser tenantAdmin = new SysUser();
                    tenantAdmin.setTenantId(existingTenant.getId());
                    tenantAdmin.setUsername("tenant_admin");
                    tenantAdmin.setPassword(passwordEncoder.encode("admin123"));
                    tenantAdmin.setRealName("租户管理员");
                    tenantAdmin.setRole("TENANT_ADMIN");
                    tenantAdmin.setStatus("1");
                    tenantAdmin.setDeleteFlag("0");
                    sysUserMapper.insert(tenantAdmin);
                    log.info("创建租户管理员成功");
                } else {
                    // 更新密码和租户ID
                    existingTenantAdmin.setTenantId(existingTenant.getId());
                    existingTenantAdmin.setPassword(passwordEncoder.encode("admin123"));
                    sysUserMapper.updateById(existingTenantAdmin);
                    log.info("更新租户管理员成功");
                }
            }

            return ApiResponse.success("测试数据初始化成功！");

        } catch (Exception e) {
            log.error("初始化测试数据失败", e);
            return ApiResponse.error("初始化失败: " + e.getMessage());
        }
    }

    /**
     * 查询当前用户数据（用于验证）
     */
    @GetMapping("/users")
    public ApiResponse<Object> listUsers() {
        return ApiResponse.success(sysUserMapper.selectList(null));
    }

    /**
     * 查询当前租户数据（用于验证）
     */
    @GetMapping("/tenants")
    public ApiResponse<Object> listTenants() {
        return ApiResponse.success(tenantMapper.selectList(null));
    }
}
