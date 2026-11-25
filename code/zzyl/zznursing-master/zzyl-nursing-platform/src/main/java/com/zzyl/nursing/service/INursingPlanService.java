package com.zzyl.nursing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzyl.nursing.domain.NursingPlan;
import com.zzyl.nursing.dto.NursingPlanDto;
import com.zzyl.nursing.vo.NursingPlanVo;

import java.util.List;

/**
 * 护理计划Service接口
 *
 * @author alexis
 * @date 2025-06-02
 */
public interface INursingPlanService extends IService<NursingPlan>
{
    /**
     * 查询护理计划列表（含 count 引用数，按 sort_no、id 升序）
     *
     * @param nursingPlan 护理计划查询条件
     * @return 护理计划集合
     */
    public List<NursingPlan> selectNursingPlanList(NursingPlan nursingPlan);

    /**
     * 查询护理计划详情（含关联的护理项目子表）
     *
     * @param id 护理计划主键
     * @return 护理计划详情
     */
    public NursingPlanVo selectNursingPlanDetail(Long id);

    /**
     * 新增护理计划（主表 + 子表同事务）
     *
     * @param nursingPlanDto 护理计划新增参数
     * @return 结果
     */
    public int insertNursingPlan(NursingPlanDto nursingPlanDto);

    /**
     * 修改护理计划（整表单全量更新主表 + 重建子表；启停仅更新 status，不重建子表）
     *
     * @param nursingPlanDto 护理计划修改参数
     * @return 结果（0 表示 id 为空或记录不存在）
     */
    public int updateNursingPlan(NursingPlanDto nursingPlanDto);

    /**
     * 删除护理计划（主表 + 子表同事务）
     *
     * @param id 护理计划主键
     * @return 结果（0 表示 id 为空或记录不存在）
     */
    public int deleteNursingPlanById(Long id);

    /**
     * 查询全部护理计划（护理等级下拉用，按 sort_no、id 升序，含禁用项）
     *
     * @return 护理计划集合
     */
    public List<NursingPlan> selectNursingPlanAll();

    /**
     * 校验护理计划名称是否唯一（修改时自动排除自身 id）
     *
     * @param nursingPlan 护理计划
     * @return true：唯一；false：已存在
     */
    public boolean checkPlanNameUnique(NursingPlan nursingPlan);
}
