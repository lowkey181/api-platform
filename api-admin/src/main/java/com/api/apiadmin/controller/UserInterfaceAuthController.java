package com.api.apiadmin.controller;

import com.api.apiadmin.config.Result;
import com.api.apiadmin.entity.UserInterfaceAuth;
import com.api.apiadmin.service.UserInterfaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/userInterfaceAuth")
public class UserInterfaceAuthController {

    @Autowired
    private UserInterfaceAuthService userInterfaceAuthService;

    @RequestMapping("/insert")
    public Result insert(@RequestBody UserInterfaceAuth userInterfaceAuth)    {
        return userInterfaceAuthService.insert(userInterfaceAuth);
    }

    @RequestMapping("/update")
    public Result update(@RequestBody UserInterfaceAuth userInterfaceAuth){
        return userInterfaceAuthService.update(userInterfaceAuth);
    }
    
    @RequestMapping("/delete")
    public Result delete(@RequestParam Integer id){
        return userInterfaceAuthService.delete(id);
    }
    
    @RequestMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize){
        return userInterfaceAuthService.selectPage(pageNum, pageSize);
    }
    
    @RequestMapping("/callApi")
    public Result callApi(@RequestBody Map<String, Object> params){
        Long userId = Long.valueOf(params.get("userId").toString());
        Long interfaceId = Long.valueOf(params.get("interfaceId").toString());
        return userInterfaceAuthService.callApi(userId, interfaceId);
    }
}
