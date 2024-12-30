/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flowlogix.weld.graalvm;

import jakarta.enterprise.inject.spi.Bean;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.jboss.weld.bean.proxy.ProxyFactory;
import org.jboss.weld.util.Proxies;
import java.lang.invoke.MethodHandle;
import static com.flowlogix.weld.graalvm.DynamicWeld.createInnerConstructorHandle;
import static com.flowlogix.weld.graalvm.DynamicWeld.substituteMethod;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
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
