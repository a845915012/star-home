package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.FurnitureUserAccountDO;
import com.ruoyi.starhome.service.IFurnitureUserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户账号")
@RestController
@RequestMapping("/starhome/userAccount")
public class FurnitureUserAccountController extends BaseController {
    @Autowired
    private IFurnitureUserAccountService furnitureUserAccountService;

    @Operation(summary = "分页查询用户账号列表", description = "按用户名、公司、状态等条件分页查询用户账号")
    @Parameters({
            @Parameter(name = "username", description = "用户名", example = "alice"),
            @Parameter(name = "company", description = "公司名称", example = "星家居科技"),
            @Parameter(name = "status", description = "状态（1正常 0禁用）", example = "1"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/list")
    public TableDataInfo list(FurnitureUserAccountDO furnitureUserAccount) {
        startPage();
        List<FurnitureUserAccountDO> list = furnitureUserAccountService.selectFurnitureUserAccountList(furnitureUserAccount);
        return getDataTable(list);
    }

    @Operation(summary = "查询用户账号详情", description = "根据用户ID查询用户账号详情")
    @GetMapping("/{userId}")
    public AjaxResult getInfo(@Parameter(description = "用户ID", required = true, example = "1001") @PathVariable Long userId) {
        return success(furnitureUserAccountService.selectFurnitureUserAccountByUserId(userId));
    }

    @Operation(summary = "新增用户账号", description = "新增一条用户账号记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "用户账号对象",
            content = @Content(schema = @Schema(implementation = FurnitureUserAccountDO.class), examples = @ExampleObject(value = "{\"username\":\"alice\",\"company\":\"星家居科技\",\"password\":\"123456\",\"secretKey\":\"sec-abc\",\"status\":1,\"remark\":\"新注册\"}"))
    )
    @Log(title = "用户账号", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FurnitureUserAccountDO furnitureUserAccount) {
        return toAjax(furnitureUserAccountService.insertFurnitureUserAccount(furnitureUserAccount));
    }

    @Operation(summary = "修改用户账号", description = "根据用户ID修改用户账号记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "用户账号对象（需包含userId）",
            content = @Content(schema = @Schema(implementation = FurnitureUserAccountDO.class), examples = @ExampleObject(value = "{\"userId\":1001,\"company\":\"星家居科技（上海）\",\"status\":1,\"remark\":\"信息更新\"}"))
    )
    @Log(title = "用户账号", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FurnitureUserAccountDO furnitureUserAccount) {
        return toAjax(furnitureUserAccountService.updateFurnitureUserAccount(furnitureUserAccount));
    }

    @Operation(summary = "删除用户账号", description = "按用户ID集合批量删除用户账号")
    @Log(title = "用户账号", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@Parameter(description = "用户ID数组，逗号分隔", required = true, example = "1001,1002") @PathVariable Long[] userIds) {
        return toAjax(furnitureUserAccountService.deleteFurnitureUserAccountByUserIds(userIds));
    }
}
