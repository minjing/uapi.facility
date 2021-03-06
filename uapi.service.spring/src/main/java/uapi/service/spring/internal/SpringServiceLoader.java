/**
 * Copyright (C) 2010 The UAPI Authors
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at the LICENSE file.
 *
 * You must gained the permission from the authors if you want to
 * use the project into a commercial product
 */

package uapi.service.spring.internal;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uapi.config.annotation.Config;
import uapi.common.ArgumentChecker;
import uapi.service.IServiceLoader;
import uapi.service.annotation.OnActivate;
import uapi.service.annotation.Service;
import uapi.service.spring.ISpringServiceLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * The service used to load Spring bean into the framework
 */
@Service(IServiceLoader.class)
public class SpringServiceLoader implements ISpringServiceLoader {

    private static final int PRIORITY   = 100;

    private final Map<String, Object> _beanCache = new HashMap<>();

    @Config(path="spring.config")
    protected String _cfgFile;

    private ApplicationContext _ctx;

    @OnActivate
    public void init() {
        this._ctx = new ClassPathXmlApplicationContext(new String[] { this._cfgFile });
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getId() {
        return NAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T load(
            final String serviceId,
            final Class<?> serviceType) {
        ArgumentChecker.notEmpty(serviceId, "serviceId");
        T bean = (T) this._beanCache.get(serviceId);
        if (bean != null) {
            return bean;
        }
        bean = (T) this._ctx.getBean(serviceId);
        this._beanCache.put(serviceId, bean);
        return bean;
    }

    @Override
    public void register(IServiceReadyListener iServiceReadyListener) {
        // Don't support dynamic service register, ignore it.
    }
}
