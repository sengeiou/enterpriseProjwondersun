package com.proj.repository;

import com.arlen.eaf.core.dto.PageReq;
import org.springframework.data.domain.Page;

import java.util.HashMap;

/**
 * 自定义报表repo
 */
public interface StruCustomReportRepository {
    Page<HashMap<String, String>> getPageList(PageReq pageReq, String userId, String dataSourceScript);
}
