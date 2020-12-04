package com.proj.biz;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * SimpleDateFormat安全的时间格式化
 * 减少创建对象的开销,提升性能，又线程安全
 */
public class ThreadLocalDateUtil {
    private static final String date_format = "yyyy-MM-dd";
    private static ThreadLocal<DateFormat> threadLocal = new ThreadLocal<DateFormat>();
 
    public static DateFormat getDateFormat()   
    {  
        DateFormat df = threadLocal.get();  
        if(df==null){  
            df = new SimpleDateFormat(date_format);
            threadLocal.set(df);  
        }  
        return df;  
    }  

    public static String formatDate(Date date) throws ParseException {
        return getDateFormat().format(date);
    }

    public static Date parse(String strDate) throws ParseException {
        return getDateFormat().parse(strDate);
    }   
}