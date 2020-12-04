package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebd.common.CommonUtil;
import com.arlen.ebd.entity.*;
import com.arlen.ebd.enums.MaterialType;
import com.arlen.ebd.repository.MaterialImageRepository;
import com.arlen.ebd.repository.MaterialRepository;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.repository.SysParaRepository;
import com.proj.appservice.ProjMaterialAppService;
import com.proj.dto.ProjMaterialDTO;
import com.proj.dto.ProjMaterialImageDTO;
import org.apache.commons.lang3.StringUtils;
import org.dozer.DozerBeanMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by arlenChen on 2019/9/11.
 *
 * @author arlenChen
 */
@Service
public class ProjMaterialAppServiceImpl implements ProjMaterialAppService {

    @Resource
    private MaterialRepository materialRepository;
    @Resource
    private MaterialImageRepository materialImageRepository;

    private static final String DEFAULT_IMG_PATH = "/material_Img/";
    private static final String SEPARATOR = "/";
    @Resource(name = "sysProperties")
    private Properties sysProperties;
    @Resource
    private SysParaRepository sysParaRepository;

    /**
     * 添加Material
     *
     * @param materialDTO materialDTO
     * @return materialDTO
     */
    @Override
    public APIResult<String> create(ProjMaterialDTO materialDTO) {
        APIResult<String> result = new APIResult<>();
        // 校验shortCode是否已经存在
        List<Material> materialList = materialRepository.getMaterialByShortCode(materialDTO.getShortCode());
        if(null != materialList && materialList.size() > 0){
            return result.fail(APIResultCode.ALREADY_EXSISTED, "短代码已存在");
        }
        if (StringUtil.isEmpty(materialDTO.getSku())) {
            //默认SKU必填，不填自动生成一个SKU
            materialDTO.setSku(UUID.randomUUID().toString());
        }
        materialDTO.setMaterialType(MaterialType.CHENGPIN.index);
        if (materialRepository.getBySKU(materialDTO.getSku()) != null) {
            return result.fail(APIResultCode.ALREADY_EXSISTED, "ERP代码已存在");
        }
        if (StringUtil.isNotEmpty(materialDTO.getShortCode())) {
            Material existed = materialRepository.getByShortCode(materialDTO.getShortCode());
            if (existed != null) {
                return result.fail(APIResultCode.ALREADY_EXSISTED, "短代码已存在");
            }
        }
        //短代码为空，赋值为产品代码
        if (StringUtil.isEmpty(materialDTO.getShortCode())) {
            materialDTO.setShortCode(materialDTO.getSku());
        }
        DozerBeanMapper mapper = new DozerBeanMapper();
        Material material = mapper.map(materialDTO, Material.class);
        if (material.getExworkPrice() == null) {
            material.setExworkPrice(BigDecimal.ZERO);
        }
        if (materialDTO.getCollecQty() <= 0) {
            material.setCollecQty(1);
        }
        if (StringUtils.isEmpty(materialDTO.getUnitId())) {
            material.setUnitId(null);
        }
        if (material.getBomList() != null && material.getBomList().size() > 0) {
            for (MaterialBOM item : material.getBomList()) {
                if (StringUtil.isNotEmpty(item.getCMaterialId())) {
                    item.setMaterial(material);
                    item.setAddBy(material.getAddBy());
                    item.setAddTime(new Date());
                    item.setEditBy(material.getAddBy());
                    item.setEditTime(new Date());
                }
            }
        }
        if (material.getGrossWeight() == null) {
            material.setGrossWeight(new BigDecimal("0.0000"));
        }
        material.setMaterialType(MaterialType.CHENGPIN.index);
        materialRepository.insert(material);
        try {
            processBabyInfo(materialDTO, material, materialDTO.getEditBy());
        } catch (Exception e) {
            return result.fail(500, e.getMessage());
        }
        return result.succeed().attachData(material.getId());
    }

    /**
     * 修改Material
     *
     * @param materialDTO materialDTO
     * @return materialDTO
     */
    @Override
    public APIResult<String> update(ProjMaterialDTO materialDTO) {
        APIResult<String> result = new APIResult<>();
        Material material = materialRepository.load(materialDTO.getId());
        if (material == null) {
            return result.fail(APIResultCode.NOT_FOUND, "数据不存在");
        }
        // 校验shortCode是否已经存在(不包含自己)
        List<Material> materialList = materialRepository.getMaterialByShortCode(materialDTO.getShortCode());
        if(null != materialList && materialList.size() > 0){
            Iterator it = materialList.iterator();
            while (it.hasNext()){
                Material bean = (Material) it.next();
                String dbSku = bean.getSku();
                String inSku = materialDTO.getSku();
                if(dbSku.equals(inSku)){
                    it.remove();
                }
            }
        }
        if(null != materialList && materialList.size() > 0){
            return result.fail(APIResultCode.ALREADY_EXSISTED, "短代码已存在");
        }
        material.setPcsVolume(materialDTO.getPcsVolume());
        material.setMaterialType(materialDTO.getMaterialType());
        material.setPcsWeight(materialDTO.getPcsWeight());
        material.setGrossWeight(materialDTO.getGrossWeight());
        material.setSpec(materialDTO.getSpec());
        material.setShortName(materialDTO.getShortName());
        if (materialDTO.getCollecQty() <= 0) {
            material.setCollecQty(1);
        } else {
            material.setCollecQty(materialDTO.getCollecQty());
        }
        if (StringUtils.isEmpty(materialDTO.getUnitId())) {
            material.setUnitId(null);
        } else {
            material.setUnitId(materialDTO.getUnitId());
        }

        materialDTO.setMaterialType(MaterialType.CHENGPIN.index);
        if (StringUtil.isNotEmpty(materialDTO.getShortCode())) {
            Material existed = materialRepository.getByShortCode(materialDTO.getShortCode());
            if (existed != null && !existed.getId().equals(material.getId())) {
                return result.fail(APIResultCode.ALREADY_EXSISTED, "短代码已存在");
            }
        }
        if (StringUtil.isNotEmpty(materialDTO.getSku())) {
            Material existed = materialRepository.getBySKU(materialDTO.getSku());
            if (existed != null && !existed.getId().equals(material.getId())) {
                return result.fail(APIResultCode.ALREADY_EXSISTED, "ERP代码已存在");
            }
        }

        material.setShelfLifeUnit(materialDTO.getShelfLifeUnit());
        material.setShelfLife(materialDTO.getShelfLife());
        material.setErrorRange(materialDTO.getErrorRange());
        material.setBoxQty(materialDTO.getBoxQty());
        material.setBoxGtin(materialDTO.getBoxGtin());
        material.setComQty(materialDTO.getComQty());
        material.setComGtin(materialDTO.getComGtin());
        material.setForeignId(materialDTO.getForeignId());
        material.setEnglishName(materialDTO.getEnglishName());
        material.setFullName(materialDTO.getFullName());
        material.setCollecQty(materialDTO.getCollecQty());
        material.setPcsQty(materialDTO.getPcsQty());
        material.setInUse(materialDTO.getInUse());
        material.setOnSale(materialDTO.getOnSale());
        material.setPackageId(materialDTO.getPackageId());
        material.setPcsGtin(materialDTO.getPcsGtin());
        material.setRetailPrice(materialDTO.getRetailPrice());
        //短代码为空，赋值为产品代码
        if (StringUtil.isEmpty(materialDTO.getShortCode())) {
            material.setShortCode(materialDTO.getSku());
        } else {
            material.setShortCode(materialDTO.getShortCode());
        }
        material.setSpecId(materialDTO.getSpecId());
        material.setSeriesId(materialDTO.getSeriesId());
        material.setStageId(materialDTO.getStageId());
        material.setVolumeUnit(materialDTO.getVolumeUnit());
        material.setWeightUnit(materialDTO.getWeightUnit());
        material.setUnitId(materialDTO.getUnitId());
        material.setOrigin(materialDTO.getOrigin());
        material.setDescription(materialDTO.getDescription());
        material.setCategoryId(materialDTO.getCategoryId());
        material.setForwardDays(materialDTO.getForwardDays());
        material.setExworkPrice(materialDTO.getExworkPrice());
        material.setAttr1(materialDTO.getAttr1());
        material.setAttr2(materialDTO.getAttr2());
        material.setAttr3(materialDTO.getAttr3());
        material.setAttr4(materialDTO.getAttr4());
        material.setAttr5(materialDTO.getAttr5());
        material.setAttr6(materialDTO.getAttr6());
        material.setRemark(materialDTO.getRemark());
        material.setEditBy(materialDTO.getEditBy());
        material.setEditTime(new Date());
        material.setExt1(materialDTO.getExt1());
        material.setExt2(materialDTO.getExt2());
        material.setExt3(materialDTO.getExt3());
        material.setExt4(materialDTO.getExt4());
        material.setExt5(materialDTO.getExt5());
        material.setExt6(materialDTO.getExt6());
        material.setExt7(materialDTO.getExt7());
        material.setExt8(materialDTO.getExt8());
        material.setExt9(materialDTO.getExt9());
        material.setExt10(materialDTO.getExt10());
        material.setExt11(materialDTO.getExt11());
        material.setExt12(materialDTO.getExt12());
        material.setExt13(materialDTO.getExt13());
        material.setExt14(materialDTO.getExt14());
        material.setExt15(materialDTO.getExt15());
        material.setNewMaterial(materialDTO.getNewMaterial());
        material.setCheckOnLine(materialDTO.getCheckOnLine());
        if (StringUtil.isNotEmpty(materialDTO.getSku())) {
            //如果前台没有传递sku，sku保持不变
            material.setSku(materialDTO.getSku());
        }
        material.setMaterialType(MaterialType.CHENGPIN.index);
        materialRepository.save(material);
        try {
            processBabyInfo(materialDTO, material, materialDTO.getEditBy());
        } catch (Exception e) {
            return result.fail(500, e.getMessage());
        }
        return result.succeed().attachData(material.getId());
    }

    /**
     * 获取产品附属文件列表
     *
     * @param materialId 产品附属文件
     * @return 列表
     */
    @Override
    public APIResult<List<ProjMaterialImageDTO>> getFileList(String materialId) {
        APIResult<List<ProjMaterialImageDTO>> result = new APIResult<>();
        List<ProjMaterialImageDTO> dtoList = new ArrayList<>();
        List<MaterialImage> imageList = materialImageRepository.getByMaterial(materialId);
        SysPara sysPara = sysParaRepository.load("webUrl");
        String fileBaseDir = getUploadDir().replace("/", SEPARATOR).replace("\\", SEPARATOR);
        for (MaterialImage materialImage : imageList) {
            ProjMaterialImageDTO dto = CommonUtil.map(materialImage, ProjMaterialImageDTO.class);
            String webUrlStr = "";
            if (sysPara != null && StringUtils.isNotEmpty(sysPara.getValue())) {
                webUrlStr = sysPara.getValue();
            }
            if (StringUtils.isNotEmpty(dto.getImgPath())) {
                String webUrl = dto.getImgPath().replace(fileBaseDir, webUrlStr + DEFAULT_IMG_PATH);
                webUrl = webUrl.replace("/", SEPARATOR).replace("\\", SEPARATOR);
                dto.setWebUrl(webUrl);
            }
            dtoList.add(dto);
        }
        return result.succeed().attachData(dtoList);
    }

    @Override
    public File getUploadFileByID(String id) {
        MaterialImage materialImage = materialImageRepository.load(id);
        return new File(materialImage.getImgPath());
    }

    private void processBabyInfo(ProjMaterialDTO dto, Material material, String addBy) throws Exception {
        List<ProjMaterialImageDTO> dtoList = dto.getDetailList();
        if (dtoList != null && !dtoList.isEmpty()) {
            List<MaterialImage> imageList = materialImageRepository.getByMaterial(material.getId());
            Map<String, String> babyInfoMap = new HashMap<>(dtoList.size());
            List<MaterialImage> insertList = new ArrayList<>();
            List<MaterialImage> saveList = new ArrayList<>();
            for (ProjMaterialImageDTO imageDTO : dtoList) {
                MaterialImage materialImage;
                if (StringUtils.isNotEmpty(imageDTO.getId())) {
                    materialImage = imageList.stream().filter(x -> x.getId().equals(imageDTO.getId())).findFirst().orElse(null);
                    if (materialImage != null) {
                        babyInfoMap.put(materialImage.getId(), null);
                        saveList.add(materialImage);
                    } else {
                        materialImage = new MaterialImage();
                        materialImage.setAddBy(addBy);
                        materialImage.setAddTime(new Date());
                        insertList.add(materialImage);
                    }
                } else {
                    materialImage = new MaterialImage();
                    materialImage.setAddBy(addBy);
                    materialImage.setAddTime(new Date());
                    insertList.add(materialImage);
                    String imgFile = uploadFile(imageDTO.getImgFile());
                    materialImage.setImgPath(imgFile);
                }
                materialImage.setEditBy(addBy);
                materialImage.setEditTime(new Date());
                materialImage.setMaterialId(material.getId());
                materialImage.setAttr1(imageDTO.getAttr1());
            }
            for (MaterialImage info : imageList) {
                if (!babyInfoMap.containsKey(info.getId())) {
                    materialImageRepository.delete(info);
                }
            }
            materialImageRepository.save(saveList);
            materialImageRepository.insert(insertList);
        } else {
            List<MaterialImage> materialImageList = materialImageRepository.getByMaterial(dto.getId());
            for (MaterialImage image : materialImageList) {
                materialImageRepository.delete(image);
            }
        }

    }

    public String uploadFile(MultipartFile imgFile) throws Exception {
        byte[] imgBytes;
        imgBytes = imgFile.getBytes();
        if (imgBytes != null && imgBytes.length > 0) {
            String originFileName = imgFile.getOriginalFilename();
            String fileBaseDir = getUploadDir().replace("/", SEPARATOR).replace("\\", SEPARATOR);
            String fileDir = getFileDir().replace("/", SEPARATOR).replace("\\", SEPARATOR);
            if (!createDir(fileBaseDir, fileDir)) {
                return "文件保存失败";
            }
            //保存
            Date now = new Date();
            String fileName = now.getTime() + "-" + originFileName;
            Path file = Paths.get(fileBaseDir + fileDir + fileName);
            if (!saveFile(imgBytes, file)) {
                return "文件保存失败";
            }
            return fileBaseDir + fileDir + fileName;
        } else {
            return "文件不存在";
        }
    }

    /**
     * 获取上传文件保存目录
     *
     * @return 上传文件保存目录
     */
    @Override
    public String getUploadDir() {
        String fileDir = sysProperties.getProperty("wx_material_Img_Dir", null);
        if (StringUtils.isEmpty(fileDir)) {
            fileDir = System.getProperty("user.dir");
        }
        return fileDir + DEFAULT_IMG_PATH;
    }

    private boolean saveFile(byte[] imgBytes, Path file) {
        try {
            Files.write(file, imgBytes, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.getStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 获取文件存放目录，包含当前年月日
     *
     * @return 文件目录
     */
    private String getFileDir() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.YEAR) + "\\" + (calendar.get(Calendar.MONTH) + 1) + "\\" + sdf.format(calendar.getTime()) + "\\";
    }

    private boolean createDir(String fileBaseDir, String fileDir) {
        Path dirPath = Paths.get(fileBaseDir + fileDir);
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                e.getStackTrace();
                return false;
            }
        }
        return true;
    }
}
