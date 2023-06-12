package com.sequoiacm.infrastructure.config.core.msg;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdater;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.infrastructure.common.RefUtil;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;

@Component
public class ConfigEntityTranslator {

    private final ObjectMapper objectMapper;

    private final Map<String, Class<? extends Config>> configClazzMap = new HashMap<>();
    private final Map<String, Class<? extends ConfigFilter>> configFilterClazzMap = new HashMap<>();

    private final Map<String, Class<? extends ConfigUpdater>> configUpdatorClazzMap = new HashMap<>();

    private final Map<String, Class<? extends NotifyOption>> notifyOptionClazzMap = new HashMap<>();

    public ConfigEntityTranslator() {
        registerClass(Config.class, configClazzMap);
        registerClass(ConfigFilter.class, configFilterClazzMap);
        registerClass(ConfigUpdater.class, configUpdatorClazzMap);
        registerClass(NotifyOption.class, notifyOptionClazzMap);

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.AUTO_DETECT_GETTERS, false);
        objectMapper.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(BSONObject.class, new BSONObjectJsonDeserializer());
        module.addDeserializer(BasicBSONList.class, new BSONListJsonDeserializer());
        objectMapper.registerModule(module);
    }

    private <T> void registerClass(Class<T> clazz, Map<String, Class<? extends T>> map) {
        Set<Class<? extends T>> subClasses = RefUtil.getSubTypesOf(clazz);
        for (Class<? extends T> subClazz : subClasses) {
            BusinessType businessType = subClazz.getAnnotation(BusinessType.class);
            if (businessType == null) {
                throw new IllegalArgumentException("no businessType annotation:clazz=" + subClazz);
            }
            Class<? extends T> old = map.put(businessType.value(), subClazz);
            if (old != null) {
                throw new IllegalArgumentException("duplicate businessType:businessType="
                        + businessType.value() + ",clazz=" + subClazz + ", " + old);
            }
        }
    }

    public Config fromConfigBSON(String businessType, BSONObject bson) {
        Class<? extends Config> clazz = configClazzMap.get(businessType);
        if (clazz == null) {
            throw new IllegalArgumentException("no such config class:businessType=" + businessType);
        }
        return objectMapper.convertValue(bson, clazz);
    }

    public ConfigFilter fromConfigFilterBSON(String businessType, BSONObject bson) {
        Class<? extends ConfigFilter> clazz = configFilterClazzMap.get(businessType);
        if (clazz == null) {
            throw new IllegalArgumentException(
                    "no such config filter class:businessType=" + businessType);
        }
        return objectMapper.convertValue(bson, clazz);
    }

    public ConfigUpdater fromConfigUpdaterBSON(String businessType, BSONObject bson) {
        Class<? extends ConfigUpdater> clazz = configUpdatorClazzMap.get(businessType);
        if (clazz == null) {
            throw new IllegalArgumentException(
                    "no such config updater class:businessType=" + businessType);
        }
        return objectMapper.convertValue(bson, clazz);
    }

    public NotifyOption fromNotifyOptionBSON(String businessType, BSONObject bson) {
        Class<? extends NotifyOption> clazz = notifyOptionClazzMap.get(businessType);
        if (clazz == null) {
            throw new IllegalArgumentException(
                    "no such notify option class:businessType=" + businessType);
        }
        return objectMapper.convertValue(bson, clazz);
    }

    public BSONObject toConfigBSON(Config config) {
        return encodeObj(config);
    }

    public BSONObject toConfigFilterBSON(ConfigFilter filter) {
        return encodeObj(filter);
    }

    public BSONObject toConfigUpdaterBSON(ConfigUpdater updater) {
        return encodeObj(updater);
    }

    public BSONObject toNotifyOptionBSON(NotifyOption option) {
        return encodeObj(option);
    }

    private BSONObject encodeObj(Object o) {
        return objectMapper.convertValue(o, BSONObject.class);
    }

    public static void main(String[] args) {



        ConfigEntityTranslator t = new ConfigEntityTranslator();


        WorkspaceUpdater wu = new WorkspaceUpdater();
        wu.setTagLibTable("taglib");
        wu.setAddDataLocation(new BasicBSONObject("a", "b"));
        BasicBSONList list = new BasicBSONList();
        list.add(new BasicBSONObject("a", "b"));
        list.add(new BasicBSONObject("a1", "b1"));
        wu.setUpdateDataLocation(list);
        BSONObject bson = t.toConfigUpdaterBSON(wu);

        System.out.println(bson);
    }

}

class BSONObjectJsonDeserializer extends StdDeserializer<BSONObject> {

    public BSONObjectJsonDeserializer() {
        super(BSONObject.class);
    }

    @Override
    public BSONObject deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        Map<?, ?> node = p.getCodec().readValue(p, Map.class);
        // 这里没有使用JSON.parse，是因为转换出来的BSONObject，数值类型可能被修改（long -> int）
        return convertMapToBson(node);
    }

    static BSONObject convertMapToBson(Map<?, ?> node) {
        BSONObject bson = new BasicBSONObject();
        for (Map.Entry<?, ?> entry : node.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = convertMapToBson((Map<?, ?>) value);
                bson.put(entry.getKey().toString(), value);
            }
            else if (value instanceof List) {
                value = convertListToBson((List<?>) value);
                bson.put(entry.getKey().toString(), value);
            }
            else {
                if (value != null && !isBasicType(value.getClass())) {
                    throw new IllegalArgumentException(
                            "unsupported type:" + value.getClass() + ", node: " + node);
                }
                bson.put(entry.getKey().toString(), value);
            }
        }
        return bson;
    }

    static boolean isBasicType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return true;
        }
        if (Boolean.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Character.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Number.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (String.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    static BasicBSONList convertListToBson(Collection<?> value) {
        BasicBSONList bsonList = new BasicBSONList();
        for (Object o : value) {
            if (o instanceof Map) {
                bsonList.add(convertMapToBson((Map<?, ?>) o));
            }
            else if (o instanceof Collection) {
                bsonList.add(convertListToBson((Collection<?>) o));
            }
            else {
                bsonList.add(o);
            }
        }
        return bsonList;
    }
}

class BSONListJsonDeserializer extends StdDeserializer<BasicBSONList> {

    public BSONListJsonDeserializer() {
        super(BasicBSONList.class);
    }

    @Override
    public BasicBSONList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<?> node = p.getCodec().readValue(p, List.class);
        // 这里没有使用JSON.parse，是因为转换出来的BSONObject，数值类型可能被修改（long-》int）
        return BSONObjectJsonDeserializer.convertListToBson(node);
    }
}