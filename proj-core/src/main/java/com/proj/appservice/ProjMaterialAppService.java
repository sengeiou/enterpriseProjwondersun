package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.proj.dto.ProjMaterialDTO;
import com.proj.dto.ProjMaterialImageDTO;

import java.io.File;
import java.util.List;

/**
 * Created by arlenChen on 2019/9/11.
 *
 * @author arlenChen
 */
public interface ProjMaterialAppService {

    /**
     * 添加Material
     *
     * @param materialDTO materialDTO
     * @return materialDTO
     */
    APIResult<String> create(ProjMaterialDTO materialDTO);

    /**
     * 修改Material
     *
     * @param materialDTO materialDTO
     * @return materialDTO
     */
    APIResult<String> update(ProjMaterialDTO materialDTO);

    /**
     * 获取产品附属文件列表
     *
     * @param materialId 产品附属文件
     * @return 列表
     */
    APIResult<List<ProjMaterialImageDTO>> getFileList(String materialId);

    /**
     * 得到上传文件
     *
     * @param id
     * @return
     */
    File getUploadFileByID(String id);

    String getUploadDir();
}
