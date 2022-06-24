/*-
 * #%L
 * Magpie API
 * %%
 * Copyright (C) 2021 Open Raven Inc
 * %%
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
 * #L%
 */
/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openraven.magpie.data.utils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class EntityTypeResolver extends TypeIdResolverBase {

    private JavaType baseType;
    private Map<String, Class<?>> typeMap = new HashMap<>();

    @Override
    public void init(JavaType javaType) {
        baseType = javaType;

        Class<?> rawClass = baseType.getRawClass();
        Set<Class<?>> subTypes = getSubClasses(rawClass);

        subTypes.forEach(type -> {
            try {
                Field resourceTypeField = type.getDeclaredField("RESOURCE_TYPE");
                String key = String.valueOf(resourceTypeField.get(null));

                if (typeMap.containsKey(key)) {
                    throw new IllegalStateException("RESOURCE_TYPE:  \"" + key + "\" already exists.");
                }

                typeMap.put(key, type);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalArgumentException(
                        format("Entity %s does not declare RESOURCE_TYPE constant", type.getName()));
            }
        });
    }

    @Override
    public String idFromValue(Object o) {
        return idFromValueAndType(o, o.getClass());
    }

    @Override
    public String idFromValueAndType(Object o, Class<?> aClass) {
        return null;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        if (typeMap.containsKey(id)) {
            return context.constructSpecializedType(baseType, typeMap.get(id));
        }

        throw new IOException("Cannot find class for type id \"" + id + "\"");
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    public static Set<Class<?>> getSubClasses(Class<?> rawClass) {
        Reflections reflections = new Reflections(rawClass.getPackageName());
        Set<Class<?>> abstractSubResources = (Set<Class<?>>) reflections.getSubTypesOf(rawClass);

        return abstractSubResources.stream()
          .map(resourceClass -> (Set<Class<?>>) reflections.getSubTypesOf(resourceClass))
          .flatMap(Set::stream)
          .collect(Collectors.toSet());
    }
}
