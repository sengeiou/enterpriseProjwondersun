package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.H5CodeQueryAppService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * Created by arlenChen on 2019/12/11.
 * 生产数据统计报表
 *
 * @author arlenChen
 */
@Controller
@RequestMapping("/api/proj/codedata")
public class ProjCodeDataApi extends BaseController {
    @Resource
    private H5CodeQueryAppService changeOrganizationInUse;

    @RequestMapping(value = {"sync"}, method = {RequestMethod.POST})
    @ResponseBody
    public Object sync(@RequestParam("ids") String[] ids) {
        APIResult<String> mes = new APIResult<>();
        int k = 0;
        if (ids == null) {
            return mes.fail(809, "ids not null");
        } else {
            for (String id : ids) {
                APIResult<String> result = changeOrganizationInUse.syncCodeData(id);
                if (result.getCode() != 0) {
                    mes = result;
                    k++;
                }
            }
            if (k == ids.length) {
                return mes;
            } else {
                return new APIResult<String>().succeed();
            }
        }
    }

    @RequestMapping(value = {"confirm"}, method = {RequestMethod.POST})
    @ResponseBody
    public Object confirm(String id, String remark) {
        return changeOrganizationInUse.confirm(id, remark, getCurrentUser().getUserName());
    }
}
