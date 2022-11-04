package com.sequoiacm.infrastructure.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

//Adjust eurekaAutoServiceRegistration destroy after springClientFactory
public class SpringClientFactoryPostProcess implements BeanFactoryPostProcessor {

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        String contextName = "springClientFactory";
        String beanName = "eurekaAutoServiceRegistration";
        if (containsBeanDefinition(beanFactory, contextName,beanName)) {
            BeanDefinition definition = beanFactory.getBeanDefinition(contextName);
            definition.setDependsOn(beanName);
        }
    }

    private boolean containsBeanDefinition(final ConfigurableListableBeanFactory beanFactory,
            String... beanName) {
        for (String name : beanName) {
            if (!beanFactory.containsBeanDefinition(name)) {
                return false;
            }
        }
        return true;
    }
}
