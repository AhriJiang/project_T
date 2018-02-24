package com.juntao.commons.spring.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProxy implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static final <T> T getBean(Class<T> clazz) {
        if (null == clazz) {
            return null;
        }

        return applicationContext.getBean(clazz);
    }
}
