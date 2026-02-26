package com.openiot.common.security.service;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限校验实现类
 * 用于返回用户的权限和角色列表
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回用户的权限码集合
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型
     * @return 权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // TODO: 从数据库或缓存中查询用户权限
        // 目前返回空列表，后续在 tenant-service 中实现
        return new ArrayList<>();
    }

    /**
     * 返回用户的角色标识集合
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型
     * @return 角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // TODO: 从数据库或缓存中查询用户角色
        // 目前返回空列表，后续在 tenant-service 中实现
        return new ArrayList<>();
    }
}
