package com.zzyl.nursing.controller;

import com.zzyl.common.annotation.Log;
import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.enums.BusinessType;
import com.zzyl.nursing.domain.NursingProject;
import com.zzyl.nursing.service.INursingProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 护理项目Controller
 *
 * @author ruoyi
 * @date 2025-06-02
 */
@RestController
@RequestMapping("/nursing/project")
@Api(tags = "护理项目相关接口")
public class NursingProjectController extends BaseController
{
    @Autowired
    private INursingProjectService nursingProjectService;

    /**
     * 查询护理项目列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:project:list')")
    @GetMapping("/list")
    @ApiOperation("查询护理项目列表")
    public TableDataInfo list(NursingProject nursingProject)
    {
        startPage();
        List<NursingProject> list = nursingProjectService.selectNursingProjectList(nursingProject);
        return getDataTable(list);
    }

    /**
     * 查询全部护理项目（下拉选项，护理计划/照护安排页依赖）
     */
    @PreAuthorize("@ss.hasPermi('nursing:project:list')")
    @GetMapping("/all")
    @ApiOperation("查询全部护理项目（下拉选项）")
    public AjaxResult all()
    {
        return success(nursingProjectService.selectNursingProjectAll());
    }

    /**
     * 获取护理项目详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:project:query')")
    @GetMapping(value = "/{id}")
    @ApiOperation("获取护理项目详细信息")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(nursingProjectService.selectNursingProjectById(id));
    }

    /**
     * 新增护理项目
     */
    @PreAuthorize("@ss.hasPermi('nursing:project:add')")
    @Log(title = "护理项目", businessType = BusinessType.INSERT)
    @PostMapping
    @ApiOperation("新增护理项目")
    public AjaxResult add(@RequestBody NursingProject nursingProject)
    {
        return toAjax(nursingProjectService.insertNursingProject(nursingProject));
    }

    /**
     * 修改护理项目（支持部分更新，启停只传 id+status）
     */
    @PreAuthorize("@ss.hasPermi('nursing:project:edit')")
    @Log(title = "护理项目", businessType = BusinessType.UPDATE)
    @PutMapping
    @ApiOperation("修改护理项目")
    public AjaxResult edit(@RequestBody NursingProject nursingProject)
    {
        int rows = nursingProjectService.updateNursingProject(nursingProject);
        return rows > 0 ? success() : error("护理项目不存在");
    }

    /**
     * 删除护理项目
     */
    @PreAuthorize("@ss.hasPermi('nursing:project:remove')")
    @Log(title = "护理项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @ApiOperation("删除护理项目")
    public AjaxResult remove(@PathVariable("id") Long id)
    {
        int rows = nursingProjectService.deleteNursingProjectById(id);
        return rows > 0 ? success() : error("护理项目不存在");
    }
}
