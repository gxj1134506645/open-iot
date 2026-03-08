package com.openiot.device.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis Plus 拦截器配置
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        // 注意：必须指定数据库类型，否则 count 查询可能不兼容
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}
