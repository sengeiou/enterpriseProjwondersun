package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.eaf.core.dto.PageReq;
import com.eaf.core.dto.PageResult;
import com.ebp.dto.SysUserDTO;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.DealerSignTypeAppService;
import com.proj.dto.DealerSignTypeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

/**
 * 经销商签收方式
 */
@Controller
@RequestMapping("/api/proj/dealersigntype")
public class DealerSignTypeApi extends BaseController {

    private static Logger logger = LoggerFactory.getLogger(DealerSignTypeApi.class);

    @Resource
    private DealerSignTypeAppService appService;

    @RequestMapping(value = "list")
    @ResponseBody
    public PageResult<Map> list(@RequestBody PageReq pageReq) {
        APIResult<PageResult<Map>> result = appService.getPageListBySql(pageReq);
        return result.getData();
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public APIResult<DealerSignTypeDTO> getById(String id) {
        return appService.getById(id);
    }

    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> add(DealerSignTypeDTO dto) {
        SysUserDTO currentUser = getCurrentUser();
        dto.setAddTime(new Date());
        dto.setEditTime(new Date());
        dto.setAddBy(currentUser.getUserName());
        dto.setEditBy(currentUser.getUserName());
        APIResult<String> result = appService.create(dto);
        return result;
    }

    @RequestMapping(value = "edit", method = RequestMethod.POST)
    @ResponseBody
    public Object edit(DealerSignTypeDTO dto) {
        SysUserDTO currentUser = getCurrentUser();
        dto.setEditTime(new Date());
        dto.setEditBy(currentUser.getUserName());
        APIResult<String> result = appService.update(dto);
        return result;
    }

    @RequestMapping(value = "delete", method = RequestMethod.POST)
    @ResponseBody
    public Object delete(String[] ids) {
        if (ids == null) {
            return new APIResult<String>().fail(APIResultCode.ARGUMENT_INVALID, "ids can not be null");
        }
        for (String id : ids) {
            appService.delete(id);
        }
        return new APIResult<String>().succeed();
    }

    /**
     * 根据经销商ID查询是否一键签收
     * APP使用接口
     * @param dealerId
     * @return
     */
    @RequestMapping(value = "queryByDealerId", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<Boolean> queryByDealerId(String dealerId){
        APIResult<Boolean> result = new APIResult<>();
        try {
            result = appService.queryByDealerId(dealerId);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("根据经销商ID查询是否一键签收出现异常：",e);
        }
        return result;
    }
}
