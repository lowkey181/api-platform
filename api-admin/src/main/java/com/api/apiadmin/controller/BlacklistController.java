package com.api.apiadmin.controller;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.Blacklist;
import com.api.apiadmin.service.BlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blacklist")
public class BlacklistController {
    @Autowired
    private BlacklistService blacklistService;

    @RequestMapping("/add")
    public SaResult addBlacklist(@RequestBody Blacklist blacklist) {
        return blacklistService.addBlacklist(blacklist);
    }
    @RequestMapping("/delete")
    public SaResult deleteBlacklist(Long id) {
        return blacklistService.deleteBlacklist(id);
    }
    @RequestMapping("/update")
    public SaResult updateBlacklist(@RequestBody Blacklist blacklist) {
        return blacklistService.updateBlacklist(blacklist);
    }
    @RequestMapping("/getPage")
    public SaResult getPage(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "5") Integer pageSize,Integer status) {
        return blacklistService.getPage(pageNum, pageSize,status);
    }

//    @RequestMapping("/selectPageAdmin")
//    public SaResult getPageAdmin(@RequestBody DeedsSelectPageDTO deedsSelectPageDTO){
//        return deedsService.selectPageAdmin(deedsSelectPageDTO.getCurrent(),
//                deedsSelectPageDTO.getSize(),
//                deedsSelectPageDTO.getTitle(),
//                deedsSelectPageDTO.getRealName(),
//                deedsSelectPageDTO.getCategoryId());
//    }
}
