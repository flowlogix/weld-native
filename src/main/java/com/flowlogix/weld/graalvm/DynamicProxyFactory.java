package com.flowlogix.weld.graalvm;

import jakarta.enterprise.inject.spi.Bean;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.jboss.weld.bean.proxy.ProxyFactory;
import org.jboss.weld.util.Proxies;
import java.lang.invoke.MethodHandle;
import static com.flowlogix.weld.graalvm.DynamicWeld.createInnerConstructorHandle;
import static com.flowlogix.weld.graalvm.DynamicWeld.substituteMethod;

class DynamicProxyFactory {
    private static MethodHandle constructor;

    public static class Substitution {
        @RuntimeType
        public static Object createCompoundProxyName(String contextId, Bean<?> bean, Proxies.TypeInfo typeInfo,
                                                     StringBuilder name) {
            return ProxyFactoryPatch.createCompoundProxyName(contextId, bean, typeInfo, name,
                    DynamicProxyFactory::supply);
        }
    }

    static void initialize() {
        constructor = substituteMethod(ProxyFactory.class, Substitution.class, "createCompoundProxyName",
                () -> createInnerConstructorHandle(ProxyFactory.class, "ProxyNameHolder",
                        String.class, String.class, Bean.class));
    }

    private static Object supply(String proxyPackage, String className, Bean<?> bean) {
        try {
            return constructor.invokeWithArguments(proxyPackage, className, bean);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
