package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebp.dto.SysUserDTO;
import com.proj.dto.ImportStoreDealerRelationDTO;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 导入门店与分销商供货关系
 * Created by Robert on 2016/9/9.
 */
public interface StoreD2RelationAppService {

    APIResult<List<ImportStoreDealerRelationDTO>> importExcel(InputStream inputStream, String fileType, SysUserDTO currentUser);

    /**
     * 保存数据
     *
     * @param list
     * @param currentUser
     */
    void importExcelSave(List<ImportStoreDealerRelationDTO> list, Set<String> d2CodeSet, Map<String, String> d2CodeMap, Set<String> storeCodeSet, Map<String, String> storeCodeSetMap, Set<String> relSet, SysUserDTO currentUser);

    /**
     * 验证导入的门店数据
     *
     * @param dataList dataList
     * @return APIResult
     */
    APIResult<Map<String, Object>> checkStoreIsNormal(List<Map<String, String>> dataList);

    /**
     * 保存导入结果
     *
     * @param dataList 库存集合
     * @param userDTO  当前用户
     * @return APIResult
     */
    APIResult<String> saveData(List<Map<String, String>> dataList, SysUserDTO userDTO) throws Exception;
}
