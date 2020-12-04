package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.dto.PageResult;
import com.arlen.ebp.dto.SysStructureDTO;
import com.arlen.ebp.entity.SysStructure;
import com.arlen.ebp.repository.SysStructureRepository;
import com.proj.appservice.StruCommonQueryAppService;
import com.proj.repository.StruCommonRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ehsure on 2016/8/25.
 */
@Service
public class StruCommonQueryAppServiceImpl  implements StruCommonQueryAppService {

    @Resource
    private StruCommonRepository repo;
    @Resource
    private SysStructureRepository sysStructureRepository;
    /**
     * 分页
     *
     * @param pageReq   分页参数
     * @param scriptStr 分页sql
     * @return 分页结果
     */
    @Override
    public APIResult<PageResult<Map>> getPageList(PageReq pageReq, String userId, String scriptStr) {
        Page<Map> page = repo.getPageList(pageReq,userId, scriptStr);
        PageResult<Map> pageResult = new PageResult<>();
        pageResult.setTotal(page.getTotalElements());
        List<Map> list = page.getContent().stream().collect(Collectors.toList());
        pageResult.setRows(list);
        APIResult<PageResult<Map>> result = new APIResult<>();
        result.succeed().attachData(pageResult);
        return result;
    }

    /**
     * 根据ID获取数据
     *
     * @param id        主键ID
     * @param scriptStr sql语句
     * @return 数据
     */
    @Override
    public APIResult<Map> getMapById(String id, String userId, String scriptStr) {
        List<SysStructure> struList = sysStructureRepository.getSysStructureByUserId(userId); //获取用户的组织结构
        List<SysStructure> provinceList = new ArrayList<>(); //省区列表
        if(struList!=null||struList.size()>0){
            for (SysStructure stru: struList
                    ) {
                if(stru.getLevelNum()==3){
                    provinceList.add(stru);
                }
            }
        }
        List<SysStructureDTO> checkCityList = repo.getChildStructures(provinceList); //省区下级考核城市列表
        List<String> checkCityIdList = new ArrayList<>(); //考核城市ID
        if(checkCityList!=null || checkCityList.size()>0){
            for (SysStructureDTO stru:checkCityList
                    ) {
                checkCityIdList.add(stru.getId());
            }
        }
        Map item = repo.getMapById(id,checkCityIdList,scriptStr);
        APIResult<Map> result = new APIResult<>();
        result.succeed().attachData(item);
        return result;
    }

    /**
     * 查询集合
     *
     * @param paramMap  查询条件参数
     * @param scriptStr sql语句
     * @return 查询结果集合
     */
    @Override
    public List<Map> getRowsByQueryMap(Map<String, String> paramMap, String scriptStr) {
        return repo.getPageListByQueryMap(paramMap, scriptStr);
    }
}
