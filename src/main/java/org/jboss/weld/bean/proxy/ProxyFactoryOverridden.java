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

package org.jboss.weld.bean.proxy;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.enterprise.inject.spi.Bean;
import org.jboss.weld.bean.AbstractProducerBean;
import org.jboss.weld.util.Proxies.TypeInfo;
import org.jboss.weld.bean.proxy.ProxyFactoryNew.ProxyNameHolder;

public class ProxyFactoryOverridden {
    /*
     * Helidon addition
     */
    private static final Map<String, String> NAME_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> INVERTED_NAME_MAP = new ConcurrentHashMap<>();

    /*
     * Helidon modification (original method with different body)
     */
     static ProxyNameHolder createCompoundProxyName(String contextId, Bean<?> bean, TypeInfo typeInfo, StringBuilder name) {
        String className;
        String proxyPackage = null;
        // we need a sorted collection without repetition, hence LinkedHashSet
        final Set<String> interfaces = new LinkedHashSet<>();
        // for producers, try to determine the most specific class and make sure the proxy starts with the same package and class
        if (bean != null && bean instanceof AbstractProducerBean) {
            Class<?> mostSpecificClass = ((AbstractProducerBean) bean).getType();
            proxyPackage = mostSpecificClass.getPackage().getName();
            if (mostSpecificClass.getDeclaringClass() != null) {
                interfaces.add(uniqueName(mostSpecificClass.getDeclaringClass()));
            }
            interfaces.add(uniqueName(mostSpecificClass));
        }
        final Set<String> declaringClasses = new HashSet<>();
        for (Class<?> type : typeInfo.getInterfaces()) {
            Class<?> declaringClass = type.getDeclaringClass();
            if (declaringClass != null && declaringClasses.add(declaringClass.getSimpleName())) {
                interfaces.add(uniqueName(declaringClass));
            }
            interfaces.add(uniqueName(type));
            if (proxyPackage == null) {
                proxyPackage = typeInfo.getPackageNameForClass(type);
            }
        }
        // no need to sort the set, because we copied and already sorted one
        Iterator<String> iterator = interfaces.iterator();
        while (iterator.hasNext()) {
            name.append(iterator.next());
            if (iterator.hasNext()) {
                name.append("$");
            }

        }
        // we use unique names, we should never get a duplicity
        className = name.toString();
        return new ProxyNameHolder(proxyPackage, className, bean);
    }

    /*
     * Helidon addition
     */
    private static String uniqueName(Class<?> type) {
        // check if we have a name
        String className = type.getName();
        String uniqueName = NAME_MAP.get(className);
        if (null != uniqueName) {
            return uniqueName;
        }

        return simpleName(className, type.getSimpleName());
    }

    /*
     * Helidon addition
     */
    private static String simpleName(String className, String simpleName) {
        String used = INVERTED_NAME_MAP.get(simpleName);
        if (null == used) {
            INVERTED_NAME_MAP.put(simpleName, className);
            NAME_MAP.put(className, simpleName);
            return simpleName;
        }

        // simple name is already used by another class
        return prefixedName(className, toPrefixed(className, simpleName));
    }

    /*
     * Helidon addition
     */
    private static String prefixedName(String className, String prefixedName) {
        String used = INVERTED_NAME_MAP.get(prefixedName);
        if (null == used) {
            INVERTED_NAME_MAP.put(prefixedName, className);
            NAME_MAP.put(className, prefixedName);
            return prefixedName;
        }
        INVERTED_NAME_MAP.put(className, className);
        NAME_MAP.put(className, className);
        return className;
    }

    /*
     * Helidon addition
     */
    private static String toPrefixed(String className, String simpleName) {
        String[] split = className.split("\\.");
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < (split.length - 1); i++) {
            String s = split[i];
            name.append(s.charAt(0));
            name.append('$');
        }
        name.append(simpleName);
        return name.toString();
    }
}
