package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebp.dto.SysUserDTO;
import com.proj.dto.ImportStoreDealerRelationDTO;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**导入门店与经销商供货关系
 * Created by Robert on 2016/9/9.
 */
public interface StoreDealerRelationAppService {

    APIResult<List<ImportStoreDealerRelationDTO>> importExcel(InputStream inputStream, String fileType, SysUserDTO currentUser);

    /**
     * 保存数据
     * @param list
     * @param dealerCodeSet
     * @param dealerCodeMap
     * @param storeCodeSet
     * @param storeCodeSetMap
     * @param relSet
     * @param currentUser
     */
    void importExcelSave(List<ImportStoreDealerRelationDTO> list,Set<String> dealerCodeSet, Map<String,String> dealerCodeMap, Set<String> storeCodeSet, Map<String,String> storeCodeSetMap, Set<String> relSet, SysUserDTO currentUser);
}
