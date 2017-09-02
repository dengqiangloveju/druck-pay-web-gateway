package com.druck.pay.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.druck.pay.domain.DruckPayOrder;
import com.druck.pay.domain.DruckTransactionMessage;
import com.druck.pay.domain.Merchant;
import com.druck.pay.enums.MessageStatusEnum;
import com.druck.pay.enums.PublicEnum;
import com.druck.pay.facade.DruckPayOrderFacade;
import com.druck.pay.facade.DruckTransactionMessageFacade;
import com.druck.pay.facade.MerchantFacade;
import com.druck.pay.utils.CommonUtils;
import com.druck.pay.utils.DateUtils;
import com.druck.pay.utils.MerchantApiUtil;
import com.druck.pay.utils.ResultBase;

@Controller
@RequestMapping("druck/pay")
public class GateWayController extends BaseController{
	private static final Logger logger = LogManager.getLogger(GateWayController.class);
	@Resource
	private MerchantFacade merchantFacade;
	@Resource
	private DruckPayOrderFacade druckPayOrderFacade;
	@Resource
	private DruckTransactionMessageFacade druckTransactionMessageFacade;
	
	@RequestMapping("/scanPay")
	@ResponseBody
	public ResultBase scanPay() {
		ResultBase resultBase = new ResultBase();
		Map<String , Object> paramMap = new HashMap<String , Object>();
		String payKey = getString_UrlDecode_UTF8("payKey");
		paramMap.put("payKey",payKey);
		String merchantOrderNo = getString_UrlDecode_UTF8("merchantOrderNo");
		paramMap.put("merchantOrderNo",merchantOrderNo);
		String druckOrderNo = getString_UrlDecode_UTF8("druckOrderNo");
		paramMap.put("druckOrderNo",druckOrderNo);
		String productName = getString_UrlDecode_UTF8("productName");
		paramMap.put("productName",productName);
		String orderAmount = getString_UrlDecode_UTF8("orderAmount");
		paramMap.put("orderAmount",orderAmount);
		String orderIp = getString_UrlDecode_UTF8("orderIp");
		paramMap.put("orderIp",orderIp);
		String orderPeriodStr = getString_UrlDecode_UTF8("orderPeriod");
		paramMap.put("orderPeriod",orderPeriodStr);
		String orderDateStr = getString_UrlDecode_UTF8("orderDate");
		paramMap.put("orderDate",orderDateStr);
		String orderTimeStr = getString_UrlDecode_UTF8("orderTime");
		paramMap.put("orderTime",orderTimeStr);
		String returnUrl = getString_UrlDecode_UTF8("returnUrl");
		paramMap.put("returnUrl",returnUrl);
		String notifyUrl = getString_UrlDecode_UTF8("notifyUrl");
		paramMap.put("notifyUrl",notifyUrl);
		String payWayCode = getString_UrlDecode_UTF8("payWayCode");
		paramMap.put("payWayCode",payWayCode);
		String remark = getString_UrlDecode_UTF8("remark");
		paramMap.put("remark",remark);
		String field1 = getString_UrlDecode_UTF8("field1"); // 扩展字段1
		paramMap.put("field1",field1);
		String field2 = getString_UrlDecode_UTF8("field2"); // 扩展字段2
		paramMap.put("field2",field2);
		String field3 = getString_UrlDecode_UTF8("field3"); // 扩展字段3
		paramMap.put("field3",field3);
		String field4 = getString_UrlDecode_UTF8("field4"); // 扩展字段4
		paramMap.put("field4",field4);
		String field5 = getString_UrlDecode_UTF8("field5"); // 扩展字段5
		paramMap.put("field5",field5);
		String sign = getString_UrlDecode_UTF8("sign"); // 签名
		
		if(CommonUtils.isEmpty(payKey)) {
			resultBase.setMsg("商户不存在");
			return resultBase; 
		}
		Merchant merchant = merchantFacade.findByKey(payKey);
		if(CommonUtils.isEmpty(merchant)) {
			resultBase.setMsg("商户不存在");
			return resultBase; 
		}
		if(!MerchantApiUtil.isRightSign(paramMap, merchant.getPaySecret(), sign)) {
			resultBase.setMsg("签名异常");
			return resultBase;
		}
		if(CommonUtils.isEmpty(merchantOrderNo)) {
			resultBase.setMsg("商户订单号不能为空");
			return resultBase; 
		}
		if(CommonUtils.isEmpty(orderAmount)) {
			resultBase.setMsg("支付金额不能为空");
			return resultBase; 
		}
		if(!CommonUtils.checkFloat(orderAmount)) {
			resultBase.setMsg("金额不能数字不合法");
			return resultBase; 
		}
		if(CommonUtils.isEmpty(payWayCode)) {
			resultBase.setMsg("支付方式不能为空");
			return resultBase;
		}
        
        Date orderDate = DateUtils.parseDate(orderDateStr,"yyyyMMdd");
        Date orderTime = DateUtils.parseDate(orderTimeStr,"yyyyMMddHHmmss");
        Short orderPeriod = Short.valueOf(orderPeriodStr);
        
        DruckPayOrder druckPayOrder = new DruckPayOrder();
        druckPayOrder.setMerchantNo(merchant.getId());
        druckPayOrder.setMerchantOrderNo(merchantOrderNo);
		druckPayOrder.setDruckOrderNo(druckOrderNo);
		druckPayOrder.setProductName(productName);
		druckPayOrder.setOrderAmount(Double.parseDouble(orderAmount));
		druckPayOrder.setPayAmount(Double.parseDouble(orderAmount));
		druckPayOrder.setIsRefund("101");
		druckPayOrder.setOrderIp(orderIp);
		druckPayOrder.setOrderPeriod(orderPeriod);
		druckPayOrder.setCreateTime(new Date());
		druckPayOrder.setEditTime(new Date());
		druckPayOrder.setOrderDate(orderDate);
		druckPayOrder.setOrderTime(orderTime);
		druckPayOrder.setReturnUrl(returnUrl);
		druckPayOrder.setNotifyUrl(notifyUrl);
		druckPayOrder.setStatus("1");
		druckPayOrder.setPayWayCode("TEST_PAY");
		druckPayOrder.setRecordTimes(1);
		druckPayOrder.setRemark(remark);
		druckPayOrder.setField1(field1);
		druckPayOrder.setField2(field2);
		druckPayOrder.setField3(field3);
		druckPayOrder.setField4(field4);
		druckPayOrder.setField5(field5);
		druckPayOrderFacade.save(druckPayOrder);
        resultBase.setSuccess(true);
        resultBase.setMsg("成功");
		return resultBase;
	}
	
	@RequestMapping("/notify")
	public void notify(String druckOrderNo,  String bankOrderNo, String payStatus) throws Exception {
		if("SUCCESS".equals(payStatus)) {
			//返回数据处理
			// 订单处理消息
			String messageId = CommonUtils.get32UUID();
			Map<String, String> notifyMap = new HashMap<String, String>();
			notifyMap.put("messageId", messageId);
			notifyMap.put("druckOrderNo", druckOrderNo);
			notifyMap.put("bankOrderNo", bankOrderNo);
			notifyMap.put("payStatus", payStatus);
			String messageBody = JSONObject.toJSONString(notifyMap);
			logger.info(druckOrderNo+"------------>"+messageBody);
	        DruckTransactionMessage druckTransactionMessage = new DruckTransactionMessage(messageId, messageBody, "BANK_NOTIFY");
	        druckTransactionMessage.setVersion(1);
	        druckTransactionMessage.setCreateTime(new Date());
	        druckTransactionMessage.setMessageSendTimes(0);
	        druckTransactionMessage.setAreadlyDead(PublicEnum.NO.name());
	        druckTransactionMessage.setStatus(MessageStatusEnum.WAITING_CONFIRM.name());
	        druckTransactionMessage.setRemark("银行队列");
	        druckTransactionMessageFacade.saveAndSendMessage(druckTransactionMessage);
		}
	}

}
