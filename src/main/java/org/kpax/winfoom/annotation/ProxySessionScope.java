package org.kpax.winfoom.annotation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a Spring managed bean as proxySession scoped.
 * <p>Note that the target bean is CGLIB proxied.
 */
@Qualifier
@Scope(value = org.kpax.winfoom.proxy.ProxySessionScope.NAME, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxySessionScope {
}