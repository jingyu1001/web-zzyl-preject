package com.zzyl.nursing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzyl.nursing.domain.NursingLevel;
import com.zzyl.nursing.vo.NursingLevelVo;

import java.util.List;

/**
 * 护理等级Service接口
 *
 * @author ruoyi
 * @date 2024-10-04
 */
public interface INursingLevelService extends IService<NursingLevel>
{
    /**
     * 查询护理等级列表（关联护理计划名称，name 模糊、status 精确，按 id 升序）
     *
     * @param nursingLevel 护理等级查询条件
     * @return 护理等级集合
     */
    public List<NursingLevelVo> selectNursingLevelList(NursingLevel nursingLevel);

    /**
     * 查询护理等级详情
     *
     * @param id 护理等级主键
     * @return 护理等级
     */
    public NursingLevel selectNursingLevelDetail(Long id);

    /**
     * 新增护理等级
     *
     * @param nursingLevel 护理等级
     * @return 结果
     */
    public int insertNursingLevel(NursingLevel nursingLevel);

    /**
     * 修改护理等级（支持启停部分更新：只传 id+status 时仅更新 status）
     *
     * @param nursingLevel 护理等级
     * @return 结果（0 表示 id 为空或记录不存在）
     */
    public int updateNursingLevel(NursingLevel nursingLevel);

    /**
     * 删除护理等级（被入住配置引用时拒绝删除）
     *
     * @param id 护理等级主键
     * @return 结果（0 表示 id 为空或记录不存在）
     */
    public int deleteNursingLevelById(Long id);

    /**
     * 查询全部护理等级（入住配置下拉用，全量含禁用，按 id 升序）
     *
     * @return 护理等级集合
     */
    public List<NursingLevel> selectNursingLevelAll();

    /**
     * 校验护理等级名称是否唯一（修改时自动排除自身 id）
     *
     * @param nursingLevel 护理等级
     * @return true：唯一；false：已存在
     */
    public boolean checkNameUnique(NursingLevel nursingLevel);
}
