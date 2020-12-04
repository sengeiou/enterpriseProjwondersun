package com.proj.billpush;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebt.billqueue.BillPushQueueHandler;
import com.arlen.ebt.billqueue.BillQueueItem;
import com.arlen.ebt.dto.BillPushInfoDTO;
import com.arlen.ebt.service.BillPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

/**
 * Created by Johnny on 2017/2/22.
 */
public class billPushToEbmsHandler implements BillPushQueueHandler {
    private static transient final Logger log = LoggerFactory.getLogger(billPushToEbmsHandler.class);
    @Resource
    private BillPushService billPushService;
    @Resource(name = "sysProperties")
    private Properties sysProperties;

    @Override
    public void pushHandler(BillQueueItem billQueueItem) throws RuntimeException {
        if (StringUtils.isEmpty(billQueueItem.getBillId())) {
            throw new RuntimeException("BillId  is  Empty");
        }
        APIResult<BillPushInfoDTO> result = billPushService.getBillPushInfoDTO(billQueueItem.getBillId());
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getErrMsg());
        }
        //获取接口地址
        String URL = sysProperties.getProperty("ebt.pushBillURL");
        if (StringUtils.isEmpty(URL)) {
            throw new RuntimeException("需要推送的服务器接口地址未配置!!");
        }
        //拼接参数
        BillPushInfoDTO pushInfoDTO = result.getData();
        String str =  JSON.toJSONString(pushInfoDTO);
        String param = "bill=" + str + "&pwd=wondersun@2017";
        JSONObject parse = (JSONObject) JSON.parse(sendPost(URL, param));
        if (parse.getInteger("code")!=0){
            throw new RuntimeException(parse.getString("errMsg"));
        }
    }

    /**
     * 向指定的URL 发生POST请求
     *
     * @param url   url
     * @param param 参数
     * @return
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);     // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            out.print(param);     // 发送请求参数
            out.flush();       // flush输出流的缓冲
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(),"utf-8"));      // 定义BufferedReader输入流来读取URL的响应
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        } finally {  //关闭输出流、输入流
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }


}
