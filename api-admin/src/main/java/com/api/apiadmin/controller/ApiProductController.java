package com.api.apiadmin.controller;

import com.api.apiadmin.entity.ApiProduct;
import com.api.apiadmin.service.ApiProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ApiProductController {
    @Autowired
    private ApiProductService apiProductService;
    @RequestMapping("/insert")
    public Object insert(ApiProduct apiProduct) {
        return apiProductService.insert(apiProduct);
    }
    @RequestMapping("/update")
    public Object update(ApiProduct apiProduct) {
        return apiProductService.update(apiProduct);
    }
    @RequestMapping("/delete")
    public Object delete(Integer id) {
        return apiProductService.delete(id);
    }
    @RequestMapping("/selectPage")
    public Object selectPage(Integer pageNum, Integer pageSize,Long interfaceId,Integer  status) {
        return apiProductService.selectPage(pageNum, pageSize, interfaceId, status);
    }
    @RequestMapping("/updateStatus")
    public Object updateStatus(Integer id, Integer status) {
        return apiProductService.updateStatus(id, status);
    }

}
