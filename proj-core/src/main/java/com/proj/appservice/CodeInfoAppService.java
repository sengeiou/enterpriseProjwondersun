package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;

import java.util.List;
import java.util.Map;

/**
 * Created by ehsure on 2016/9/13.
 */
public interface CodeInfoAppService {
    /**
     * 根据箱码查询下级编码集合
     *
     * @param boxCode 箱码
     * @return
     */
    APIResult<List<Map<String, Object>>> getByBoxCode(String boxCode);


    /**
     * 根据积分码或者单品码查询数据
     *
     * @param eptCode 积分码
     * @param code    单品码
     * @return
     */
    APIResult<Map<String, Object>> getByCode(String code, String eptCode);
}
