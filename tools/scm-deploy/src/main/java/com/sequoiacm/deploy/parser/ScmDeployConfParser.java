package com.sequoiacm.deploy.parser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmDeployConfParser {
    private static final Logger logger = LoggerFactory.getLogger(ScmDeployConfParser.class);
    private File confFile;
    private Map<String, List<BSONObject>> seactionBsonRecords;

    public ScmDeployConfParser(String filePath) throws IOException {
        confFile = new File(filePath);
        SeactionParser seactionParser = new SeactionParser(confFile);
        parse(seactionParser);
    }

    private void parse(SeactionParser seactionParser) throws IOException {
        seactionBsonRecords = new HashMap<>();
        List<String> seactionNames = seactionParser.getSeactions();
        for (String seactionName : seactionNames) {
            Reader seactionReader = seactionParser.getSeaction(seactionName);
            try {
                List<BSONObject> records = parseSeaction(seactionReader);
                seactionBsonRecords.put(seactionName, records);
            }
            finally {
                seactionReader.close();
            }
        }

        // seactionBsonRecords = new HashMap<>();
        // for (String seactionName : seactionNames) {
        // relapceRef(seactionName, new RefParseContext(seactionRecords,
        // seactionBsonRecords));
        // }

        if (logger.isDebugEnabled()) {
            for (Entry<String, List<BSONObject>> entry : seactionBsonRecords.entrySet()) {
                logger.debug("seaction parse result:{}", entry.getKey());
                for (BSONObject r : entry.getValue()) {
                    logger.debug(r.toString());
                }
            }
        }
    }

    private void relapceRef(String seactionName, RefParseContext context) {
        if (context.getProcessedSeactionRecords().containsKey(seactionName)) {
            return;
        }

        if (!context.getSrcSeactionRecords().containsKey(seactionName)) {
            throw new IllegalArgumentException("unknown reference seaction name:" + seactionName);
        }

        if (context.isCircularReference(seactionName)) {
            throw new IllegalArgumentException(
                    "circular reference:refrenceSeactionPath=" + context.getReferencePath());
        }

        context.addReferenceToPath(seactionName);

        List<BSONObject> bsonRecords = new ArrayList<>();
        for (Map<String, String> record : context.getSrcSeactionRecords().get(seactionName)) {
            BasicBSONObject bsonRecord = new BasicBSONObject();
            for (String key : record.keySet()) {
                String value = record.get(key);
                boolean isRef = Pattern.matches("^\\[[a-zA-Z0-9]+\\]\\[[0-9]+\\]$", value);
                if (isRef) {
                    BSONObject refBson = parseRef(value, context);
                    bsonRecord.put(key, refBson);
                    continue;
                }
                bsonRecord.put(key, value);
            }
            bsonRecords.add(bsonRecord);
        }
        context.getProcessedSeactionRecords().put(seactionName, bsonRecords);
        context.removeReferenceFromPath(seactionName);
    }

    private BSONObject parseRef(String ref, RefParseContext context) {
        String[] seactionNameAndNum = ref.split("\\]\\[");
        String seactionName = seactionNameAndNum[0].substring(1);
        int num = Integer
                .valueOf(seactionNameAndNum[1].substring(0, seactionNameAndNum[1].length() - 1));
        relapceRef(seactionName, context);
        List<BSONObject> seactionRecords = context.getProcessedSeactionRecords().get(seactionName);
        if (seactionRecords.size() <= num || num < 0) {
            throw new IllegalArgumentException("index out of boud:seaction=" + seactionName
                    + ",index=" + num + ", maxIndex=" + (seactionRecords.size() - 1));
        }
        return seactionRecords.get(num);

    }

    private List<BSONObject> parseSeaction(Reader seactionReader) throws IOException {
        List<BSONObject> ret = new ArrayList<>();
        CSVParser csvParser = new CSVParser(seactionReader,
                CSVFormat.EXCEL.withHeader().withDelimiter(',').withEscape('\\').withQuote('\'')
                        .withTrim().withIgnoreSurroundingSpaces());
        try {
            for (CSVRecord record : csvParser) {
                Map<String, String> map = record.toMap();
                Map<String, String> formateMap = new HashMap<>();

                for (Entry<String, String> entry : map.entrySet()) {
                    formateMap.put(entry.getKey().trim(), entry.getValue().trim());
                }
                ret.add(new BasicBSONObject(formateMap));
            }
            return ret;
        }
        finally {
            csvParser.close();
        }
    }

    public List<String> getSeactionNames() {
        return new ArrayList<>(seactionBsonRecords.keySet());
    }

    public <T> List<T> getSeaction(String seactionName, ConfCoverter<T> converter) {
        List<BSONObject> bsons = seactionBsonRecords.get(seactionName);
        if (bsons == null) {
            return null;
        }
        List<T> ret = new ArrayList<>();
        for (BSONObject bson : bsons) {
            try {
                ret.add(converter.convert(bson));
            }
            catch (Exception e) {
                throw new IllegalArgumentException("failed to parse conf:seaction=" + seactionName
                        + ", record=" + bson + ", causeby=" + e.getMessage(), e);
            }
        }
        return ret;
    }

    public <T> List<T> getSeactionWithCheck(String seactionName, ConfCoverter<T> converter) {
        List<T> ret = getSeaction(seactionName, converter);
        if (ret == null) {
            throw new IllegalArgumentException("no such seaction:" + seactionName);
        }
        return ret;
    }

}
