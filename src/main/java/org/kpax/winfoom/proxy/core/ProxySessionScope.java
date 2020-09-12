/*
 *  Copyright (c) 2020. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.proxy.core;

import org.kpax.winfoom.util.InputOutputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A proxy session {@link Scope} implementation.
 *
 * @see org.kpax.winfoom.annotation.ProxySessionScope
 */
public class ProxySessionScope implements Scope {

    /**
     * The scope name.
     */
    public static final String NAME = "proxySession";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The beans repository.
     */
    private final Map<String, Object> scopedBeans = new ConcurrentHashMap<>();

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Object scopedObject = scopedBeans.get(name);
        if (scopedObject == null) {
            logger.debug("Creating an instance of proxySession bean {}", name);
            scopedObject = objectFactory.getObject();
            scopedBeans.put(name, scopedObject);
        }
        return scopedObject;

    }

    @Override
    public Object remove(String name) {
        logger.debug("Remove the instance of proxySession bean {}", name);
        return scopedBeans.remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable runnable) {
        logger.debug("ProxySessionScope does not support destruction callbacks");
    }

    @Override
    public Object resolveContextualObject(String s) {
        return null;
    }

    @Override
    public String getConversationId() {
        return null;
    }


    void clear() {
        logger.debug("Clear the proxySession scope: found {} beans", scopedBeans.size());

        // Close all the AutoCloseable beans in ordered fashion
        scopedBeans.values().stream().sorted(AnnotationAwareOrderComparator.INSTANCE).forEach(bean -> {
            logger.debug("proxySession bean type {}", bean.getClass());
            if (bean instanceof AutoCloseable) {
                InputOutputs.close((AutoCloseable) bean);
            }
        });
        scopedBeans.clear();
    }
}
