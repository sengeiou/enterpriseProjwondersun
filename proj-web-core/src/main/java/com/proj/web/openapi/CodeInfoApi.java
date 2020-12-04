package com.proj.web.openapi;

import com.eaf.core.dto.APIResult;
import com.eaf.core.utils.StringUtil;
import com.ebms.service.ProductTraceService;
import com.ebp.web.openapi.AuthOpenApi;
import com.proj.appservice.CodeInfoAppService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.*;

/**
 * 编码信息查询接口
 */
@Controller(value = "projCodeInfoApi")
@RequestMapping({"openapi/proj/codeinfo"})
public class CodeInfoApi {

    private static Logger logger = LoggerFactory.getLogger(CodeInfoApi.class);

    @Resource
    private ProductTraceService productTraceService;

    @Resource(name = "sysProperties")
    private Properties sysProperties;
    @Resource
    private CodeInfoAppService codeInfoAppService;

    @RequestMapping(value = "query", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> query(String code,String eptCode, String boxCode, String pwd) {
        Map<String, Object> map = new HashMap<>();
        if (!sysProperties.getProperty("proj.getCode.pwd").equalsIgnoreCase(pwd)) {
            map.put("ErrorCode", -3);
            map.put("ErrorInfo", "接口验证码不正确");
            return map;
        }
        List<Map<String,Object>> list =new ArrayList<>();
        if(StringUtils.isNotEmpty(code)){
            //查单品码
            APIResult<Map<String, Object>> result = codeInfoAppService.getByCode(code,null);
            if(result!=null){
                list.add(result.getData());
            }
        }else if(StringUtils.isNotEmpty(eptCode)) {
            //查积分码
            APIResult<Map<String, Object>> result = codeInfoAppService.getByCode(null,eptCode);
            if(result!=null){
                list.add(result.getData());
            }
        }else if(StringUtils.isNotEmpty(boxCode)){
            //查箱码
            APIResult<List<Map<String, Object>>> result = codeInfoAppService.getByBoxCode(boxCode);
            list.addAll(result.getData());
        }else{
            map.put("ErrorCode", -2);
            map.put("ErrorInfo", "参数不正确");
            return map;
        }
        map.put("ErrorCode", 0);
        map.put("ErrorInfo", null);
        map.put("Data", list);
        return map;
    }
    @RequestMapping(value = "getByBoxCode", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getByCode(String boxCode, String pwd) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtil.isEmpty(boxCode) || StringUtil.isEmpty(pwd)) {
            map.put("ErrorCode", -2);
            map.put("ErrorInfo", "参数不正确");
            return map;
        }
        if (!sysProperties.getProperty("proj.getCode.pwd").equalsIgnoreCase(pwd)) {
            map.put("ErrorCode", -3);
            map.put("ErrorInfo", "接口验证码不正确");
            return map;
        }
        APIResult<List<Map<String, Object>>> result = codeInfoAppService.getByBoxCode(boxCode);
        if (result.getCode() != 0) {
            map.put("ErrorCode", -1);
            map.put("ErrorInfo", result.getErrMsg());
            return map;
        }
        map.put("ErrorCode", 0);
        map.put("ErrorInfo", null);
        map.put("Data", result.getData());
        return map;
    }

    @RequestMapping(value = "getByCode", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getByCode(String code, String eptCode, String pwd) {
        Map<String, Object> map = new HashMap<>();
        if ((StringUtil.isEmpty(code) && StringUtil.isEmpty(eptCode)) || StringUtil.isEmpty(pwd)) {
            map.put("ErrorCode", -2);
            map.put("ErrorInfo", "参数不正确");
            return map;
        }
        if (!sysProperties.getProperty("proj.getCode.pwd").equalsIgnoreCase(pwd)) {
            map.put("ErrorCode", -3);
            map.put("ErrorInfo", "接口验证码不正确");
            return map;
        }
        APIResult<Map<String, Object>> result = codeInfoAppService.getByCode(code, eptCode);
        if (result.getCode() != 0) {
            map.put("ErrorCode", -1);
            map.put("ErrorInfo", result.getErrMsg());
            return map;
        }
        map.put("ErrorCode", 0);
        map.put("ErrorInfo", null);
        map.put("Data", result.getData());
        return map;
    }


    /**
     * 积分状态查询
     */
    @RequestMapping(value = "queryintegralstate")
    @ResponseBody
    @AuthOpenApi("proj_codeinfo:queryintegralstate")
    public APIResult<Object> queryIntegralState(String code) {
        APIResult<Object> result = new APIResult<>();
        try {
            result = productTraceService.queryIntegralState(code);
        } catch (Exception e) {
            result.fail(500, "系统异常：" + e.getMessage());
            logger.error("/openapi/proj/codeinfo/queryintegralstate-积分状态查询失败", e);
        }
        return result;
    }
}
