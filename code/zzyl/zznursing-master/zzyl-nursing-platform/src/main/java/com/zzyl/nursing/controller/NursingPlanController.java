package com.zzyl.nursing.controller;

import com.zzyl.common.annotation.Log;
import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.enums.BusinessType;
import com.zzyl.nursing.domain.NursingPlan;
import com.zzyl.nursing.dto.NursingPlanDto;
import com.zzyl.nursing.service.INursingPlanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 护理计划Controller
 *
 * @author alexis
 * @date 2025-06-02
 */
@RestController
@RequestMapping("/nursing/plan")
@Api(tags = "护理计划相关接口")
public class NursingPlanController extends BaseController
{
    @Autowired
    private INursingPlanService nursingPlanService;

    /**
     * 查询护理计划列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:plan:list')")
    @GetMapping("/list")
    @ApiOperation("查询护理计划列表")
    public TableDataInfo list(NursingPlan nursingPlan)
    {
        startPage();
        List<NursingPlan> list = nursingPlanService.selectNursingPlanList(nursingPlan);
        return getDataTable(list);
    }

    /**
     * 查询所有护理计划（护理等级下拉用）
     */
    @PreAuthorize("@ss.hasPermi('nursing:plan:list')")
    @GetMapping("/all")
    @ApiOperation("查询所有护理计划（下拉选项）")
    public AjaxResult all()
    {
        return success(nursingPlanService.selectNursingPlanAll());
    }

    /**
     * 获取护理计划详细信息（含护理项目子表）
     */
    @PreAuthorize("@ss.hasPermi('nursing:plan:query')")
    @GetMapping(value = "/{id}")
    @ApiOperation("获取护理计划详细信息")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(nursingPlanService.selectNursingPlanDetail(id));
    }

    /**
     * 新增护理计划
     */
    @PreAuthorize("@ss.hasPermi('nursing:plan:add')")
    @Log(title = "护理计划", businessType = BusinessType.INSERT)
    @PostMapping
    @ApiOperation("新增护理计划")
    public AjaxResult add(@RequestBody NursingPlanDto nursingPlanDto)
    {
        return toAjax(nursingPlanService.insertNursingPlan(nursingPlanDto));
    }

    /**
     * 修改护理计划（支持启停部分更新：只传 id+status）
     */
    @PreAuthorize("@ss.hasPermi('nursing:plan:edit')")
    @Log(title = "护理计划", businessType = BusinessType.UPDATE)
    @PutMapping
    @ApiOperation("修改护理计划")
    public AjaxResult edit(@RequestBody NursingPlanDto nursingPlanDto)
    {
        int rows = nursingPlanService.updateNursingPlan(nursingPlanDto);
        return rows > 0 ? success() : error("护理计划不存在");
    }

    /**
     * 删除护理计划
     */
    @PreAuthorize("@ss.hasPermi('nursing:plan:remove')")
    @Log(title = "护理计划", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @ApiOperation("删除护理计划")
    public AjaxResult remove(@PathVariable("id") Long id)
    {
        int rows = nursingPlanService.deleteNursingPlanById(id);
        return rows > 0 ? success() : error("护理计划不存在");
    }
}
