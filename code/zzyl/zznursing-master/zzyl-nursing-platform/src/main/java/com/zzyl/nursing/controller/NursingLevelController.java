package com.zzyl.nursing.controller;

import com.zzyl.common.annotation.Log;
import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.enums.BusinessType;
import com.zzyl.nursing.domain.NursingLevel;
import com.zzyl.nursing.service.INursingLevelService;
import com.zzyl.nursing.vo.NursingLevelVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 护理等级Controller
 *
 * @author ruoyi
 * @date 2024-10-04
 */
@RestController
@RequestMapping("/nursing/level")
@Api(tags = "护理等级相关接口")
public class NursingLevelController extends BaseController
{
    @Autowired
    private INursingLevelService nursingLevelService;

    /**
     * 查询护理等级列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:level:list')")
    @GetMapping("/list")
    @ApiOperation("查询护理等级列表")
    public TableDataInfo list(NursingLevel nursingLevel)
    {
        startPage();
        List<NursingLevelVo> list = nursingLevelService.selectNursingLevelList(nursingLevel);
        return getDataTable(list);
    }

    /**
     * 查询所有护理等级（入住配置下拉用）
     */
    @PreAuthorize("@ss.hasPermi('nursing:level:list')")
    @GetMapping("/all")
    @ApiOperation("查询所有护理等级（下拉选项）")
    public AjaxResult all()
    {
        return success(nursingLevelService.selectNursingLevelAll());
    }

    /**
     * 获取护理等级详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:level:query')")
    @GetMapping(value = "/{id}")
    @ApiOperation("获取护理等级详细信息")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(nursingLevelService.selectNursingLevelDetail(id));
    }

    /**
     * 新增护理等级
     */
    @PreAuthorize("@ss.hasPermi('nursing:level:add')")
    @Log(title = "护理等级", businessType = BusinessType.INSERT)
    @PostMapping
    @ApiOperation("新增护理等级")
    public AjaxResult add(@RequestBody NursingLevel nursingLevel)
    {
        return toAjax(nursingLevelService.insertNursingLevel(nursingLevel));
    }

    /**
     * 修改护理等级（支持启停部分更新：只传 id+status）
     */
    @PreAuthorize("@ss.hasPermi('nursing:level:edit')")
    @Log(title = "护理等级", businessType = BusinessType.UPDATE)
    @PutMapping
    @ApiOperation("修改护理等级")
    public AjaxResult edit(@RequestBody NursingLevel nursingLevel)
    {
        int rows = nursingLevelService.updateNursingLevel(nursingLevel);
        return rows > 0 ? success() : error("护理等级不存在");
    }

    /**
     * 删除护理等级
     */
    @PreAuthorize("@ss.hasPermi('nursing:level:remove')")
    @Log(title = "护理等级", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @ApiOperation("删除护理等级")
    public AjaxResult remove(@PathVariable("id") Long id)
    {
        int rows = nursingLevelService.deleteNursingLevelById(id);
        return rows > 0 ? success() : error("护理等级不存在");
    }
}
