package com.sequoiacm.fulltext.es.client.base;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EsClientAutoConfig {
    private static final Logger logger = LoggerFactory.getLogger(EsClientAutoConfig.class);
    @Bean
    public EsClientConfig esClientConfig() {
        return new EsClientConfig();
    }

    @Bean
    public EsClient esClient(EsClientConfig config) throws Exception {
        logger.info("scan adapter path: {}", config.getAdapterPath());
        List<URL> adapterJarUrlList = getAdapterJarUrls(config.getAdapterPath());
        URL[] urlArray = adapterJarUrlList.toArray(new URL[0]);
        URLClassLoader urlClassLoader = new URLClassLoader(urlArray,
                EsClientAutoConfig.class.getClassLoader());
        List<URL> scmJarUrl = adapterJarUrlList.stream()
                .filter(url -> new File(url.getFile()).getName().contains("sequoiacm"))
                .collect(Collectors.toList());
        Reflections refs = new Reflections(
                new ConfigurationBuilder().addUrls(scmJarUrl).addClassLoaders(urlClassLoader));
        Set<Class<? extends EsClientFactory>> esClientFactorySet = refs
                .getSubTypesOf(EsClientFactory.class);
        if (esClientFactorySet == null || esClientFactorySet.size() <= 0) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "es client factory implement not found: " + EsClientFactory.class);
        }

        if (esClientFactorySet.size() > 1) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "found multiple es client factory implement: " + esClientFactorySet.toString());
        }
        Class<? extends EsClientFactory> esClientFactoryClass = esClientFactorySet.iterator()
                .next();
        logger.info("using es client factory implement: {}", esClientFactoryClass.getName());
        return esClientFactoryClass.newInstance().createEsClient(config);
    }

    private List<URL> getAdapterJarUrls(String adapterPath) throws MalformedURLException {
        ArrayList<URL> ret = new ArrayList<>();
        File adapterDir = new File(adapterPath);
        for (File jar : adapterDir.listFiles()) {
            if (jar.isFile() && jar.getName().endsWith(".jar")) {
                ret.add(new URL("file:" + jar.getAbsolutePath()));
            }
        }
        return ret;
    }
}
