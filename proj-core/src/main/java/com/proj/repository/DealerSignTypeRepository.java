package com.proj.repository;

import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.repository.BaseCrudRepository;
import com.proj.entity.DealerSignType;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * 经销商签收方式
 */
public interface DealerSignTypeRepository extends BaseCrudRepository<DealerSignType> {

    /**
     * 获取所有经销商签收方式配置
     *
     * @return
     */
    List<DealerSignType> getList();

    /**
     * 分页获取
     *
     * @param pageReq
     * @return
     */
    Page<Map> getPageListBySql(PageReq pageReq);

    /**
     * 获取指定经销商启用的配置
     *
     * @param dealerId
     * @return
     */
    List<DealerSignType> getListByDealerId(String dealerId);
}
