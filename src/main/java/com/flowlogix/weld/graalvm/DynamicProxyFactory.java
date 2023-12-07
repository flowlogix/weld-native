package com.flowlogix.weld.graalvm;

import jakarta.enterprise.inject.spi.Bean;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.jboss.weld.bean.proxy.ProxyFactory;
import org.jboss.weld.util.Proxies;
import java.lang.invoke.MethodHandle;
import static com.flowlogix.weld.graalvm.DynamicWeld.createInnerConstructorHandle;
import static com.flowlogix.weld.graalvm.DynamicWeld.substituteMethod;

public class DynamicProxyFactory {
    private static MethodHandle constructor;

    static void initialize() {
        substituteMethod(ProxyFactory.class, DynamicProxyFactory.class, "createCompoundProxyName");
        constructor = createInnerConstructorHandle(ProxyFactory.class, "ProxyNameHolder",
                String.class, String.class, Bean.class);
    }

    @RuntimeType
    public static Object createCompoundProxyName(String contextId, Bean<?> bean, Proxies.TypeInfo typeInfo,
                                                 StringBuilder name) {
        return ProxyFactoryPatch.createCompoundProxyName(contextId, bean, typeInfo, name,
                DynamicProxyFactory::supply);
    }

    private static Object supply(String proxyPackage, String className, Bean<?> bean) {
        try {
            return constructor.invokeWithArguments(proxyPackage, className, bean);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
