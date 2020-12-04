package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.PageReq;
import com.eaf.core.dto.PageResult;
import com.ebd.appservice.SimpleTypeAppService;
import com.ebd.dto.SimpleTypeDTO;
import com.ebd.dto.SimpleTypeEditDTO;
import com.ebp.dto.SysUserDTO;
import com.ebp.web.comm.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by ehsure on 2016/10/24.
 * 经销商类别
 */
@Controller
@RequestMapping("/api/proj/weightunit")
public class WeightUnitApi extends BaseController {
    @Resource
    private SimpleTypeAppService appService;

    /**
     * 根据ID获取码表
     *
     * @param id 码表ID
     * @return 码表
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public APIResult<SimpleTypeDTO> getById(String id) {
        return appService.getById(id);
    }

    /**
     * 添加码表
     *
     * @param simpleTypeDTO 码表数据
     * @return 操作结果
     */
    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public Object add(SimpleTypeDTO simpleTypeDTO) {
        SysUserDTO currentUser = getCurrentUser();
        APIResult<SimpleTypeDTO> apiResult = appService.getById(simpleTypeDTO.getCategoryId());
        if (apiResult.getData() == null) {
            SimpleTypeDTO simpleTypeDTO1 = new SimpleTypeDTO();
            simpleTypeDTO1.setId("WEIGHTUNIT");
            simpleTypeDTO1.setAddBy(currentUser.getUserName());
            simpleTypeDTO1.setEditBy(currentUser.getUserName());
            simpleTypeDTO1.setAddTime(new Date());
            simpleTypeDTO1.setEditTime(new Date());
            simpleTypeDTO1.setCode("WEIGHTUNIT");
            simpleTypeDTO1.setName("重量单位");
            simpleTypeDTO1.setInUse(true);
            appService.create(simpleTypeDTO1);
        }
        simpleTypeDTO.setAddTime(new Date());
        simpleTypeDTO.setEditTime(new Date());
        simpleTypeDTO.setId("WEIGHTUNIT_" + simpleTypeDTO.getCode());
        simpleTypeDTO.setAddBy(currentUser.getUserName());
        simpleTypeDTO.setEditBy(currentUser.getUserName());
        APIResult<String> result = appService.create(simpleTypeDTO);
        return result;
    }

    /**
     * 修改码表
     *
     * @param dto SimpleTypeDTO 码表数据
     * @return 操作结果
     */
    @RequestMapping(value = "edit", method = RequestMethod.POST)
    @ResponseBody
    public Object edit(SimpleTypeEditDTO dto) {
        SysUserDTO currentUser = getCurrentUser();
        dto.setEditTime(new Date());
        dto.setEditBy(currentUser.getUserName());
        APIResult<String> result = appService.update(dto);
        return result;
    }


    /**
     * 分页获取码表集合
     *
     * @param pageReq 分页参数
     * @return 分页结果
     */
    @RequestMapping(value = "list")
    @ResponseBody
    public PageResult<Map> list(@RequestBody PageReq pageReq) {
        APIResult<PageResult<Map>> result = appService.getPageListBySql(pageReq);
        return result.getData();
    }

    /**
     * 获取子类集合
     *
     * @param categoryId 父级Id
     * @return 码表集合
     */
    @RequestMapping(value = "getchildlist")
    @ResponseBody
    public APIResult<List<SimpleTypeDTO>> getChildList(String categoryId) {
        APIResult<List<SimpleTypeDTO>> result = appService.getChildList(categoryId);
        return result;
    }

    @RequestMapping(value = {"delete"}, method = {RequestMethod.POST})
    @ResponseBody
    public Object delete(@RequestParam("ids") String[] ids) {
        APIResult<String> mes = new APIResult<>();
        int k = 0;
        if (ids == null) {
            return mes.fail(809, "ids not null");
        } else {
            for (String id : ids) {
                APIResult<String> result = appService.delete(id);
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

    /**
     * 获取所有码表集合
     *
     * @param pid    上级ID
     * @param addAll 获取所有     true
     * @return 码表集合
     */
    @RequestMapping(value = "simpletypelist")
    @ResponseBody
    public List<SimpleTypeDTO> simpleTypeList(String pid, @RequestParam(required = false, defaultValue = "true") boolean addAll) {
        APIResult<List<SimpleTypeDTO>> result = appService.getChildList(pid);
        if (addAll) {
            SimpleTypeDTO fristdto = new SimpleTypeDTO();
            fristdto.setId("");
            fristdto.setName("所有");
            result.getData().add(0, fristdto);
        }
        return result.getData();
    }

    /**
     * 获取下拉列表用子类集合
     *
     * @param categoryId 父级Id
     * @return 码表集合
     */
    @RequestMapping(value = "getchildlistcombobox")
    @ResponseBody
    public List<Map> getChildListComboBox(String categoryId, @RequestParam(required = false, defaultValue = "true") boolean addAll) {
        APIResult<List<SimpleTypeDTO>> result = appService.getChildList(categoryId);
        List<Map> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        for (SimpleTypeDTO simpleTypeDTO : result.getData()) {
            map.put("value", simpleTypeDTO.getId());
            map.put("text", simpleTypeDTO.getName());
            list.add(map);
        }
        if (addAll) {
            map.put("value", "");
            map.put("text", "所有");
            list.add(0, map);
        }
        return list;
    }
}
