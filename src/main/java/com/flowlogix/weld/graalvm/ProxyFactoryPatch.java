package com.flowlogix.weld.graalvm;

import jakarta.enterprise.inject.spi.Bean;
import org.jboss.weld.bean.AbstractProducerBean;
import org.jboss.weld.util.Proxies;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helidon patch for Weld ProxyFactory
 * Patches {@link org.jboss.weld.bean.proxy.ProxyFactory#createCompoundProxyName(String, Bean, Proxies.TypeInfo, StringBuilder)}
 */
class ProxyFactoryPatch {
    private static final Map<String, String> NAME_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> INVERTED_NAME_MAP = new ConcurrentHashMap<>();

    @FunctionalInterface
    interface ReturnValueSupplier {
        Object supply(String proxyPackage, String className, Bean<?> bean);
    }

    static Object createCompoundProxyName(String contextId, Bean<?> bean, Proxies.TypeInfo typeInfo, StringBuilder name,
                                 ReturnValueSupplier returnValueSupplier) {
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
        return returnValueSupplier.supply(proxyPackage, className, bean);
    }

    private static String uniqueName(Class<?> type) {
        // check if we have a name
        String className = type.getName();
        String uniqueName = NAME_MAP.get(className);
        if (null != uniqueName) {
            return uniqueName;
        }

        return simpleName(className, type.getSimpleName());
    }

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
