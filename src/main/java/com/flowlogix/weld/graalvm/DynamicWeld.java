package com.flowlogix.weld.graalvm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import org.graalvm.nativeimage.ImageInfo;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Uses ByteBuddy to patch ProxyFactory.
 * {@link #initialize()} needs to be called to initialize from main()
 */
public class DynamicWeld {
    public static void initialize() {
        DynamicProxyFactory.initialize();
    }

    static void substituteMethod(Class<?> originalClass, Class<?> overriddenClass, String methodName) {
        try {
            if (ImageInfo.isExecutable() || ImageInfo.isSharedLibrary()) {
                return;
            }
        } catch (NoClassDefFoundError ignore) { }
        ByteBuddyAgent.install();
        new ByteBuddy().redefine(originalClass)
                .method(named(methodName))
                .intercept(MethodDelegation.to(overriddenClass))
                .make().load(originalClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    }

    static MethodHandle createInnerConstructorHandle(Class<?> originalClass, String innerClassName,
                                                     Class<?> firstParam, Class<?>... restParams) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(originalClass, MethodHandles.lookup());
            Class<?> inner = Class.forName("%s$%s".formatted(originalClass.getName(), innerClassName));
            return lookup.findConstructor(inner, MethodType.methodType(void.class, firstParam, restParams));
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }
}
