package com.sequoiacm.test.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class ProjectInfoConfParser {

    private static final Logger logger = LoggerFactory.getLogger(ProjectInfoConfParser.class);
    private File confFile;
    private Map<String, List<BSONObject>> seactionBsonRecords;

    public ProjectInfoConfParser(String filePath) throws IOException {
        confFile = new File(filePath);
        SeactionParser seactionParser = new SeactionParser(confFile);
        parse(seactionParser);
    }

    private void parse(SeactionParser seactionParser) throws IOException {
        seactionBsonRecords = new HashMap<>();
        List<String> seactionList = seactionParser.getSeactionList();
        for (String seactionName : seactionList) {
            Reader seactionReader = seactionParser.getSeaction(seactionName);
            try {
                List<BSONObject> records = parseSeaction(seactionReader);
                seactionBsonRecords.put(seactionName, records);
            }
            finally {
                seactionReader.close();
            }
        }

        if (logger.isDebugEnabled()) {
            for (Map.Entry<String, List<BSONObject>> entry : seactionBsonRecords.entrySet()) {
                logger.debug("seaction parse result:{}", entry.getKey());
                for (BSONObject r : entry.getValue()) {
                    logger.debug(r.toString());
                }
            }
        }
    }

    private List<BSONObject> parseSeaction(Reader seactionReader) throws IOException {
        List<BSONObject> ret = new ArrayList<>();
        CSVParser csvParser = new CSVParser(seactionReader,
                CSVFormat.EXCEL.withHeader().withDelimiter(',').withEscape('\\').withQuote('\'')
                        .withTrim().withIgnoreSurroundingSpaces());
        try {
            for (CSVRecord record : csvParser) {
                Map<String, String> map = record.toMap();
                Map<String, String> formaterMap = new HashMap<>();

                for (Map.Entry<String, String> entry : map.entrySet()) {
                    formaterMap.put(entry.getKey().trim(), entry.getValue().trim());
                }
                ret.add(new BasicBSONObject(formaterMap));
            }
            return ret;
        }
        finally {
            csvParser.close();
        }
    }

    public <T> List<T> getSeaction(String seactionName, ConfConverter<T> converter) {
        List<BSONObject> bsonList = seactionBsonRecords.get(seactionName);
        if (bsonList == null) {
            return null;
        }
        List<T> ret = new ArrayList<>();
        for (BSONObject bson : bsonList) {
            try {
                ret.add(converter.convert(bson));
            }
            catch (Exception e) {
                throw new IllegalArgumentException("failed to parse conf:seaction=" + seactionName
                        + ", record=" + bson + ", cause by:" + e.getMessage(), e);
            }
        }
        return ret;
    }

    public <T> List<T> getSeactionWithCheck(String seactionName, ConfConverter<T> converter) {
        List<T> ret = getSeaction(seactionName, converter);
        if (ret == null) {
            throw new IllegalArgumentException("no such seaction:" + seactionName);
        }
        return ret;
    }

}
