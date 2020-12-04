package com.proj.appservice;


import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.dto.PageResult;
import com.proj.dto.DealerSignTypeDTO;

import java.util.List;
import java.util.Map;

/**
 * 经销商签收方式
 */
public interface DealerSignTypeAppService {

    APIResult<DealerSignTypeDTO> getById(String id);

    APIResult<String> create(DealerSignTypeDTO dto);

    APIResult<String> update(DealerSignTypeDTO dto);

    APIResult<String> delete(String id);

    APIResult<List<DealerSignTypeDTO>> getChildList();

    APIResult<PageResult<Map>> getPageListBySql(PageReq pageReq);

    APIResult<Boolean> queryByDealerId(String dealerId);

    APIResult<Boolean> isAutoReceive(String dealerId);

    APIResult<String> getAutoDeliveryStore(String dealerId);

}
