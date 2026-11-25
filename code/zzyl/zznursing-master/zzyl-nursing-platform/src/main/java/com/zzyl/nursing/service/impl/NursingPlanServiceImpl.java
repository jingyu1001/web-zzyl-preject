package com.zzyl.nursing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.exception.ServiceException;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.domain.NursingLevel;
import com.zzyl.nursing.domain.NursingPlan;
import com.zzyl.nursing.domain.NursingProjectPlan;
import com.zzyl.nursing.dto.NursingPlanDto;
import com.zzyl.nursing.mapper.NursingLevelMapper;
import com.zzyl.nursing.mapper.NursingPlanMapper;
import com.zzyl.nursing.mapper.NursingProjectPlanMapper;
import com.zzyl.nursing.service.INursingPlanService;
import com.zzyl.nursing.vo.NursingPlanVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 护理计划Service业务层处理
 *
 * @author alexis
 * @date 2025-06-02
 */
@Service
public class NursingPlanServiceImpl extends ServiceImpl<NursingPlanMapper, NursingPlan> implements INursingPlanService
{
    /** 计划名称最大长度（nursing_plan.plan_name varchar(255)） */
    private static final int PLAN_NAME_MAX_LENGTH = 255;

    /** 期望服务时间最大长度（nursing_project_plan.execute_time varchar(50)） */
    private static final int EXECUTE_TIME_MAX_LENGTH = 50;

    /** 被护理等级引用时的统一提示 */
    private static final String REFERENCED_MESSAGE = "该护理计划已被护理等级引用，无法修改或删除";

    @Autowired
    private NursingPlanMapper nursingPlanMapper;

    @Autowired
    private NursingProjectPlanMapper nursingProjectPlanMapper;

    @Autowired
    private NursingLevelMapper nursingLevelMapper;

    /**
     * 查询护理计划列表（planName 模糊、status 精确，含 count 引用数，按 sort_no、id 升序）
     *
     * @param nursingPlan 护理计划查询条件
     * @return 护理计划集合
     */
    @Override
    public List<NursingPlan> selectNursingPlanList(NursingPlan nursingPlan)
    {
        return nursingPlanMapper.selectNursingPlanList(nursingPlan);
    }

    /**
     * 查询护理计划详情（主表 + 关联子表）
     *
     * @param id 护理计划主键
     * @return 护理计划详情
     */
    @Override
    public NursingPlanVo selectNursingPlanDetail(Long id)
    {
        if (id == null)
        {
            return null;
        }
        NursingPlan nursingPlan = nursingPlanMapper.selectNursingPlanById(id);
        if (nursingPlan == null)
        {
            return null;
        }
        NursingPlanVo vo = new NursingPlanVo();
        vo.setId(nursingPlan.getId());
        vo.setSortNo(nursingPlan.getSortNo());
        vo.setPlanName(nursingPlan.getPlanName());
        vo.setStatus(nursingPlan.getStatus());
        vo.setRemark(nursingPlan.getRemark());
        vo.setCreateTime(nursingPlan.getCreateTime());
        // 子表项目：projectId/executeCycle 由 MyBatis 自动转为字符串，供前端下拉回填
        vo.setProjectPlans(nursingProjectPlanMapper.selectByNursingPlanId(id));
        return vo;
    }

    /**
     * 新增护理计划（主表 + 子表同事务）
     *
     * @param nursingPlanDto 护理计划新增参数
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertNursingPlan(NursingPlanDto nursingPlanDto)
    {
        if (nursingPlanDto == null)
        {
            throw new ServiceException("护理计划不能为空");
        }
        String planName = validatePlanName(nursingPlanDto.getPlanName(), null);
        // 子表校验放在主表落库前，校验失败不产生脏主表
        List<NursingProjectPlan> projectPlans = validateProjectPlans(nursingPlanDto.getProjectPlans());

        NursingPlan nursingPlan = new NursingPlan();
        nursingPlan.setPlanName(planName);
        nursingPlan.setSortNo(nursingPlanDto.getSortNo() == null ? 1 : nursingPlanDto.getSortNo());
        nursingPlan.setStatus(nursingPlanDto.getStatus() == null ? 1 : nursingPlanDto.getStatus());
        nursingPlan.setRemark(nursingPlanDto.getRemark());
        // R2：主表走 XML 自定义 SQL，不触发 MyMetaObjectHandler；create_time（NOT NULL 无默认）由 Service 显式设置，
        // create_by/update_by 为 bigint 列，按任务单口径不赋值（NULL，与种子一致）
        nursingPlan.setCreateTime(new Date());
        try
        {
            if (nursingPlanMapper.insertNursingPlan(nursingPlan) <= 0)
            {
                throw new ServiceException("新增护理计划失败");
            }
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("护理计划名称已存在");
        }
        // 子表批量插入（计划内项目已去重校验）
        nursingProjectPlanMapper.batchInsert(projectPlans, nursingPlan.getId());
        return 1;
    }

    /**
     * 修改护理计划：
     * 1）启停（只传 id+status）：仅更新 status，null 字段不覆盖，不重建子表，不做引用数拦截；
     * 2）整表单：引用数校验 → 名称唯一 → 更新主表 → 删除旧子表 → 批量插入新子表（同一事务）
     *
     * @param nursingPlanDto 护理计划修改参数
     * @return 结果（0 表示 id 为空或记录不存在）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateNursingPlan(NursingPlanDto nursingPlanDto)
    {
        if (nursingPlanDto == null || nursingPlanDto.getId() == null)
        {
            return 0;
        }
        Long planId = nursingPlanDto.getId();
        NursingPlan existing = nursingPlanMapper.selectNursingPlanById(planId);
        if (existing == null)
        {
            return 0;
        }
        // 启停语义：只传 id+status → 仅更新 status
        boolean enableOrDisable = nursingPlanDto.getPlanName() == null
                && nursingPlanDto.getProjectPlans() == null
                && nursingPlanDto.getStatus() != null;
        if (enableOrDisable)
        {
            return updateStatusOnly(planId, nursingPlanDto.getStatus());
        }
        // 整表单修改：被护理等级引用时禁止修改
        if (getNursingLevelRefCount(planId) > 0)
        {
            throw new ServiceException(REFERENCED_MESSAGE);
        }
        String planName = validatePlanName(nursingPlanDto.getPlanName(), planId);
        List<NursingProjectPlan> projectPlans = validateProjectPlans(nursingPlanDto.getProjectPlans());

        NursingPlan nursingPlan = new NursingPlan();
        nursingPlan.setId(planId);
        nursingPlan.setPlanName(planName);
        nursingPlan.setSortNo(nursingPlanDto.getSortNo());
        nursingPlan.setStatus(nursingPlanDto.getStatus());
        nursingPlan.setRemark(nursingPlanDto.getRemark());
        // R2：update_time 显式设置；create_by/update_by（bigint）不赋值
        nursingPlan.setUpdateTime(new Date());
        try
        {
            nursingPlanMapper.updateNursingPlan(nursingPlan);
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("护理计划名称已存在");
        }
        // 重建子表：先删旧子表再批量插入，与主表更新同一事务
        nursingProjectPlanMapper.deleteByNursingPlanId(planId);
        nursingProjectPlanMapper.batchInsert(projectPlans, planId);
        return 1;
    }

    /**
     * 删除护理计划（引用数校验 → 先删子表 → 再删主表，同一事务）
     *
     * @param id 护理计划主键
     * @return 结果（0 表示 id 为空或记录不存在）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteNursingPlanById(Long id)
    {
        if (id == null)
        {
            return 0;
        }
        NursingPlan existing = nursingPlanMapper.selectNursingPlanById(id);
        if (existing == null)
        {
            return 0;
        }
        if (getNursingLevelRefCount(id) > 0)
        {
            throw new ServiceException(REFERENCED_MESSAGE);
        }
        nursingProjectPlanMapper.deleteByNursingPlanId(id);
        return nursingPlanMapper.deleteNursingPlanById(id) > 0 ? 1 : 0;
    }

    /**
     * 查询全部护理计划（护理等级下拉用，含禁用项，按 sort_no、id 升序）
     *
     * @return 护理计划集合
     */
    @Override
    public List<NursingPlan> selectNursingPlanAll()
    {
        LambdaQueryWrapper<NursingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(NursingPlan::getSortNo).orderByAsc(NursingPlan::getId);
        return list(wrapper);
    }

    /**
     * 校验护理计划名称是否唯一（修改时自动排除自身 id）
     *
     * @param nursingPlan 护理计划
     * @return true：唯一；false：已存在
     */
    @Override
    public boolean checkPlanNameUnique(NursingPlan nursingPlan)
    {
        if (nursingPlan == null || StringUtils.isEmpty(nursingPlan.getPlanName()))
        {
            return true;
        }
        LambdaQueryWrapper<NursingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NursingPlan::getPlanName, nursingPlan.getPlanName().trim());
        if (nursingPlan.getId() != null)
        {
            wrapper.ne(NursingPlan::getId, nursingPlan.getId());
        }
        return count(wrapper) == 0;
    }

    /**
     * 仅更新状态（启停场景使用，使用 XML 部分更新，null 字段不进入 SET）
     */
    private int updateStatusOnly(Long planId, Integer status)
    {
        NursingPlan nursingPlan = new NursingPlan();
        nursingPlan.setId(planId);
        nursingPlan.setStatus(status);
        nursingPlan.setUpdateTime(new Date());
        return nursingPlanMapper.updateNursingPlan(nursingPlan) > 0 ? 1 : 0;
    }

    /**
     * 统计护理计划被护理等级引用的次数
     */
    private long getNursingLevelRefCount(Long planId)
    {
        LambdaQueryWrapper<NursingLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NursingLevel::getPlanId, planId);
        return nursingLevelMapper.selectCount(wrapper);
    }

    /**
     * 名称校验：trim + 必填 + 长度 + 唯一（修改时排除自身 id）
     */
    private String validatePlanName(String planName, Long excludeId)
    {
        String name = planName == null ? null : planName.trim();
        if (StringUtils.isEmpty(name))
        {
            throw new ServiceException("护理计划名称不能为空");
        }
        if (name.length() > PLAN_NAME_MAX_LENGTH)
        {
            throw new ServiceException("护理计划名称长度不能超过255");
        }
        NursingPlan query = new NursingPlan();
        query.setPlanName(name);
        query.setId(excludeId);
        if (!checkPlanNameUnique(query))
        {
            throw new ServiceException("护理计划名称已存在");
        }
        return name;
    }

    /**
     * 子表校验：非空、字段必填、projectId 计划内去重、周期/频次范围
     */
    private List<NursingProjectPlan> validateProjectPlans(List<NursingProjectPlan> projectPlans)
    {
        if (projectPlans == null || projectPlans.isEmpty())
        {
            throw new ServiceException("护理项目不能为空");
        }
        Set<Long> projectIds = new HashSet<>();
        for (NursingProjectPlan projectPlan : projectPlans)
        {
            if (projectPlan == null || projectPlan.getProjectId() == null)
            {
                throw new ServiceException("护理项目不能为空");
            }
            if (!projectIds.add(projectPlan.getProjectId()))
            {
                throw new ServiceException("请勿选择重复的护理项目");
            }
            String executeTime = projectPlan.getExecuteTime() == null ? null : projectPlan.getExecuteTime().trim();
            if (StringUtils.isEmpty(executeTime))
            {
                throw new ServiceException("期望服务时间不能为空");
            }
            if (executeTime.length() > EXECUTE_TIME_MAX_LENGTH)
            {
                throw new ServiceException("期望服务时间长度不能超过50");
            }
            projectPlan.setExecuteTime(executeTime);
            if (projectPlan.getExecuteCycle() == null
                    || projectPlan.getExecuteCycle() < 0 || projectPlan.getExecuteCycle() > 2)
            {
                throw new ServiceException("执行周期不能为空，且只能为0天/1周/2月");
            }
            if (projectPlan.getExecuteFrequency() == null
                    || projectPlan.getExecuteFrequency() < 1 || projectPlan.getExecuteFrequency() > 7)
            {
                throw new ServiceException("执行频次不能为空，且范围为1~7");
            }
        }
        return projectPlans;
    }
}
