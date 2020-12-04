package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebp.dto.SysUserDTO;
import com.proj.dto.ImportStoreDealerRelationDTO;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 导入经销商与分销商供货关系
 * Created by Robert on 2016/9/20.
 */
public interface DealerD2RelationAppService {

    APIResult<List<ImportStoreDealerRelationDTO>> importExcel(InputStream inputStream, String fileType, SysUserDTO currentUser);

    /**
     * 保存数据
     *
     * @param list
     * @param d2CodeMap
     * @param dealerCodeSetMap
     * @param relSet
     * @param currentUser
     */
    void importExcelSave(List<ImportStoreDealerRelationDTO> list, Map<String, String> d2CodeMap, Map<String, String> dealerCodeSetMap, Set<String> relSet, SysUserDTO currentUser);

    /**
     * 改变经销商或门店的禁用启用状态
     *
     * @param type 类型 1：启用;2：禁用
     * @param id   id
     * @return 结果
     */
    APIResult<String> changeOrganizationInUse(int type, String id, String userName);
}
