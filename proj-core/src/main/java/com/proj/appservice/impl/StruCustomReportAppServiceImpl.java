package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.dto.PageResult;
import com.arlen.ebp.repository.SysStructureRepository;
import com.proj.appservice.StruCustomReportAppService;
import com.proj.repository.StruCustomReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义报表服务实现
 */

@Service
public class StruCustomReportAppServiceImpl implements StruCustomReportAppService {
    @Resource
    private StruCustomReportRepository repo;
    @Resource
    private SysStructureRepository sysStructureRepository;

    /**
     * 分页查询
     *
     * @param pageReq          查询条件
     * @param dataSourceScript 查询语句
     * @return 分页结果
     */
    @Override
    public PageResult<HashMap<String, String>> getPageList(PageReq pageReq,String userId, String dataSourceScript) {
        Page<HashMap<String, String>> page = repo.getPageList(pageReq,userId, dataSourceScript);
        PageResult<HashMap<String, String>> pageResult = new PageResult<>();
        pageResult.setTotal(page.getTotalElements());
        List<HashMap<String, String>> list = page.getContent().stream().collect(Collectors.toList());
        pageResult.setRows(list);
        return pageResult;
    }
}
