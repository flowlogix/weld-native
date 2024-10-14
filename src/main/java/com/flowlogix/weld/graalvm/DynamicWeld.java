/*
 * Copyright (C) 2011-2024 Flow Logix, Inc. All Rights Reserved.
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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import org.graalvm.nativeimage.ImageInfo;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.function.Supplier;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Uses ByteBuddy to patch ProxyFactory.
 * {@link #initialize()} needs to be called to initialize from main()
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class DynamicWeld {
    public static void initialize() {
        DynamicProxyFactory.initialize();
    }

    static <TT> TT substituteMethod(Class<?> originalClass, Class<?> overriddenClass, String methodName,
                                   Supplier<TT> additionalSetup) {
        try {
            if (ImageInfo.isExecutable() || ImageInfo.isSharedLibrary()) {
                return null;
            }
        } catch (NoClassDefFoundError ignore) { }
        ByteBuddyAgent.install();
        new ByteBuddy().redefine(originalClass)
                .method(named(methodName))
                .intercept(MethodDelegation.to(overriddenClass))
                .make().load(originalClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
        return additionalSetup.get();
    }

    static MethodHandle createInnerConstructorHandle(Class<?> originalClass, String innerClassName,
                                                     Class<?>... parameters) {
        try {
            Class<?> inner = Class.forName("%s$%s".formatted(originalClass.getName(), innerClassName));
            Constructor<?> constructor = inner.getDeclaredConstructor(parameters);
            constructor.setAccessible(true);
            return MethodHandles.lookup().unreflectConstructor(constructor);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }
}
