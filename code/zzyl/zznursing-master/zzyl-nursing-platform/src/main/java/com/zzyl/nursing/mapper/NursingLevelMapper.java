package com.zzyl.nursing.mapper;

import java.util.List;
import com.zzyl.nursing.domain.NursingLevel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzyl.nursing.vo.NursingLevelVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 护理等级Mapper接口
 *
 * @author ruoyi
 * @date 2024-10-04
 */
@Mapper
public interface NursingLevelMapper extends BaseMapper<NursingLevel> {

    /**
     * 查询护理等级列表（关联护理计划名称，name 模糊、status 精确）
     *
     * @param nursingLevel 护理等级查询条件
     * @return 护理等级集合
     */
    public List<NursingLevelVo> selectNursingLevelVoList(NursingLevel nursingLevel);

    /**
     * 查询护理等级
     *
     * @param id 护理等级主键
     * @return 护理等级
     */
    public NursingLevel selectNursingLevelById(Long id);

    /**
     * 新增护理等级
     *
     * @param nursingLevel 护理等级
     * @return 结果
     */
    public int insertNursingLevel(NursingLevel nursingLevel);

    /**
     * 修改护理等级
     *
     * @param nursingLevel 护理等级
     * @return 结果
     */
    public int updateNursingLevel(NursingLevel nursingLevel);

    /**
     * 删除护理等级
     *
     * @param id 护理等级主键
     * @return 结果
     */
    public int deleteNursingLevelById(Long id);
}
