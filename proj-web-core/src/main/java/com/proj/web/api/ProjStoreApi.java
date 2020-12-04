package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.utils.StringUtil;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.ProjStoreAppService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * Created by ehsure on 2017/8/25.
 * 门店
 */
@Controller
@RequestMapping("/api/proj/checkstore")
public class ProjStoreApi extends BaseController {

    @Resource
    private ProjStoreAppService projStoreAppService;

    /**
     * 添加码表
     *
     * @param shortName 简称
     * @param fullName  全称
     * @return 操作结果
     */
    @RequestMapping(value = "checkname", method = RequestMethod.POST)
    @ResponseBody
    public Object checkName(String shortName, String fullName, String id) {
        APIResult<String> result = new APIResult<>();
        String msg = projStoreAppService.checkName(shortName, fullName, id);
        if (StringUtil.isNotEmpty(msg)) {
            return result.fail(500, msg);
        } else {
            return result.succeed().attachData(msg);
        }
    }
}
