package com.proj.web.openapi;

import com.eaf.core.dto.APIResult;
import com.eaf.core.utils.StringUtil;
import com.proj.appservice.WonderSunOpenAppService;
import com.proj.dto.ProductionCodeQueryDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Properties;

/**
 * 编码信息查询接口
 */
@Controller
@RequestMapping({"openapi/proj/codequery"})
public class WonderSunOpenApi {
    @Resource(name = "sysProperties")
    private Properties sysProperties;
    @Resource
    private WonderSunOpenAppService appService;

    @RequestMapping(value = "query")
    @ResponseBody
    public Object query(String Code, String password) throws IOException {
        APIResult<ProductionCodeQueryDTO> apiResult = new APIResult<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (!sysProperties.getProperty("proj.query.pwd").equalsIgnoreCase(password)) {
                apiResult.fail(403, "接口验证码不正确");
            } else if (StringUtil.isEmpty(Code)) {
                apiResult.fail(403, "接口参数不正确");
            } else {
                apiResult = appService.getProductionCodeByQRId(Code);
            }
            return objectMapper.writeValueAsString(apiResult);
        } catch (Exception e) {
            return objectMapper.writeValueAsString(apiResult.fail(500, e.getMessage()));
        }
    }

    @RequestMapping(value = "queryMaterialFile")
    @ResponseBody
    public Object queryMaterialFile(String Code, String password) throws IOException {
        APIResult<ProductionCodeQueryDTO> apiResult = new APIResult<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (!sysProperties.getProperty("proj.query.pwd").equalsIgnoreCase(password)) {
                apiResult.fail(403, "接口验证码不正确");
            } else if (StringUtil.isEmpty(Code)) {
                apiResult.fail(403, "接口参数不正确");
            } else {
                apiResult = appService.getProductionCodeByQRId(Code);
            }
            return objectMapper.writeValueAsString(apiResult);
        } catch (Exception e) {
            return objectMapper.writeValueAsString(apiResult.fail(500, e.getMessage()));
        }
    }
}
