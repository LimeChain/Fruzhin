package com.limechain.network.protocol;

import com.limechain.network.protocol.base.BaseController;
import com.limechain.network.protocol.base.BaseEngine;
import com.limechain.network.protocol.base.BaseProtocol;
import io.libp2p.core.Stream;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@UtilityClass
public class BaseUtils {

    public void setProtectedEngineField(BaseController controller, BaseEngine engine) throws NoSuchFieldException, IllegalAccessException {
        Field engineField = BaseController.class.getDeclaredField("engine");
        engineField.setAccessible(true);
        engineField.set(controller, engine);
    }

    public Object callProtectedMethod(BaseProtocol protocol, Stream stream, String methodName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method method = BaseProtocol.class.getDeclaredMethod(methodName, Stream.class);
        method.setAccessible(true);
        return method.invoke(protocol, stream);
    }

    public Stream getProtectedStreamField(BaseController controller)
            throws NoSuchFieldException, IllegalAccessException {

        Field streamField = BaseController.class.getDeclaredField("stream");
        streamField.setAccessible(true);
        return (Stream) streamField.get(controller);
    }
}
