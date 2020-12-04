package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.eaf.core.utils.StringUtil;
import com.ebp.dto.SysUserDTO;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.CodeFlowAppService;
import com.proj.dto.CodeFlow;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * Created by ehsure on 2016/6/13.
 */
@Controller
@RequestMapping({"api/proj/codeflow"})
public class CodeFlowApi extends BaseController{
    @Resource
    CodeFlowAppService appService;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public APIResult<CodeFlow> getByCode(@RequestParam(value="code") String code){
        return appService.getSyncCodeData(code);
    }

    @RequestMapping(value="org_codeflow",method = RequestMethod.GET)
    @ResponseBody
    public APIResult<CodeFlow> getOrgCodeFlowByCode(@RequestParam(value="code") String code){
        APIResult<CodeFlow> result = new APIResult<CodeFlow>();
        try{
            SysUserDTO userDTO = getCurrentUser();
            if(StringUtil.isEmpty(userDTO.getSysOrganizationId())){
                return result.fail(APIResultCode.FORBIDDEN,"当前用户不是经销商或者分销商用户！");
            }
            return appService.getOrgSyncCodeData(code,userDTO.getSysOrganizationId());
        }catch(Exception e ){
            e.printStackTrace();
            return result.fail(505,e.getMessage());
        }
    }

}
