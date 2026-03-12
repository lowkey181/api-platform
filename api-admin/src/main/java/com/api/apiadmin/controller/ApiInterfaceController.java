package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.ApiInterface;
import com.api.apiadmin.service.ApiInterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apiInterface")
public class ApiInterfaceController {
    @Autowired
    private ApiInterfaceService apiInterfaceService;
    
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
                               @RequestParam(required = false) Integer status) {
        return apiInterfaceService.selectPage(pageNum, pageSize, status);
    }
}
