package com.sequoiacm.deploy.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.sequoiacm.deploy.common.ConfFileDefine;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmConfParser {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfParser.class);
    private File confFile;
    private Map<String, List<BSONObject>> tableSeactionBsonRecords;
    private Map<String, BSONObject> keyValueSeactionBsonRecord;

    public ScmConfParser(String filePath) throws IOException {
        confFile = new File(filePath);
        SeactionParser seactionParser = new SeactionParser(confFile);
        parse(seactionParser);
    }

    private void parse(SeactionParser seactionParser) throws IOException {
        tableSeactionBsonRecords = new HashMap<>();
        keyValueSeactionBsonRecord = new HashMap<>();
        List<String> seactionNames = seactionParser.getSeactions();
        for (String seactionName : seactionNames) {
            Reader seactionReader = seactionParser.getSeaction(seactionName);
            try {
                if (ConfFileDefine.SEACTION_CONFIG.equals(seactionName)) {
                    BSONObject record = parseKeyValueSeaction(seactionReader);
                    keyValueSeactionBsonRecord.put(seactionName, record);
                    continue;
                }
                List<BSONObject> records = parseTableSeaction(seactionReader);
                tableSeactionBsonRecords.put(seactionName, records);
            }
            finally {
                seactionReader.close();
            }
        }

    }

    private List<BSONObject> parseTableSeaction(Reader seactionReader) throws IOException {
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

    private BSONObject parseKeyValueSeaction(Reader seactionReader) throws IOException {
        BSONObject bson = new BasicBSONObject();
        BufferedReader bufferedReader = new BufferedReader(seactionReader);
        try {
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                int index = line.indexOf("=");
                String key = line.substring(0, index);
                String value = line.substring(index + 1);
                bson.put(key, value);
            }
        } finally {
            bufferedReader.close();
        }
        return bson;
    }


    public List<String> getSeactionNames() {
        return new ArrayList<>(tableSeactionBsonRecords.keySet());
    }

    public <T> List<T> getSeaction(String seactionName, ConfCoverter<T> converter) {
        List<BSONObject> bsons = tableSeactionBsonRecords.get(seactionName);
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

    public <T> T getKeyValueSeaction(String seactionName, ConfCoverter<T> converter) {
        BSONObject bson = keyValueSeactionBsonRecord.get(seactionName);
        if (bson == null){
            return null;
        }
        return converter.convert(bson);
    }
}
