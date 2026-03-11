package com.api.apiadmin.controller;


import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.api.apiadmin.config.AlipayTemplate;
import com.api.apiadmin.entity.OrderInfo;
import com.api.apiadmin.entity.PayRecord;
import com.api.apiadmin.entity.UserRecharge;
import com.api.apiadmin.service.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.alipay.api.AlipayConstants.SIGN_TYPE_RSA2;

/**
 * 支付宝接口
 */
@Slf4j
@RestController
@RequestMapping("/alipay")
public class AliPayController {


    @Resource
    AlipayTemplate alipayTemplate;
    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private PayRecordService payRecordService;
    @Autowired
    private UserInterfaceAuthService userInterfaceAuthService;
    @Autowired
    private ApiProductService apiProductService;
    @Autowired
    private UserRechargeService userRechargeService;

    @GetMapping(value = "/pay", produces = "text/html")
    @ResponseBody
    public String pay(@RequestParam OrderInfo  order) throws AlipayApiException {
        order.setUserId((Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
//        OrderInfo order = new OrderInfo();
//        order.setId(284125577335818L);//out_trade_no商家订单号
////        long id1 = System.currentTimeMillis() * 1000 + new Random().nextInt(1000);
////        order.setId(id1);
//        order.setUserId(1299504405947L);
//        order.setInterfaceId(29438194472934L);
//        order.setTotalAmount(BigDecimal.valueOf(100.00));
//        order.setPaymentMethod("支付宝");
        // 幂等性校验：如果订单已经是已支付，直接返回success
        if (order.getPayStatus() == 1) {
            log.info("订单{}已支付，无需重复处理", order.getOrderNo());
            return "success";
        }
        return alipayTemplate.pay(order);
    }

    @PostMapping("/notify")
    public String payNotify(HttpServletRequest request) throws Exception {
        // 1. 获取所有回调参数
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();

        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            if (values != null && values.length > 0) {
                params.put(name, values[0]);
            }
        }

        log.info("========= 收到支付宝异步回调 =========");
        log.info("回调参数: {}", params);

        String tradeStatus = params.get("trade_status");

        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            try {
                // 打印公钥信息用于调试
                String publicKey = alipayTemplate.getAlipayPublicKey();
                log.info("支付宝公钥长度: {}", publicKey != null ? publicKey.length() : "null");
                log.info("支付宝公钥前50字符: {}", publicKey != null && publicKey.length() > 50 ? publicKey.substring(0, 50) : publicKey);
                
                // 2. 验签 - rsaCheckV1会自动处理sign和sign_type参数
                boolean signVerified = AlipaySignature.rsaCheckV1(
                        params,
                        publicKey,
                        "utf-8",
                        SIGN_TYPE_RSA2
                );

                if (signVerified) {
                    log.info("支付宝验签成功！");

                    // 3. 按照文档要求进行二次校验
                    if (!alipayTemplate.getAppId().equals(params.get("app_id"))) {
                        log.error("验签失败：appId 不匹配");
                        return "failure";
                    }

                    // TODO: 校验 total_amount 是否与订单金额一致（必须加！防止金额被篡改）
                    String outTradeNo = params.get("out_trade_no"); // 你的订单号
                    String alipayTotalAmount = params.get("total_amount"); // 支付宝回调的金额
                    // 从数据库查询原订单，对比金额是否一致
                    OrderInfo dbOrder = orderInfoService.getByOrderNo(outTradeNo);
                    if (dbOrder == null || !alipayTotalAmount.equals(dbOrder.getTotalAmount().toString())) {
                        if (dbOrder != null) {
                            log.error("订单金额不匹配，原订单金额：{}，回调金额：{}", dbOrder.getTotalAmount(), alipayTotalAmount);
                        }
                        return "failure";
                    }

                    log.info("商户订单号: {}, 支付宝交易号: {}, 交易金额: {}",
                            params.get("out_trade_no"), params.get("trade_no"), params.get("total_amount"));

                    // 1. 更新订单状态：未支付→已支付，填充支付时间、支付宝交易号
                    dbOrder.setPayStatus(1); // 1=已支付（对应system_dict的订单状态）
                    dbOrder.setPayTime(new Date()); // 支付完成时间
                    dbOrder.setTradeNo(params.get("trade_no")); // 支付宝交易号（方便对账）
                    orderInfoService.updateById(dbOrder);

                    // 2. 写入支付记录pay_record
                    PayRecord payRecord = new PayRecord();
                    payRecord.setOrderNo(outTradeNo);
                    payRecord.setUserId(dbOrder.getUserId());
                    payRecord.setPayAmount(new BigDecimal(alipayTotalAmount));
                    payRecord.setPayType("支付宝"); // 支付方式：支付宝
                    payRecord.setPayStatus(1); // 1=支付成功
                    payRecord.setCreateTime(new Date());
                    payRecord.setTradeNo(params.get("trade_no"));
                    payRecordService.save1(payRecord);

                    //3. 给用户增加接口次数（更新user_interface_auth）
                    userInterfaceAuthService.addUserInterfaceCount(dbOrder);

                    // 4. 写入用户充值记录user_recharge
                    UserRecharge userRecharge = new UserRecharge();
                    userRecharge.setUserId(dbOrder.getUserId());
                    userRecharge.setInterfaceId(dbOrder.getInterfaceId());
                    userRecharge.setRechargeAmount(BigDecimal.valueOf(apiProductService.getCallCountById(dbOrder.getProductId())));
                    userRecharge.setOrderId(dbOrder.getId());
                    userRecharge.setCreateTime(new Date());
                    userRechargeService.save(userRecharge);

                    return "success";
                } else {
                    log.error("支付宝验签失败！请检查公钥是否正确");
                    log.error("配置的appId: {}, 回调的appId: {}", alipayTemplate.getAppId(), params.get("app_id"));
                    return "failure";
                }
            } catch (AlipayApiException e) {
                log.error("验签异常: {}", e.getMessage(), e);
                log.error("异常详情: ", e);
                return "failure";
            }
        }


        return "success";
    }
}

