package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.dto.PageResult;

import java.util.List;
import java.util.Map;

/**
 * Created by ehsure on 2016/8/25.
 */
public interface StruCommonQueryAppService {

    /**
     * 分页
     *
     * @param pageReq   分页参数
     * @param scriptStr 分页sql
     * @return 分页结果
     */
    APIResult<PageResult<Map>> getPageList(PageReq pageReq, String userId, String scriptStr);

    /**
     * 根据ID获取数据
     *
     * @param id        主键ID
     * @param scriptStr sql语句
     * @return 数据
     */
    APIResult<Map> getMapById(String id, String userId, String scriptStr);

    /**
     * 查询集合
     *
     * @param paramMap  查询条件参数
     * @param scriptStr sql语句
     * @return 查询结果集合
     */
    List<Map> getRowsByQueryMap(Map<String, String> paramMap, String scriptStr);
}
