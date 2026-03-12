package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.ApiProduct;
import com.api.apiadmin.service.ApiProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ApiProductController {
    @Autowired
    private ApiProductService apiProductService;
    
    @RequestMapping("/insert")
    public Result insert(@RequestBody ApiProduct apiProduct) {
        return apiProductService.insert(apiProduct);
    }
    
    @RequestMapping("/update")
    public Result update(@RequestBody ApiProduct apiProduct) {
        return apiProductService.update(apiProduct);
    }
    
    @RequestMapping("/delete")
    public Result delete(@RequestParam Integer id) {
        return apiProductService.delete(id);
    }
    
    @RequestMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize,
                            @RequestParam(required = false) Long interfaceId,
                            @RequestParam(required = false) Integer status) {
        return apiProductService.selectPage(pageNum, pageSize, interfaceId, status);
    }
    
    @RequestMapping("/updateStatus")
    public Result updateStatus(@RequestParam Integer id, @RequestParam Integer status) {
        return apiProductService.updateStatus(id, status);
    }
}
