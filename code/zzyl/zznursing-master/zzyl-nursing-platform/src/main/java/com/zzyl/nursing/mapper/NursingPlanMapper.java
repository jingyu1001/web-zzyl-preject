package com.zzyl.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzyl.nursing.domain.NursingPlan;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 护理计划Mapper接口
 *
 * @author alexis
 * @date 2025-06-02
 */
@Mapper
public interface NursingPlanMapper extends BaseMapper<NursingPlan>
{
    /**
     * 查询护理计划列表（planName 模糊、status 精确，含被护理等级引用次数 count，按 sort_no、id 升序）
     *
     * @param nursingPlan 护理计划查询条件
     * @return 护理计划集合
     */
    public List<NursingPlan> selectNursingPlanList(NursingPlan nursingPlan);

    /**
     * 查询护理计划
     *
     * @param id 护理计划主键
     * @return 护理计划
     */
    public NursingPlan selectNursingPlanById(Long id);

    /**
     * 新增护理计划
     *
     * @param nursingPlan 护理计划
     * @return 结果
     */
    public int insertNursingPlan(NursingPlan nursingPlan);

    /**
     * 修改护理计划
     *
     * @param nursingPlan 护理计划
     * @return 结果
     */
    public int updateNursingPlan(NursingPlan nursingPlan);

    /**
     * 删除护理计划
     *
     * @param id 护理计划主键
     * @return 结果
     */
    public int deleteNursingPlanById(Long id);
}
