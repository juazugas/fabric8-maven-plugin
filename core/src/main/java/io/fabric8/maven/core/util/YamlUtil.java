/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.maven.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import org.yaml.snakeyaml.Yaml;

class YamlUtil {
    static Properties getPropertiesFromYamlResource(URL resource) {
        if (resource != null) {
            try (InputStream yamlStream = resource.openStream()) {
                Yaml yaml = new Yaml();
                SortedMap<?, ?> source = yaml.loadAs(yamlStream, SortedMap.class);
                Properties properties = new Properties();
                if (source != null) {
                    try {
                        properties.putAll(getFlattenedMap(source));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(String.format("Spring Boot configuration file %s is not formatted correctly. %s",
                            resource.toString(), e.getMessage()));
                    }
                }
                return properties;
            } catch (IOException e) {
                throw new IllegalStateException("Error while reading Yaml resource from URL " + resource, e);
            }
        }
        return new Properties();
    }

    /**
     * Build a flattened representation of the Yaml tree. The conversion is compliant with the thorntail spring-boot rules.
     */
    private static Map<String, Object> getFlattenedMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    private static void buildFlattenedMap(Map<String, Object> result, Map<?, ?> source, String path) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object keyObject = entry.getKey();

            String key;
            if (keyObject instanceof String) {
                key = (String) keyObject;
            } else if (keyObject instanceof Number) {
                key = String.valueOf(keyObject);
            } else {
                // If user creates a wrong application.yml then we get a runtime classcastexception
                throw new IllegalArgumentException(String.format("Expected to find a key of type String but %s with content %s found.",
                    keyObject.getClass(), keyObject.toString()));
            }

            if (path !=null && path.trim().length()>0) {
                if (key.startsWith("[")) {
                    key = path + key;
                }
                else {
                    key = path + "." + key;
                }
            }
            Object value = entry.getValue();
            if (value instanceof Map) {

                Map<?, ?> map = (Map<?, ?>) value;
                buildFlattenedMap(result, map, key);
            }
            else if (value instanceof Collection) {
                Collection<?> collection = (Collection<?>) value;
                int count = 0;
                for (Object object : collection) {
                    buildFlattenedMap(result,
                        Collections.singletonMap("[" + (count++) + "]", object), key);
                }
            }
            else {
                result.put(key, (value != null ? value.toString() : ""));
            }
        }
    }

}
