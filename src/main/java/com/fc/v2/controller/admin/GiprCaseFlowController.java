package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TGiprCaseFlow;
import com.fc.v2.model.custom.GiprFlowResult;
import com.fc.v2.service.ITGiprCaseFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 专用标志使用核准办件流转单 Controller（state-machine 形状：流转入口）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "专用标志使用核准办件流转单")
@Controller
@RequestMapping("/giprCaseFlow")
public class GiprCaseFlowController extends BaseController {

    private final String prefix = "admin/giprCaseFlow";

    @Autowired
    private ITGiprCaseFlowService giprCaseFlowService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("giprCaseFlow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "专用标志使用核准办件流转单流转台账", action = "list")
    @ApiOperation(value = "流转台账", notes = "流转台账（已闭上的件照旧在名册里数得着）")
    @GetMapping("/list")
    @RequiresPermissions("giprCaseFlow:list")
    @ResponseBody
    public ResultTable list(TGiprCaseFlow record) {
        QueryWrapper<TGiprCaseFlow> queryWrapper = new QueryWrapper<TGiprCaseFlow>();
        startPage();
        com.github.pagehelper.PageInfo<TGiprCaseFlow> page =
                new com.github.pagehelper.PageInfo<TGiprCaseFlow>(giprCaseFlowService.selectTGiprCaseFlowList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "专用标志使用核准办件起件", action = "start")
    @ApiOperation(value = "起一条件", notes = "四样对上方能起件；同一件次并着进两回幂等")
    @PostMapping("/start")
    @RequiresPermissions("giprCaseFlow:start")
    @ResponseBody
    public AjaxResult start(String bizNo, String holderNo, String prodNo, String goodsKind,
                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date submitTime) {
        return toAjax(giprCaseFlowService.start(bizNo, holderNo, prodNo, goodsKind, submitTime));
    }

    @Log(title = "专用标志使用核准办件流转单推进", action = "advance")
    @ApiOperation(value = "推进一档", notes = "一次只挪一格；报面序号只供对着看，跨两格的递件不认")
    @PostMapping("/advance")
    @RequiresPermissions("giprCaseFlow:advance")
    @ResponseBody
    public AjaxResult advance(Long id, Integer formStage, String remark) {
        return toAjax(giprCaseFlowService.advance(id, formStage, remark));
    }

    @Log(title = "专用标志使用核准办件流转单回退", action = "rollback")
    @ApiOperation(value = "回退一档", notes = "一次只退一格；退掉那格的按语原样挪进追溯串末尾")
    @PostMapping("/rollback")
    @RequiresPermissions("giprCaseFlow:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String remark) {
        return toAjax(giprCaseFlowService.rollback(id, remark));
    }

    @Log(title = "专用标志使用核准办件落按语", action = "annotate")
    @ApiOperation(value = "落一句按语", notes = "同一格第二次落字只认最早那句")
    @PostMapping("/annotate")
    @RequiresPermissions("giprCaseFlow:annotate")
    @ResponseBody
    public AjaxResult annotate(Long id, String content) {
        return toAjax(giprCaseFlowService.annotate(id, content));
    }

    @Log(title = "专用标志使用核准办件核签", action = "sign")
    @ApiOperation(value = "核签岗落一笔", notes = "两名核签岗各落一笔，签齐把核发记号写回本行")
    @PostMapping("/sign")
    @RequiresPermissions("giprCaseFlow:sign")
    @ResponseBody
    public AjaxResult sign(Long id, String signer) {
        return toAjax(giprCaseFlowService.sign(id, signer));
    }

    @Log(title = "专用标志使用核准办件了结", action = "terminate")
    @ApiOperation(value = "就此了结", notes = "1 递件方撤回；2 挂靠名录产品整段并走")
    @PostMapping("/terminate")
    @RequiresPermissions("giprCaseFlow:terminate")
    @ResponseBody
    public AjaxResult terminate(Long id, Integer endKind) {
        return toAjax(giprCaseFlowService.terminate(id, endKind));
    }

    @Log(title = "专用标志使用核准办件回放", action = "replay")
    @ApiOperation(value = "倒着回放", notes = "自末一格往回一格一格演，只出结果不回写")
    @GetMapping("/replay")
    @RequiresPermissions("giprCaseFlow:replay")
    @ResponseBody
    public AjaxResult replay(Long id) {
        return toAjax(giprCaseFlowService.replay(id));
    }

    @Log(title = "专用标志使用核准办件流转单修改", action = "updateContent")
    @ApiOperation(value = "修改内容", notes = "已闭上的件不得再改")
    @PostMapping("/updateContent")
    @RequiresPermissions("giprCaseFlow:updateContent")
    @ResponseBody
    public AjaxResult updateContent(Long id, String remark) {
        return toAjax(giprCaseFlowService.updateContent(id, remark) ? 1 : 0);
    }

    @Log(title = "专用标志使用核准办件流转单删除", action = "remove")
    @ApiOperation(value = "删除单据", notes = "已闭上的件不得抽走")
    @PostMapping("/remove")
    @RequiresPermissions("giprCaseFlow:remove")
    @ResponseBody
    public AjaxResult remove(Long id) {
        return toAjax(giprCaseFlowService.remove(id) ? 1 : 0);
    }

    /** 流转回话统一转 Ajax：成了带件与格次，被拒照实说缘故 */
    private AjaxResult toAjax(GiprFlowResult r) {
        if (r == null) {
            return error();
        }
        AjaxResult ajax = r.isOk() ? success(r.getMsg()) : error(r.getMsg());
        if (r.getStage() != null) {
            ajax.put("stage", r.getStage());
        }
        if (r.getData() != null) {
            ajax.put("data", r.getData());
        }
        if (r.getTrace() != null) {
            ajax.put("trace", r.getTrace());
        }
        return ajax;
    }
}
