package com.proj.web.openapi;

import com.ebc.biz.codefile.ThreadLocalDateUtil;
import com.ebp.web.comm.BaseController;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**发送文件到工信部
 * Created by ehsure on 2016/11/22.
 */
@Controller
@RequestMapping("/openapi/proj/productqcrecord")
public class MIITDownloadApi extends BaseController {
    private Logger logger = LoggerFactory.getLogger(MIITDownloadApi.class);

    @Resource(name = "sysProperties")
    private Properties sysProperties;

    /**
     * 发送文件到工信部 单品文件
     * @param req
     * @param res
     */
    @RequestMapping(value = "spx.xml")
    @ResponseBody
    public void sendSingleProdFileToMIIT(HttpServletRequest req, HttpServletResponse res) {
        sendFile(req, res,"spx");
    }

    /**
     * 发送文件到工信部 单品文件
     * @param req
     * @param res
     */
    @RequestMapping(value = "pvx.xml")
    @ResponseBody
    public void sendProdVarietyFileToMIIT(HttpServletRequest req, HttpServletResponse res) {
        sendFile(req, res,"pvx");
    }

    private void sendFile(HttpServletRequest req, HttpServletResponse res,String what) {
        String midPath = getFileDir();
        //XML文件输出目录
        String xmlMidPath = sysProperties.getProperty("SOURCEXMLOUTPUTPATH") + File.separator + midPath;
        String fileName =null;
        logger.info("工信部XML文件输出目录:\"{}\"",xmlMidPath);
        if("pvx".equals(what)){
             fileName = "prodvariety.xml";
            try {
                createFile(xmlMidPath,"prodvariety.txt");
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("生成品种标记文件出错",e);
            }
        }else{
             fileName = "singleprod.xml";
            try {
                createFile(xmlMidPath,"singleprod.txt");
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("生成单品标记文件出错",e);
            }
        }

        String xmlOutputFile = xmlMidPath + fileName;
        int index=xmlOutputFile.lastIndexOf(File.separator);
        String parentPath=xmlOutputFile.substring(0,index);
        Path dirPath = Paths.get(parentPath);
        File targetFile=new File(xmlOutputFile);
        if (!Files.exists(dirPath)||!targetFile.exists()) {
            logger.error("文件未找到-"+fileName);
            ServletOutputStream out = null;
            try {
                out = res.getOutputStream();
                out.write("404".getBytes());
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
        MIITDownloadApi.downLoadFile(req, res, new File(xmlOutputFile));
    }

    /**
     * 创建文件
     * @param xmlMidPath
     * @param fileName
     * @throws IOException
     */
    private void createFile(String xmlMidPath,String fileName) throws IOException {
        String xmlOutputFile = xmlMidPath  + fileName;
        int index=xmlOutputFile.lastIndexOf(File.separator);
        String parentPath=xmlOutputFile.substring(0,index);
        Path dirPath = Paths.get(parentPath);
        File targetFile=new File(xmlOutputFile);
        if (!Files.exists(dirPath)||!targetFile.exists()){
            Files.createFile(Paths.get(xmlOutputFile));
        }
    }

    /**
     * 获取文件存放目录，包含当前年月日
     *
     * @return 文件目录
     */
    private String getFileDir() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        String fileDir = calendar.get(Calendar.YEAR) + "/" + (calendar.get(Calendar.MONTH) + 1) + "/" + ThreadLocalDateUtil.getDateFormat().format(calendar.getTime()) + "/";
        return fileDir;
    }

    public static void downLoadFile(HttpServletRequest req, HttpServletResponse response, File file) {
        if (file != null && file.exists()) {
            ServletOutputStream out = null;
            String fileName = "";
            try {
                String ua = req.getHeader("User-Agent");
                if (ua.toUpperCase().indexOf("FIREFOX") > -1) {
                    fileName = new String(file.getName().getBytes("UTF-8"), "ISO-8859-1");
                } else {
                    fileName = URLEncoder.encode(file.getName(), "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                fileName = file.getName();
            }

            try {
                response.reset();
                response.setContentType("application/octet-stream; charset=utf-8");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                out = response.getOutputStream();
                out.write(FileUtils.readFileToByteArray(file));
                out.flush();
            } catch (IOException var12) {
                var12.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException var11) {
                        var11.printStackTrace();
                    }
                }

            }

        }
    }
}
