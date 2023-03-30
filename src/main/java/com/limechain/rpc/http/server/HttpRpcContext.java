package com.limechain.rpc.http.server;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @deprecated Spring application pattern allowing runtime code to access any bean in the application
 * <p>
 * Not used anywhere currently. Could be helpful in the future.
 * <p>
 * Pattern taken from <a href="https://bit.ly/3yqYkX0">here</a>
 */
@Deprecated
@Component
public class HttpRpcContext implements ApplicationContextAware {

    private static ApplicationContext context;

    /**
     * Returns the Spring managed bean instance of the given class type if it exists.
     * Returns null otherwise.
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
