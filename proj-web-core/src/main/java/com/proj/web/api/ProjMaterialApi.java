package com.proj.web.api;


import com.eaf.core.dto.APIResult;
import com.ebp.dto.SysUserDTO;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.ProjMaterialAppService;
import com.proj.dto.ProjMaterialDTO;
import com.proj.dto.ProjMaterialImageDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 物料api
 */
@Controller
@RequestMapping("/api/proj/material")
public class ProjMaterialApi extends BaseController {

    @Resource
    private ProjMaterialAppService appService;

    /**
     * 添加物料
     *
     * @param materialDTO 物料数据
     * @return 操作结果
     */
    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public Object add(ProjMaterialDTO materialDTO) {
        SysUserDTO currentUser = getCurrentUser();
        materialDTO.setAddTime(new Date());
        materialDTO.setEditTime(new Date());
        materialDTO.setAddBy(currentUser.getUserName());
        materialDTO.setEditBy(currentUser.getUserName());
        APIResult<String> result = new APIResult<>();
        if (materialDTO.getDetailList() != null && !materialDTO.getDetailList().isEmpty()) {
            for (ProjMaterialImageDTO imageDTO : materialDTO.getDetailList()) {
                if (StringUtils.isEmpty(imageDTO.getAttr1())) {
                    return result.fail(403, "请选择文件类型");
                }
                if(StringUtils.isNotEmpty(imageDTO.getImgPath())){
                    String fileName = imageDTO.getImgPath();
                    boolean isPdf = (fileName.endsWith(".pdf") || fileName.endsWith(".PDF"));
                    if (isPdf) {
                        if (!"pdf".equals(imageDTO.getAttr1())) {
                            return result.fail(403, "请重新选择文件类型");
                        }
                    }
                    boolean isjpg = (fileName.endsWith(".jpg") || fileName.endsWith(".JPG"));
                    if (isjpg) {
                        if (!"jpg".equals(imageDTO.getAttr1())&&!"jpg2".equals(imageDTO.getAttr1())) {
                            return result.fail(403, "请重新选择文件类型");
                        }
                    }
                }else{
                    String fileName = imageDTO.getImgFile().getOriginalFilename();
                    boolean isPdf = (fileName.endsWith(".pdf") || fileName.endsWith(".PDF"));
                    if (isPdf) {
                        if (!"pdf".equals(imageDTO.getAttr1())) {
                            return result.fail(403, "请重新选择文件类型");
                        }
                    }
                    boolean isjpg = (fileName.endsWith(".jpg") || fileName.endsWith(".JPG"));
                    if (isjpg) {
                        if (!"jpg".equals(imageDTO.getAttr1())&&!"jpg2".equals(imageDTO.getAttr1())) {
                            return result.fail(403, "请重新选择文件类型");
                        }
                    }
                }
            }
        }
        return appService.create(materialDTO);
    }

    /**
     * 修改Material
     *
     * @param dto MaterialDTO
     * @return 操作结果
     */
    @RequestMapping(value = "edit", method = RequestMethod.POST)
    @ResponseBody
    public Object edit(ProjMaterialDTO dto) {
        SysUserDTO currentUser = getCurrentUser();
        dto.setEditTime(new Date());
        dto.setEditBy(currentUser.getUserName());
        APIResult<String> result = new APIResult<>();
        if (dto.getDetailList() != null && !dto.getDetailList().isEmpty()) {
            for (ProjMaterialImageDTO imageDTO : dto.getDetailList()) {
                if (StringUtils.isEmpty(imageDTO.getAttr1())) {
                    return result.fail(403, "请选择文件类型");
                }
                if(StringUtils.isNotEmpty(imageDTO.getImgPath())){
                    String fileName = imageDTO.getImgPath();
                    boolean isPdf = (fileName.endsWith(".pdf") || fileName.endsWith(".PDF"));
                    if (isPdf) {
                        if (!"pdf".equals(imageDTO.getAttr1())) {
                            return result.fail(403, "请重新选择文件类型");
                        }
                    }
                    boolean isjpg = (fileName.endsWith(".jpg") || fileName.endsWith(".JPG"));
                    if (isjpg) {
                        if (!"jpg".equals(imageDTO.getAttr1())&&!"jpg2".equals(imageDTO.getAttr1())) {
                            return result.fail(403, "请重新选择文件类型");
                        }
                    }
                }else{
                    String fileName = imageDTO.getImgFile().getOriginalFilename();
                    boolean isPdf = (fileName.endsWith(".pdf") || fileName.endsWith(".PDF"));
                    if (isPdf) {
                        if (!"pdf".equals(imageDTO.getAttr1())) {
                            return result.fail(403, "请重新选择文件类型");
                        }
                    }
                    boolean isjpg = (fileName.endsWith(".jpg") || fileName.endsWith(".JPG"));
                    if (isjpg) {
                        if (!"jpg".equals(imageDTO.getAttr1())&&!"jpg2".equals(imageDTO.getAttr1())) {
                            return result.fail(403, "请重新选择文件类型");
                        }
                    }
                }
            }
        }
        return appService.update(dto);
    }

    /**
     * 获取产品附属文件列表
     *
     * @param materialId 产品附属文件
     * @return 列表
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.GET)
    @ResponseBody
    public APIResult<List<ProjMaterialImageDTO>> getFileList(String materialId) {
        return appService.getFileList(materialId);
    }

    @RequestMapping(value = "download")
    @ResponseBody
    public void download(@RequestParam(value = "id") String id, HttpServletResponse res) {
        File file = appService.getUploadFileByID(id);
        if (file == null) {
            ServletOutputStream out = null;
            try {
                out = res.getOutputStream();
                out.write("文件不存在".getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            return;
        }
        ProjMaterialApi.downLoadFile(res, file);
    }
}
