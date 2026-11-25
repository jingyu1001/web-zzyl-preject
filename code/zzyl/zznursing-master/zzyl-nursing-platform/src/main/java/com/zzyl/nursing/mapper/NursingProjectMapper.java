package com.zzyl.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzyl.nursing.domain.NursingProject;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 护理项目Mapper接口
 *
 * @author ruoyi
 * @date 2025-06-02
 */
@Mapper
public interface NursingProjectMapper extends BaseMapper<NursingProject>
{
    /**
     * 查询护理项目列表（name 模糊、status 精确，按 order_no、id 升序）
     *
     * @param nursingProject 护理项目查询条件
     * @return 护理项目集合
     */
    public List<NursingProject> selectNursingProjectList(NursingProject nursingProject);
}
