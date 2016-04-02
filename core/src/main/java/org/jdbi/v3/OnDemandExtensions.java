/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class OnDemandExtensions {
    private static final Method EQUALS_METHOD;
    private static final Method HASHCODE_METHOD;
    private static final Method TOSTRING_METHOD;

    static {
        try {
            EQUALS_METHOD = Object.class.getMethod("equals", Object.class);
            HASHCODE_METHOD = Object.class.getMethod("hashCode");
            TOSTRING_METHOD = Object.class.getMethod("toString");
        }
        catch (NoSuchMethodException wat) {
            throw new IllegalStateException("JVM error", wat);
        }
    }

    public static <E> E create(DBI dbi, Class<E> extensionType) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (EQUALS_METHOD.equals(method)) {
                return proxy == args[0];
            }

            if (HASHCODE_METHOD.equals(method)) {
                return System.identityHashCode(proxy);
            }

            if (TOSTRING_METHOD.equals(method)) {
                return extensionType + "@" + Integer.toHexString(System.identityHashCode(proxy));
            }

            if ("clone".equals(method.getName()) && method.getParameterCount() == 0) {
                // why not
                return create(dbi, extensionType);
            }

            if ("finalize".equals(method.getName()) && method.getParameterCount() == 0) {
                return null;
            }

            try {
                return dbi.withExtension(extensionType, extension -> method.invoke(extension, args));
            }
            catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        };

        return (E) Proxy.newProxyInstance(extensionType.getClassLoader(), new Class[]{extensionType}, handler);
    }
}
