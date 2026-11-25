package com.zzyl.nursing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.exception.ServiceException;
import com.zzyl.nursing.domain.NursingProject;
import com.zzyl.nursing.mapper.NursingProjectMapper;
import com.zzyl.nursing.service.INursingProjectService;
import com.zzyl.nursing.vo.NursingProjectVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 护理项目Service业务层处理
 *
 * @author ruoyi
 * @date 2025-06-02
 */
@Service
public class NursingProjectServiceImpl extends ServiceImpl<NursingProjectMapper, NursingProject> implements INursingProjectService
{
    /** 名称最大长度（与表 name varchar(100) 保持一致） */
    private static final int NAME_MAX_LENGTH = 100;

    @Autowired
    private NursingProjectMapper nursingProjectMapper;

    /**
     * 查询护理项目列表（name 模糊、status 精确，排序在 XML 中按 order_no、id 升序）
     *
     * @param nursingProject 护理项目查询条件
     * @return 护理项目集合
     */
    @Override
    public List<NursingProject> selectNursingProjectList(NursingProject nursingProject)
    {
        return nursingProjectMapper.selectNursingProjectList(nursingProject);
    }

    /**
     * 查询护理项目
     *
     * @param id 护理项目主键
     * @return 护理项目
     */
    @Override
    public NursingProject selectNursingProjectById(Long id)
    {
        return id == null ? null : getById(id);
    }

    /**
     * 新增护理项目
     *
     * @param nursingProject 护理项目
     * @return 结果
     */
    @Override
    public int insertNursingProject(NursingProject nursingProject)
    {
        if (nursingProject == null)
        {
            throw new ServiceException("护理项目不能为空");
        }
        // 名称：去首尾空格、必填、长度校验
        nursingProject.setName(trimName(nursingProject.getName()));
        if (nursingProject.getName() == null || nursingProject.getName().isEmpty())
        {
            throw new ServiceException("护理项目名称不能为空");
        }
        if (nursingProject.getName().length() > NAME_MAX_LENGTH)
        {
            throw new ServiceException("护理项目名称长度不能超过100");
        }
        // 图片：空字符串按 null 处理，避免写入空串
        if (nursingProject.getImage() != null && nursingProject.getImage().trim().isEmpty())
        {
            nursingProject.setImage(null);
        }
        // 状态未传时默认启用（与表默认值一致）
        if (nursingProject.getStatus() == null)
        {
            nursingProject.setStatus(1);
        }
        // 名称唯一：预校验 + 数据库唯一索引兜底
        if (!checkNameUnique(nursingProject))
        {
            throw new ServiceException("护理项目名称已存在");
        }
        try
        {
            // createBy/createTime/updateBy/updateTime 由 MyMetaObjectHandler 自动填充，不手工覆盖
            return save(nursingProject) ? 1 : 0;
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("护理项目名称已存在");
        }
    }

    /**
     * 修改护理项目（部分更新：null 字段由 MyBatis-Plus 默认 NOT_NULL 策略忽略，不会覆盖其它字段）
     *
     * @param nursingProject 护理项目
     * @return 结果（0 表示 id 为空或记录不存在）
     */
    @Override
    public int updateNursingProject(NursingProject nursingProject)
    {
        if (nursingProject == null || nursingProject.getId() == null)
        {
            return 0;
        }
        // 仅当本次传入名称时才校验（启停场景只传 id+status，name 为 null）
        if (nursingProject.getName() != null)
        {
            nursingProject.setName(trimName(nursingProject.getName()));
            if (nursingProject.getName().isEmpty())
            {
                throw new ServiceException("护理项目名称不能为空");
            }
            if (nursingProject.getName().length() > NAME_MAX_LENGTH)
            {
                throw new ServiceException("护理项目名称长度不能超过100");
            }
            // 名称变更时查重需排除自身 id
            if (!checkNameUnique(nursingProject))
            {
                throw new ServiceException("护理项目名称已存在");
            }
        }
        // 图片：空字符串按 null 处理（null 不参与 SET，避免覆盖原图）
        if (nursingProject.getImage() != null && nursingProject.getImage().trim().isEmpty())
        {
            nursingProject.setImage(null);
        }
        try
        {
            return updateById(nursingProject) ? 1 : 0;
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("护理项目名称已存在");
        }
    }

    /**
     * 删除护理项目
     *
     * @param id 护理项目主键
     * @return 结果（0 表示 id 为空或记录不存在）
     */
    @Override
    public int deleteNursingProjectById(Long id)
    {
        return id != null && removeById(id) ? 1 : 0;
    }

    /**
     * 查询全部护理项目下拉选项（label=名称、value=id 字符串，含禁用项，按 order_no、id 升序）
     *
     * @return 护理项目下拉选项集合
     */
    @Override
    public List<NursingProjectVo> selectNursingProjectAll()
    {
        LambdaQueryWrapper<NursingProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(NursingProject::getOrderNo).orderByAsc(NursingProject::getId);
        return list(wrapper).stream().map(nursingProject -> {
            NursingProjectVo vo = new NursingProjectVo();
            vo.setLabel(nursingProject.getName());
            vo.setValue(String.valueOf(nursingProject.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 校验护理项目名称是否唯一（修改时自动排除自身 id）
     *
     * @param nursingProject 护理项目
     * @return true：唯一；false：已存在
     */
    @Override
    public boolean checkNameUnique(NursingProject nursingProject)
    {
        if (nursingProject == null || nursingProject.getName() == null || nursingProject.getName().isEmpty())
        {
            return true;
        }
        LambdaQueryWrapper<NursingProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NursingProject::getName, nursingProject.getName());
        if (nursingProject.getId() != null)
        {
            wrapper.ne(NursingProject::getId, nursingProject.getId());
        }
        return count(wrapper) == 0;
    }

    /**
     * 名称去首尾空格
     */
    private String trimName(String name)
    {
        return name == null ? null : name.trim();
    }
}
