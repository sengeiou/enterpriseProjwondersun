package com.proj.biz;

import org.apache.commons.beanutils.PropertyUtils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Set;

/**过滤掉JAVABEAN中字段的值，使其为空，减小存储大小，减少网络传输量
 * Created by ehsure on 2016/9/30.
 */
public class BeanValuePartForNull {
    /**
     * 过滤字段使其值为空
     * @param bean 对象
     * @param fieldNameSet 不过滤字段集合
     */
    public static Object filterField(Object bean,Set<String> fieldNameSet){
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(bean.getClass());
        } catch (Exception e1) {
            return bean;
        }
        PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();
        for(PropertyDescriptor property:properties){
            String name=property.getName();
            if(!fieldNameSet.contains(name)&&!name.equals("class")){
                try {
                    PropertyUtils.setProperty(bean, name, null);
                } catch (Exception e) {
                }
            }
        }
        return bean;
    }
}
