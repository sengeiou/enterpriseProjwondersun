package com.proj.web.api;

import com.ebp.web.comm.BaseController;
import com.proj.appservice.ProjStoreAppService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * Created by ehsure on 2017/8/25.
 * 替换记录
 */
@Controller
@RequestMapping("/api/proj/replacerecord")
public class replaceRecordApi extends BaseController {

    @Resource
    private ProjStoreAppService appService;

    /**
     * 添加码表
     *
     * @return 操作结果
     */
    @RequestMapping(value = "check")
    @ResponseBody
    public Object checkName(String id) {
        return appService.check(id);
    }
}
