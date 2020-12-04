package com.proj.repository;

import com.arlen.eaf.core.dto.PageReq;
import com.arlen.ebp.dto.SysStructureDTO;
import com.arlen.ebp.entity.SysStructure;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * Created by ehsure on 2016/8/25.
 */
public interface StruCommonRepository {

    Page<Map> getPageList(PageReq pageReq, String userId, String scriptStr);

    /**
     * 根据ID查询数据
     * @param id 数据ID
     * @param scriptStr sql
     * @return 数据
     */
    Map getMapById(String id, List<String> checkCityIdList, String scriptStr);

    /**
     * 查询集合
     *
     * @param paramMap  查询条件参数
     * @param scriptStr sql语句
     * @return 查询结果集合
     */
    List<Map> getPageListByQueryMap(Map<String, String> paramMap, String scriptStr);

    List<SysStructureDTO> getChildStructures(List<SysStructure> struList);
}
