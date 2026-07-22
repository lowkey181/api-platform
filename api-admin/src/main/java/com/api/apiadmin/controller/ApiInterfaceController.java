package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.ApiInterface;
import com.api.apiadmin.entity.ApiInterfaceParam;
import com.api.apiadmin.service.ApiInterfaceParamService;
import com.api.apiadmin.service.ApiInterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/apiInterface")
public class ApiInterfaceController {

    @Autowired
    private ApiInterfaceService apiInterfaceService;

    @Autowired
    private ApiInterfaceParamService apiInterfaceParamService;

    @RequestMapping("/insert")
    public Result insert(@RequestBody ApiInterface apiInterface) {
        return apiInterfaceService.insert(apiInterface);
    }

    @RequestMapping("/update")
    public Result update(@RequestBody ApiInterface apiInterface) {
        return apiInterfaceService.update(apiInterface);
    }

    @RequestMapping("/delete")
    public Result delete(@RequestParam Integer id) {
        return apiInterfaceService.delete(id);
    }

    @RequestMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false) Integer status,
                             @RequestParam(required = false) Long authorId) {
        return apiInterfaceService.selectPage(pageNum, pageSize, status, authorId);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        return apiInterfaceService.getById(id);
    }

    @GetMapping("/params/{interfaceId}")
    public Result getParams(@PathVariable Long interfaceId) {
        List<ApiInterfaceParam> requestParams = apiInterfaceParamService.getByInterfaceId(interfaceId, "request");
        List<ApiInterfaceParam> responseParams = apiInterfaceParamService.getByInterfaceId(interfaceId, "response");
        return Result.ok(Map.of(
                "requestParams", requestParams,
                "responseParams", responseParams
        ));
    }

    @PostMapping("/params/{interfaceId}")
    public Result saveParams(@PathVariable Long interfaceId,
                             @RequestBody List<ApiInterfaceParam> params) {
        for (ApiInterfaceParam p : params) {
            p.setInterfaceId(interfaceId);
        }
        apiInterfaceParamService.saveParams(interfaceId, params);
        return Result.ok("参数保存成功");
    }
}
