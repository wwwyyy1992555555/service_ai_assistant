package com.myproject.service_ai_assistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.common.PasswordUtil;
import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.entity.TenantConfig;
import com.myproject.service_ai_assistant.entity.TenantInfo;
import com.myproject.service_ai_assistant.entity.User;
import com.myproject.service_ai_assistant.exception.BusinessException;
import com.myproject.service_ai_assistant.mapper.TenantConfigMapper;
import com.myproject.service_ai_assistant.mapper.TenantInfoMapper;
import com.myproject.service_ai_assistant.mapper.UserMapper;
import com.myproject.service_ai_assistant.service.EmailService;
import com.myproject.service_ai_assistant.service.TenantInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private UserMapper userMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private EmailService emailService;
    
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
            throw new BusinessException(ResultCode.TENANT_CODE_EXISTS);
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
            throw new BusinessException(ResultCode.TENANT_NOT_FOUND);
        }
        
        // 2. 检查租户状态（只能删除禁用的租户）
        if (tenant.getStatus() == 1) {
            throw new BusinessException(ResultCode.TENANT_DELETE_FAILED);
        }
        
        // 3. 逻辑删除租户（MyBatis-Plus 自动设置 deleted=1）
        this.removeById(tenantId);
        log.info("【删除租户】租户逻辑删除成功：tenantId={}", tenantId);
        
        // 4. 清除 Redis 缓存
        String cacheKey = TENANT_STATUS_CACHE_PREFIX + tenantId;
        redisTemplate.delete(cacheKey);
        log.info("【删除租户】缓存已清除：tenantId={}", tenantId);
        
        // 5. 关联数据处理说明（软删除模式下保留数据）：
        // - 用户数据 (user_info): 保留，通过 tenant.status=0 控制不可登录
        // - 租户配置 (tenant_config): 保留，便于恢复租户时复用
        // - 知识库/对话记录/反馈: 保留，作为历史数据审计追溯
        // 注意：所有查询接口已通过 MyBatis-Plus 的 @TableLogic 自动过滤 deleted=1 的租户
        
        log.info("【删除租户】完成：tenantId={}", tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenantStatus(Long tenantId, Integer status) {
        log.info("【更新租户状态】tenantId={}, status={}", tenantId, status);
        
        // 1. 检查租户是否存在
        TenantInfo tenant = this.getById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ResultCode.TENANT_NOT_FOUND);
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
            throw new BusinessException(ResultCode.TENANT_NOT_FOUND);
        }
        
        // 2. 更新非空字段（MyBatis-Plus 默认只更新非 null 字段）
        boolean success = this.updateById(tenantInfo);
        if (!success) {
            throw new BusinessException(ResultCode.TENANT_UPDATE_FAILED);
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

    /**
     * 发送注册验证码
     */
    @Override
    public void sendVerifyCode(String contactEmail) {
        log.info("【发送注册验证码】开始：contactEmail={}", contactEmail);
            
        // 1. 校验邮箱唯一性
        validateEmailUnique(contactEmail.toLowerCase());
            
        // 2. 生成 6 位数字验证码
        String verifyCode = String.format("%06d", (int) (Math.random() * 1000000));
        log.info("【发送注册验证码】生成验证码：verifyCode={}", verifyCode);
            
        // 3. 存储到 Redis（5 分钟有效期）
        String redisKey = "register:verify:" + contactEmail;
        try {
            redisTemplate.opsForValue().set(redisKey, verifyCode, 5, TimeUnit.MINUTES);
            log.info("【发送注册验证码】验证码已存入Redis：key={}", redisKey);
        } catch (Exception e) {
            log.error("【发送注册验证码】Redis存储失败", e);
            throw new BusinessException(ResultCode.REDIS_OPERATION_FAILED);
        }
            
        // 4. 发送邮件
        try {
            log.info("【发送注册验证码】开始发送邮件...");
            emailService.sendVerifyCodeEmail(contactEmail, verifyCode);
            log.info("【发送注册验证码】邮件发送成功：contactEmail={}, verifyCode={}", contactEmail, verifyCode);
        } catch (Exception e) {
            log.error("【发送注册验证码】邮件发送失败：contactEmail={}, error={}", contactEmail, e.getMessage(), e);
            throw new BusinessException(ResultCode.EMAIL_SEND_FAILED);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String registerTenant(String tenantName, String contactPerson, String contactPhone,
                                 String contactEmail, String verifyCode, String adminUsername, String adminPassword) {
        log.info("【租户自助注册】tenantName={}, contactPerson={}", tenantName, contactPerson);
    
        // 1. 校验邮箱验证码
        String redisKey = "register:verify:" + contactEmail;
        String cachedCode = (String) redisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null) {
            throw new BusinessException(ResultCode.VERIFY_CODE_EXPIRED);
        }
        if (!cachedCode.equals(verifyCode)) {
            throw new BusinessException(ResultCode.VERIFY_CODE_ERROR);
        }
            
        // 2. 校验企业名称唯一性
        validateTenantNameUnique(tenantName.trim());
        
        // 2.1 校验邮箱唯一性
        validateEmailUnique(contactEmail.toLowerCase());
    
        // 3. 校验用户名格式
        if (adminUsername == null || adminUsername.trim().isEmpty()) {
            throw new BusinessException(ResultCode.USERNAME_EMPTY);
        }
        if (adminUsername.length() < 3 || adminUsername.length() > 20) {
            throw new BusinessException(ResultCode.USERNAME_LENGTH_INVALID);
        }
        if (!adminUsername.matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException(ResultCode.USERNAME_FORMAT_INVALID);
        }
    
        // 4. 生成唯一的 tenant_code（格式：TENANT_时间戳_4 位随机数）
        String tenantCode = generateUniqueTenantCode();
        log.info("【租户自助注册】生成 tenant_code: {}", tenantCode);
    
        // 5. 创建租户信息（status=1 直接启用）
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setTenantName(tenantName);
        tenantInfo.setTenantCode(tenantCode);
        tenantInfo.setIndustryType(0); // 默认行业类型：其他
        tenantInfo.setContactPerson(contactPerson);
        tenantInfo.setContactPhone(contactPhone);
        tenantInfo.setContactEmail(contactEmail.toLowerCase());
        tenantInfo.setStatus(1); // 直接启用
        this.save(tenantInfo);
        log.info("【租户自助注册】租户创建成功：id={}, tenantCode={}, status=1(已启用)", tenantInfo.getId(), tenantCode);
    
        // 6. 初始化租户配置
        try {
            TenantConfig config = new TenantConfig();
            config.setTenantId(tenantInfo.getId());
            config.setCompanyName(tenantName);
            config.setWelcomeMessage("您好，请问有什么可以帮您？");
            config.setThemeColor("#1890ff");
            config.setServicePhone(contactPhone);
            config.setServiceTime("工作时间：周一至周日 9:00-17:00");
            tenantConfigMapper.insert(config);
            log.info("【租户自助注册】租户配置初始化成功");
        } catch (Exception e) {
            log.error("【租户自助注册】租户配置初始化失败", e);
            throw new BusinessException(ResultCode.TENANT_CREATE_FAILED);
        }
    
        // 7. 创建默认管理员账号（role_level=1 普通管理员）
        User adminUser = new User();
        adminUser.setTenantId(tenantInfo.getId());
        adminUser.setUsername(adminUsername);
        adminUser.setPassword(PasswordUtil.encrypt(adminPassword));
        adminUser.setRealName(contactPerson);
        adminUser.setRoleLevel(LevelCode.ROLE_LEVEL_ADMIN); // 普通管理员
        adminUser.setStatus(1);
        adminUser.setCreatedTime(LocalDateTime.now());
        userMapper.insert(adminUser);
        log.info("【租户自助注册】管理员账号创建成功：userId={}", adminUser.getId());
    
        // 8. 清除验证码
        redisTemplate.delete(redisKey);
            
        log.info("【租户自助注册】完成：tenantCode={}", tenantCode);
        return tenantCode;
    }

    /**
     * 生成唯一的租户编码
     * 格式：TENANT_yyyyMMddHHmmss_xxxx（4位随机数）
     */
    private String generateUniqueTenantCode() {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 10000);
        String code = String.format("TENANT_%s_%04d", timestamp, random);

        // 确保唯一性（极低概率重复，但做双重保障）
        LambdaQueryWrapper<TenantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantInfo::getTenantCode, code);
        while (this.count(wrapper) > 0) {
            random = (int) (Math.random() * 10000);
            code = String.format("TENANT_%s_%04d", timestamp, random);
            wrapper.eq(TenantInfo::getTenantCode, code);
        }

        return code;
    }
    
    /**
     * 校验企业名称唯一性（排除已删除的记录）
     */
    private void validateTenantNameUnique(String tenantName) {
        LambdaQueryWrapper<TenantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantInfo::getTenantName, tenantName)
               .eq(TenantInfo::getDeleted, 0);
        Long count = this.count(wrapper);
        if (count > 0) {
            log.warn("【企业名称校验失败】已注册：tenantName={}", tenantName);
            throw new BusinessException(ResultCode.TENANT_NAME_REGISTERED);
        }
    }
    
    /**
     * 校验邮箱唯一性（排除已删除的记录，不区分大小写）
     */
    private void validateEmailUnique(String contactEmail) {
        String emailLower = contactEmail.toLowerCase();
        LambdaQueryWrapper<TenantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantInfo::getContactEmail, emailLower)
               .eq(TenantInfo::getDeleted, 0);
        Long count = this.count(wrapper);
        if (count > 0) {
            log.warn("【邮箱校验失败】邮箱已注册：contactEmail={}", contactEmail);
            throw new BusinessException(ResultCode.EMAIL_REGISTERED);
        }
    }
}
