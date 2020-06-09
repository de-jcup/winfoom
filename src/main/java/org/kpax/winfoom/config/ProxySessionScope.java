package org.kpax.winfoom.config;

import org.kpax.winfoom.util.InputOutputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxySessionScope implements Scope, AutoCloseable {

    public static final String NAME = "proxySession";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, Object> scopedBeans = new ConcurrentHashMap<>();

    private final Map<String, Runnable> destructionCallbacks = new ConcurrentHashMap<>();

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Object scopedObject = scopedBeans.get(name);
        if (scopedObject == null) {
            scopedObject = objectFactory.getObject();
            scopedBeans.put(name, scopedObject);
        }
        return scopedObject;

    }

    @Override
    public Object remove(String name) {
        return scopedBeans.remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable runnable) {
        destructionCallbacks.put(name, runnable);
    }

    @Override
    public Object resolveContextualObject(String s) {
        return null;
    }

    @Override
    public String getConversationId() {
        return null;
    }


    @Override
    public void close() {
        logger.debug("Clear the proxySession scope: found {} beans", scopedBeans.size());
        scopedBeans.values().stream().sorted(AnnotationAwareOrderComparator.INSTANCE).forEach(bean -> {
            logger.debug("Bean type {}", bean.getClass());
            if (bean instanceof AutoCloseable) {
                InputOutputs.close((AutoCloseable) bean);
            }
        });;
        scopedBeans.clear();
    }
}
