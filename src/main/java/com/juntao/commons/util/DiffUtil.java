package com.juntao.commons.util;

import com.juntao.commons.annotation.NotDiffField;
import com.juntao.commons.dto.DiffDto;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by HouKun on 2017/2/3.
 */
public class DiffUtil {

    public static List<DiffDto> diffObject(Object oldClass, Object newClass) throws Exception{
        List<DiffDto> list=new ArrayList<DiffDto>();
        //获取对象的class
        Class<?> clazz1 = oldClass.getClass();
        Class<?> clazz2 = newClass.getClass();
        //获取对象的属性列表
        Field[] field1 = clazz1.getDeclaredFields();
        Field[] field2 = clazz2.getDeclaredFields();
        //遍历属性列表field1
        for(int i=0;i<field1.length;i++){
            if (field1[i].getAnnotation(NotDiffField.class) != null) {
                continue;
            }
            //遍历属性列表field2
            for(int j=0;j<field2.length;j++){
                //如果注解为NotDiffField则不
                if (field2[i].getAnnotation(NotDiffField.class) != null) {
                    continue;
                }
                //如果field1[i]属性名与field2[j]属性名内容相同
                if(field1[i].getName().equals(field2[j].getName())){
                    if(field1[i].getName().equals(field2[j].getName())){
                        field1[i].setAccessible(true);
                        field2[j].setAccessible(true);
                        //如果field1[i]属性值与field2[j]属性值内容不相同
                        if (!compareTwoValue(field1[i].get(oldClass), field2[j].get(newClass))){
                            DiffDto diffDto = new DiffDto();
                            diffDto.setFieldName(field1[i].getName());
                            diffDto.setOldValue(field1[i].get(oldClass));
                            diffDto.setNewValue(field2[j].get(newClass));
                            list.add(diffDto);
                        }
                        break;
                    }
                }
            }
        }
        return list;
    }


    private static boolean compareTwoValue(Object object1,Object object2){

        if(object1==null&&object2==null){
            return true;
        }
        if(object1==null&&object2!=null){
            return false;
        }
        if(object1.equals(object2)){
            return true;
        }
        return false;
    }


}
