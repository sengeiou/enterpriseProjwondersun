package com.proj.appservice;


import com.arlen.eaf.core.dto.APIResult;
import com.proj.dto.H5CodeQueryDTO;

/**
 * Created by arlenChen on 2019/9/24.
 * h5追溯信息
 *
 * @author arlenChen
 */
public interface H5CodeQueryAppService {
    /**
     * 追溯码查询数据
     *
     * @param code 追溯码
     * @return 数据
     */
    APIResult<H5CodeQueryDTO> queryByCode(String code);

    /**
     * 生产数据统计报表 id
     *
     * @param id id
     * @return 数据
     */
    APIResult<String> syncCodeData(String id);

    /**
     * 确认
     *
     * @param id     d
     * @param remark 原因
     * @param confirmBy 确认
     * @return return
     */
    APIResult<String> confirm(String id, String remark, String confirmBy);
}
