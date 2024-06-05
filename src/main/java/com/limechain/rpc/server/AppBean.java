package com.limechain.rpc.server;

import lombok.extern.java.Log;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring application pattern allowing runtime code to access any bean in the application
 * <p>
 * Not used anywhere currently. Could be helpful in the future.
 * <p>
 * Pattern taken from <a href="https://bit.ly/3yqYkX0">here</a>
 */
@Component
@Log
public class AppBean implements ApplicationContextAware {

    private static ApplicationContext context;

    /**
     * Returns the Spring managed bean instance of the given class type if it exists.
     * Returns null otherwise.
     */
    public static <T extends Object> T getBean(Class<T> beanClass) {
        if (context == null) {
            log.warning("Application context is not set");
            return null;
        }
        try {
            return context.getBean(beanClass);
        } catch (NoSuchBeanDefinitionException e) {
            log.warning("No bean of type " + beanClass.getName() + " found in the application context");
            return null;
        }
    }

    /**
     * Private method context setting (better practice for setting a static field in a bean instance).
     */
    private static synchronized void setContext(ApplicationContext context) {
        AppBean.context = context;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        // store ApplicationContext reference to access required beans later on
        setContext(context);
    }
}
