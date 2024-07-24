package com.limechain.rpc.server;

import lombok.extern.java.Log;

/**
 * Spring application pattern allowing runtime code to access any bean in the application
 * <p>
 * Not used anywhere currently. Could be helpful in the future.
 * <p>
 * Pattern taken from <a href="https://bit.ly/3yqYkX0">here</a>
 */
@Log
public class AppBean {

    /**
     * Returns the Spring managed bean instance of the given class type if it exists.
     * Returns null otherwise.
     */
    public static <T extends Object> T getBean(Class<T> beanClass) {
        return (T) CommonConfig.getBean(beanClass);
    }
}
