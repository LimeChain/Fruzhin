package com.limechain.rpc.http.server;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// Not used anywhere currently. Could be helpful in the future.
// Pattern from: https://confluence.jaytaala.com/display/TKB/Super+simple+approach+to+accessing+Spring+beans+from+non-Spring+managed+classes+and+POJOs
@Component
public class HttpRpcContext implements ApplicationContextAware {

    private static ApplicationContext context;

    /**
     * Returns the Spring managed bean instance of the given class type if it exists.
     * Returns null otherwise.
     *
     * @param beanClass
     * @return
     */
    public static <T extends Object> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    /**
     * Private method context setting (better practice for setting a static field in a bean instance).
     */
    private static synchronized void setContext(ApplicationContext context) {
        HttpRpcContext.context = context;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        // store ApplicationContext reference to access required beans later on
        setContext(context);
    }
}
