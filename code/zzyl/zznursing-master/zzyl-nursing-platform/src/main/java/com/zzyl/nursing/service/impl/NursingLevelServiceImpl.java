package com.zzyl.nursing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.exception.ServiceException;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.domain.CheckInConfig;
import com.zzyl.nursing.domain.NursingLevel;
import com.zzyl.nursing.mapper.CheckInConfigMapper;
import com.zzyl.nursing.mapper.NursingLevelMapper;
import com.zzyl.nursing.mapper.NursingPlanMapper;
import com.zzyl.nursing.service.INursingLevelService;
import com.zzyl.nursing.vo.NursingLevelVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 护理等级Service业务层处理
 *
 * @author ruoyi
 * @date 2024-10-04
 */
@Service
public class NursingLevelServiceImpl extends ServiceImpl<NursingLevelMapper, NursingLevel> implements INursingLevelService
{
    /** 等级名称最大长度（nursing_level.name varchar(50)） */
    private static final int NAME_MAX_LENGTH = 50;

    /** 等级说明最大长度（nursing_level.description varchar(255)） */
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    @Autowired
    private NursingLevelMapper nursingLevelMapper;

    @Autowired
    private NursingPlanMapper nursingPlanMapper;

    @Autowired
    private CheckInConfigMapper checkInConfigMapper;

    /**
     * 查询护理等级列表（关联护理计划名称，name 模糊、status 精确，按 id 升序）
     *
     * @param nursingLevel 护理等级查询条件
     * @return 护理等级集合
     */
    @Override
    public List<NursingLevelVo> selectNursingLevelList(NursingLevel nursingLevel)
    {
        return nursingLevelMapper.selectNursingLevelVoList(nursingLevel);
    }

    /**
     * 查询护理等级详情
     *
     * @param id 护理等级主键
     * @return 护理等级
     */
    @Override
    public NursingLevel selectNursingLevelDetail(Long id)
    {
        return id == null ? null : nursingLevelMapper.selectNursingLevelById(id);
    }

    /**
     * 新增护理等级
     *
     * @param nursingLevel 护理等级
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertNursingLevel(NursingLevel nursingLevel)
    {
        if (nursingLevel == null)
        {
            throw new ServiceException("护理等级不能为空");
        }
        nursingLevel.setName(validateName(nursingLevel.getName(), null));
        validatePlanId(nursingLevel.getPlanId());
        if (nursingLevel.getFee() == null)
        {
            throw new ServiceException("护理费用不能为空");
        }
        nursingLevel.setDescription(validateDescription(nursingLevel.getDescription()));
        // 状态必填，未传时默认启用；取值不做限制
        if (nursingLevel.getStatus() == null)
        {
            nursingLevel.setStatus(1);
        }
        // R2 口径：主表走 XML 自定义 SQL，不触发 MyMetaObjectHandler；create_time 显式设置，create_by/update_by 不赋值（NULL）
        nursingLevel.setCreateTime(new Date());
        try
        {
            return nursingLevelMapper.insertNursingLevel(nursingLevel) > 0 ? 1 : 0;
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("护理等级名称已存在");
        }
    }

    /**
     * 修改护理等级：启停（只传 id+status）仅更新 status；整表单校验后全量更新（null 字段不覆盖）
     *
     * @param nursingLevel 护理等级
     * @return 结果（0 表示 id 为空或记录不存在）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateNursingLevel(NursingLevel nursingLevel)
    {
        if (nursingLevel == null || nursingLevel.getId() == null)
        {
            return 0;
        }
        NursingLevel existing = nursingLevelMapper.selectNursingLevelById(nursingLevel.getId());
        if (existing == null)
        {
            return 0;
        }
        boolean enableOrDisable = isEnableOrDisable(nursingLevel);
        if (!enableOrDisable)
        {
            // 整表单：按必填项校验（修改时名称唯一排除自身 id）
            nursingLevel.setName(validateName(nursingLevel.getName(), nursingLevel.getId()));
            validatePlanId(nursingLevel.getPlanId());
            if (nursingLevel.getFee() == null)
            {
                throw new ServiceException("护理费用不能为空");
            }
            nursingLevel.setDescription(validateDescription(nursingLevel.getDescription()));
            if (nursingLevel.getStatus() == null)
            {
                throw new ServiceException("状态不能为空");
            }
        }
        nursingLevel.setUpdateTime(new Date());
        try
        {
            return nursingLevelMapper.updateNursingLevel(nursingLevel) > 0 ? 1 : 0;
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("护理等级名称已存在");
        }
    }

    /**
     * 删除护理等级（被入住配置引用时拒绝删除）
     *
     * @param id 护理等级主键
     * @return 结果（0 表示 id 为空或记录不存在）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteNursingLevelById(Long id)
    {
        if (id == null)
        {
            return 0;
        }
        NursingLevel existing = nursingLevelMapper.selectNursingLevelById(id);
        if (existing == null)
        {
            return 0;
        }
        // lead 决策：被入住配置（check_in_config.nursing_level_id）引用时禁止删除；修改/启停放行
        LambdaQueryWrapper<CheckInConfig> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(CheckInConfig::getNursingLevelId, id);
        if (checkInConfigMapper.selectCount(checkWrapper) > 0)
        {
            throw new ServiceException("该护理等级已被入住配置引用，无法删除");
        }
        return nursingLevelMapper.deleteNursingLevelById(id) > 0 ? 1 : 0;
    }

    /**
     * 查询全部护理等级（入住配置下拉用，全量含禁用，按 id 升序）
     *
     * @return 护理等级集合
     */
    @Override
    public List<NursingLevel> selectNursingLevelAll()
    {
        LambdaQueryWrapper<NursingLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(NursingLevel::getId);
        return list(wrapper);
    }

    /**
     * 校验护理等级名称是否唯一（修改时自动排除自身 id）
     *
     * @param nursingLevel 护理等级
     * @return true：唯一；false：已存在
     */
    @Override
    public boolean checkNameUnique(NursingLevel nursingLevel)
    {
        if (nursingLevel == null || StringUtils.isEmpty(nursingLevel.getName()))
        {
            return true;
        }
        LambdaQueryWrapper<NursingLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NursingLevel::getName, nursingLevel.getName().trim());
        if (nursingLevel.getId() != null)
        {
            wrapper.ne(NursingLevel::getId, nursingLevel.getId());
        }
        return count(wrapper) == 0;
    }

    /**
     * 是否为启停语义：只传 id+status
     */
    private boolean isEnableOrDisable(NursingLevel nursingLevel)
    {
        return nursingLevel.getName() == null
                && nursingLevel.getPlanId() == null
                && nursingLevel.getFee() == null
                && nursingLevel.getDescription() == null
                && nursingLevel.getStatus() != null;
    }

    /**
     * 名称校验：trim + 必填 + 长度 + 唯一（修改时排除自身 id）
     */
    private String validateName(String name, Long excludeId)
    {
        String trimmed = name == null ? null : name.trim();
        if (StringUtils.isEmpty(trimmed))
        {
            throw new ServiceException("护理等级名称不能为空");
        }
        if (trimmed.length() > NAME_MAX_LENGTH)
        {
            throw new ServiceException("护理等级名称长度不能超过50");
        }
        NursingLevel query = new NursingLevel();
        query.setName(trimmed);
        query.setId(excludeId);
        if (!checkNameUnique(query))
        {
            throw new ServiceException("护理等级名称已存在");
        }
        return trimmed;
    }

    /**
     * 护理计划校验：必填 + 存在性（不校验计划启用状态，避免禁用的既有计划无法编辑）
     */
    private void validatePlanId(Long planId)
    {
        if (planId == null)
        {
            throw new ServiceException("护理计划不能为空");
        }
        if (nursingPlanMapper.selectNursingPlanById(planId) == null)
        {
            throw new ServiceException("护理计划不存在");
        }
    }

    /**
     * 等级说明校验：必填 + 长度
     */
    private String validateDescription(String description)
    {
        String trimmed = description == null ? null : description.trim();
        if (StringUtils.isEmpty(trimmed))
        {
            throw new ServiceException("等级说明不能为空");
        }
        if (trimmed.length() > DESCRIPTION_MAX_LENGTH)
        {
            throw new ServiceException("等级说明长度不能超过255");
        }
        return trimmed;
    }
}
