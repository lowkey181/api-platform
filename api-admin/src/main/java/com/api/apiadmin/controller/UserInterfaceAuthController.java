package com.api.apiadmin.controller;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.UserInterfaceAuth;
import com.api.apiadmin.service.UserInterfaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/userInterfaceAuth")
public class UserInterfaceAuthController {

    @Autowired
    private UserInterfaceAuthService userInterfaceAuthService;

    @RequestMapping("/insert")
    public SaResult insert(UserInterfaceAuth userInterfaceAuth)    {
        return userInterfaceAuthService.insert(userInterfaceAuth);
    }

    @RequestMapping("/update")
    public SaResult update(UserInterfaceAuth userInterfaceAuth){
        return userInterfaceAuthService.update(userInterfaceAuth);
    }
    @RequestMapping("/delete")
    public SaResult delete(Integer id){
        return userInterfaceAuthService.delete(id);
    }
    @RequestMapping("/selectPage")
    public SaResult selectPage(Integer pageNum, Integer pageSize){
        return userInterfaceAuthService.selectPage(pageNum, pageSize);
    }
    @RequestMapping("/callApi")
    public SaResult callApi(@RequestBody Map<String, Object> params){
        Long userId = Long.valueOf(params.get("userId").toString());
        Long interfaceId = Long.valueOf(params.get("interfaceId").toString());
        return userInterfaceAuthService.callApi(userId,interfaceId);
    }

}
