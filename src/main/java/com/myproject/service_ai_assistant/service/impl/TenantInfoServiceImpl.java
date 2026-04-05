package com.myproject.service_ai_assistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.entity.TenantConfig;
import com.myproject.service_ai_assistant.entity.TenantInfo;
import com.myproject.service_ai_assistant.exception.BusinessException;
import com.myproject.service_ai_assistant.mapper.TenantConfigMapper;
import com.myproject.service_ai_assistant.mapper.TenantInfoMapper;
import com.myproject.service_ai_assistant.service.TenantInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 租户信息服务实现类
 */
@Slf4j
@Service
public class TenantInfoServiceImpl extends ServiceImpl<TenantInfoMapper, TenantInfo> implements TenantInfoService {

    @Autowired
    private TenantConfigMapper tenantConfigMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /** 租户状态缓存前缀 */
    private static final String TENANT_STATUS_CACHE_PREFIX = "tenant:status:";

    @Override
    public Page<TenantInfo> getTenantList(Integer current, Integer size, String keyword, Integer status, Integer industryType) {
        log.info("【获取租户列表】current={}, size={}, keyword={}, status={}, industryType={}", current, size, keyword, status, industryType);
        
        Page<TenantInfo> page = new Page<>(current, size);
        LambdaQueryWrapper<TenantInfo> wrapper = new LambdaQueryWrapper<>();
        
        // 搜索条件
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(TenantInfo::getTenantCode, keyword)
                    .or()
                    .like(TenantInfo::getTenantName, keyword));
        }
        
        // 状态筛选
        if (status != null) {
            wrapper.eq(TenantInfo::getStatus, status);
        }
        
        // 行业类型筛选
        if (industryType != null) {
            wrapper.eq(TenantInfo::getIndustryType, industryType);
        }
        
        wrapper.orderByDesc(TenantInfo::getCreatedTime);
        
        Page<TenantInfo> result = this.page(page, wrapper);
        log.info("【获取租户列表成功】total={}", result.getTotal());
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantInfo createTenant(TenantInfo tenantInfo) {
        log.info("【创建租户】tenantCode={}, tenantName={}", tenantInfo.getTenantCode(), tenantInfo.getTenantName());
        
        // 1. 检查租户编码是否已存在
        LambdaQueryWrapper<TenantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantInfo::getTenantCode, tenantInfo.getTenantCode());
        Long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException(400, "租户编码已存在");
        }
        
        // 2. 设置默认值
        if (tenantInfo.getStatus() == null) {
            tenantInfo.setStatus(1); // 默认启用
        }
        
        // 3. 保存租户信息
        this.save(tenantInfo);
        log.info("【创建租户】保存成功：id={}", tenantInfo.getId());
        
        // 4. 自动创建租户配置
        try {
            TenantConfig config = new TenantConfig();
            config.setTenantId(tenantInfo.getId());
            config.setCompanyName(tenantInfo.getTenantName());
            config.setWelcomeMessage("您好，请问有什么可以帮您？");
            config.setThemeColor("#1890ff");
            tenantConfigMapper.insert(config);
            log.info("【创建租户】租户配置创建成功");
        } catch (Exception e) {
            log.error("【创建租户】租户配置创建失败", e);
            // 不影响主流程，只记录日志
        }
        
        return tenantInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTenant(Long tenantId) {
        log.info("【删除租户】tenantId={}", tenantId);
        
        // 1. 检查租户是否存在
        TenantInfo tenant = this.getById(tenantId);
        if (tenant == null) {
            throw new BusinessException(404, "租户不存在");
        }
        
        // 2. 检查租户状态（只能删除禁用的租户）
        if (tenant.getStatus() == 1) {
            throw new BusinessException(400, "只能删除已禁用的租户，请先禁用该租户");
        }
        
        // 3. 逻辑删除租户
        this.removeById(tenantId);
        log.info("【删除租户】删除成功：tenantId={}", tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenantStatus(Long tenantId, Integer status) {
        log.info("【更新租户状态】tenantId={}, status={}", tenantId, status);
        
        // 1. 检查租户是否存在
        TenantInfo tenant = this.getById(tenantId);
        if (tenant == null) {
            throw new BusinessException(404, "租户不存在");
        }
        
        // 2. 更新状态
        TenantInfo updateTenant = new TenantInfo();
        updateTenant.setId(tenantId);
        updateTenant.setStatus(status);
        this.updateById(updateTenant);
        
        // 3. 清除 Redis 缓存（下次访问时重新检查）
        String cacheKey = TENANT_STATUS_CACHE_PREFIX + tenantId;
        redisTemplate.delete(cacheKey);
        log.info("【更新租户状态】缓存已清除：tenantId={}", tenantId);
        
        log.info("【更新租户状态】更新成功：tenantId={}, status={}", tenantId, status);
    }

    @Override
    public TenantInfo getTenantById(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenant(TenantInfo tenantInfo) {
        log.info("【更新租户信息】id={}, tenantCode={}", tenantInfo.getId(), tenantInfo.getTenantCode());
        
        // 1. 检查租户是否存在
        TenantInfo existing = this.getById(tenantInfo.getId());
        if (existing == null) {
            throw new BusinessException(404, "租户不存在");
        }
        
        // 2. 更新非空字段（MyBatis-Plus 默认只更新非 null 字段）
        boolean success = this.updateById(tenantInfo);
        if (!success) {
            throw new BusinessException(500, "更新租户信息失败");
        }
        
        log.info("【更新租户信息】更新成功：id={}", tenantInfo.getId());
    }
    
    /**
     * 获取租户状态（带缓存）
     */
    public TenantInfo getTenantInfoWithCache(Long tenantId) {
        String cacheKey = TENANT_STATUS_CACHE_PREFIX + tenantId;
        
        // 1. 先查缓存
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null) {
            log.debug("【租户状态缓存命中】tenantId={}", tenantId);
            // 从缓存中重建 TenantInfo 对象（只包含必要的字段）
            TenantInfo tenant = new TenantInfo();
            tenant.setId(tenantId);
            // 兼容不同的类型：String 或 Integer
            int status;
            if (cachedObj instanceof String) {
                status = Integer.parseInt((String) cachedObj);
            } else if (cachedObj instanceof Integer) {
                status = (Integer) cachedObj;
            } else {
                status = Integer.parseInt(cachedObj.toString());
            }
            tenant.setStatus(status);
            return tenant;
        }
        
        // 2. 缓存未命中，查数据库
        TenantInfo tenant = this.getById(tenantId);
        if (tenant != null) {
            // 3. 写入缓存（TTL 1小时，状态变更时会被清除）
            // 统一使用 String 类型存储
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(tenant.getStatus()), 1, TimeUnit.HOURS);
            log.info("【租户状态缓存更新】tenantId={}, status={}", tenantId, tenant.getStatus());
        }
        
        return tenant;
    }
}
