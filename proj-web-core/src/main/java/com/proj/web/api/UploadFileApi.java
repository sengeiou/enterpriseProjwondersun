package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.eaf.core.utils.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Created by ehsure on 2016/11/3.
 */
@Controller
@RequestMapping({"api/proj/upload"})
public class UploadFileApi {

    @Resource(
            name = "sysProperties"
    )
    private Properties sysProperties;

    /**
     * 上传文件，保存到属性名称propertyName配置的文件目录中
     * @param file
     * @param propertyName
     * @return
     * @throws JsonProcessingException
     */
    @RequestMapping(value = "uploadfile", method = RequestMethod.POST)
    @ResponseBody
    private APIResult<Map<String,Object>> uploadFile(@RequestParam("file") MultipartFile file,@RequestParam String propertyName) {
        APIResult<Map<String,Object>> result = new APIResult<>();
        String  dirPath = sysProperties.getProperty("proj."+propertyName+".dir");
        if(StringUtil.isEmpty(dirPath)){
            result.fail(APIResultCode.NOT_FOUND,"配置'"+"proj."+propertyName+".dir"+"'不存在或者为空");
            return result;
        }
        File dir = new File(dirPath);
        if(!dir.exists() && !dir.isDirectory()){
            dir.mkdirs();
        }
        File outFile = new File(dirPath+"\\"+file.getOriginalFilename());
        if(!outFile.exists()){
            try {
                outFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                result.fail(APIResultCode.UNEXPECTED_ERROR,"文件创建失败:"+e.getMessage());
                return result;
            }
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            out.write(file.getBytes());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            result.fail(APIResultCode.UNEXPECTED_ERROR,e.getMessage());
            return result;
        }finally {
            if(out!=null){
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        result.succeed();
        return result;
    }
}
