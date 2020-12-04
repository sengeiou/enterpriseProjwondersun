package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebp.dto.SysUserDTO;
import com.proj.dto.ImportStoreDealerRelationDTO;

import java.io.InputStream;
import java.util.List;

/**导入经销商更新客户经理
 * Created by Robert on 2016/9/30.
 */
public interface DealerUpdateCMAppService {

    APIResult<List<ImportStoreDealerRelationDTO>> importExcel(InputStream inputStream, String fileType, SysUserDTO currentUser);

    /**
     * 保存数据
     * @param list
     * @param currentUser
     */
    void importExcelSave(List<ImportStoreDealerRelationDTO> list,SysUserDTO currentUser);
}
