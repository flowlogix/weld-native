/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc. and/or its affiliates, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowlogix.weld.graalvm;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import jakarta.enterprise.inject.spi.Bean;
import org.jboss.weld.bean.proxy.ProxyFactory;
import org.jboss.weld.util.Proxies.TypeInfo;

/**
 * Patches {@link ProxyFactory} via GraalVM substitutions. No initialization needed.
 */
@TargetClass(ProxyFactory.class)
final class GraalProxyFactory {
    @TargetClass(value = ProxyFactory.class, innerClass = "ProxyNameHolder")
    static final class ProxyNameHolder {
        @Alias
        public ProxyNameHolder(String proxyPackage, String className, Bean<?> bean) { }
    }

    /*
     * Helidon modification (original method with different body)
     */
    @Substitute
    static ProxyNameHolder createCompoundProxyName(String contextId, Bean<?> bean, TypeInfo typeInfo, StringBuilder name) {
        return (ProxyNameHolder) ProxyFactoryPatch.createCompoundProxyName(contextId, bean, typeInfo, name, ProxyNameHolder::new);
    }
}
