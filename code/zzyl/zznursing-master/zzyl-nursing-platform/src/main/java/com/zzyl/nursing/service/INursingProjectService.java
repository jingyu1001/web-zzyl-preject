package com.zzyl.nursing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzyl.nursing.domain.NursingProject;
import com.zzyl.nursing.vo.NursingProjectVo;

import java.util.List;

/**
 * 护理项目Service接口
 *
 * @author ruoyi
 * @date 2025-06-02
 */
public interface INursingProjectService extends IService<NursingProject>
{
    /**
     * 查询护理项目列表（name 模糊、status 精确，按 order_no、id 升序）
     *
     * @param nursingProject 护理项目查询条件
     * @return 护理项目集合
     */
    public List<NursingProject> selectNursingProjectList(NursingProject nursingProject);

    /**
     * 查询护理项目
     *
     * @param id 护理项目主键
     * @return 护理项目
     */
    public NursingProject selectNursingProjectById(Long id);

    /**
     * 新增护理项目
     *
     * @param nursingProject 护理项目
     * @return 结果
     */
    public int insertNursingProject(NursingProject nursingProject);

    /**
     * 修改护理项目（部分更新：仅更新非 null 字段）
     *
     * @param nursingProject 护理项目
     * @return 结果
     */
    public int updateNursingProject(NursingProject nursingProject);

    /**
     * 删除护理项目
     *
     * @param id 护理项目主键
     * @return 结果
     */
    public int deleteNursingProjectById(Long id);

    /**
     * 查询全部护理项目下拉选项（label=名称、value=id 字符串，按 order_no、id 升序）
     *
     * @return 护理项目下拉选项集合
     */
    public List<NursingProjectVo> selectNursingProjectAll();

    /**
     * 校验护理项目名称是否唯一（修改时自动排除自身 id）
     *
     * @param nursingProject 护理项目
     * @return true：唯一；false：已存在
     */
    public boolean checkNameUnique(NursingProject nursingProject);
}
