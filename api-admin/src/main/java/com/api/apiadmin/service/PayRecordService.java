package com.api.apiadmin.service;

import com.api.apiadmin.config.SaResult;
import com.api.apiadmin.entity.PayRecord;
import com.api.apiadmin.mapper.PayRecordMapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayRecordService extends ServiceImpl<PayRecordMapper, PayRecord> {
    @Autowired
    private PayRecordMapper payRecordMapper;
    @Transactional(rollbackFor = Exception.class)
    public SaResult save1(PayRecord payRecord) {
        if (ObjectUtils.isEmpty(payRecord)) {
            return SaResult.error("支付记录不能为空");
        }
        try {
            int result = payRecordMapper.insert(payRecord);
            if (result > 0) {
                return SaResult.ok("保存成功");
            } else {
                return SaResult.error("保存失败");
            }
        } catch (Exception e) {
            log.error("保存支付记录异常", e);
            return SaResult.error("保存异常：" + e.getMessage());
        }
    }
}
