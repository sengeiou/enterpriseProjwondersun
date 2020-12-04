package com.proj.appservice;

import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.dto.PageResult;

import java.util.HashMap;

/**
 * 自定义报表服务
 */
public interface StruCustomReportAppService {

    /**
     * 分页查询
     *
     * @param pageReq          查询条件
     * @param dataSourceScript 查询语句
     * @return 分页结果
     */
    PageResult<HashMap<String, String>> getPageList(PageReq pageReq, String userId, String dataSourceScript);

}
