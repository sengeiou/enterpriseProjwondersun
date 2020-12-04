package com.proj.web.openapi;

import com.eaf.core.dto.APIResult;
import com.eaf.core.utils.StringUtil;
import com.ebp.web.openapi.AuthOpenApi;
import com.proj.appservice.WonderSunOpenAppService;
import com.proj.dto.ProductionCodeQueryDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by arlenChen on 2019/12/17.
 * 单据同步
 *
 * @author arlenChen
 */
@Controller
@RequestMapping({"openapi/proj/billsync"})
public class BillSyncApi {
    @Resource(name = "sysProperties")
    private Properties sysProperties;
    @Resource
    private WonderSunOpenAppService appService;

    /**
     * 新增单据
     *
     * @param billData 单据
     * @return 新增结果
     */
    @RequestMapping(value = "createbill")
    @ResponseBody
    @AuthOpenApi("proj_billsync:createbill")
    public APIResult<String> createBill(@RequestParam() String billData) {
        return appService.createBill(billData);

    }

    /**
     * 新增单据
     *
     * @param billData 单据
     * @return 新增结果
     */
    @RequestMapping(value = "createrecyclebill")
    @ResponseBody
    @AuthOpenApi("proj_billsync:createrecyclebill")
    public APIResult<Object> createRecycleBill(@RequestParam() String billData) {
        return appService.createRecycleBill(billData);

    }

    /**
     * 新增单据-->门店调拨单
     *
     * @param billData 单据
     * @return 新增结果
     */
    @RequestMapping(value = "createstoreadjustbill")
    @ResponseBody
    @AuthOpenApi("proj_billsync:createstoreadjustbill")
    public APIResult<Object> createStoreAdjustBill(@RequestParam() String billData) {
        return appService.createStoreAdjustBill(billData);

    }

    /**
     * 取消单据
     *
     * @param billData 单据
     * @return 取消结果
     */
    @RequestMapping(value = "cancelbill")
    @ResponseBody
    @AuthOpenApi("proj_billsync:cancelbill")
    public APIResult<String> cancelBill(@RequestParam() String billData) {
        return appService.cancelBill(billData);

    }
}
