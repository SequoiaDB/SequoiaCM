package com.sequoiacm.deploy.parser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeactionParser {
    private static final Logger logger = LoggerFactory.getLogger(SeactionParser.class);
    private File confFile;
    private final static int INIT_SEACTION_BUFFER_SIZE = 1024;
    private HashMap<String, List<String>> seactionMap;

    private String lineSeparator;

    @SuppressWarnings("restriction")
    public SeactionParser(File confFile) throws IOException {
        this.confFile = confFile;
        this.seactionMap = new HashMap<>();
        this.lineSeparator = java.security.AccessController
                .doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));
        BufferedReader bfReader = new BufferedReader(new FileReader(confFile));
        try {
            parse(bfReader);
        }
        finally {
            bfReader.close();
        }
    }

    private void parse(BufferedReader bfReader) throws IOException {
        String currentSeaction = null;
        while (true) {
            String line = bfReader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }

            if (Pattern.matches("^\\[[A-Za-z0-9]+\\]$", line)) {
                logger.debug("parse seaction:{}", line);
                currentSeaction = line.substring(1, line.length() - 1);
                if (seactionMap.containsKey(currentSeaction)) {
                    throw new IOException("duplicate seaction:" + line);
                }
                seactionMap.put(currentSeaction, new ArrayList<String>());
                continue;
            }

            if (currentSeaction == null) {
                throw new IOException("failed to parse conf file, missing seaction line:" + line
                        + ", confFile=" + confFile);
            }
            List<String> secLines = seactionMap.get(currentSeaction);
            secLines.add(line);
        }
    }

    public Reader getSeaction(String seactionName) throws IOException {
        ByteArrayOutputStream byteOs = new ByteArrayOutputStream(INIT_SEACTION_BUFFER_SIZE);
        try {
            List<String> lines = seactionMap.get(seactionName);
            if (lines == null) {
                throw new IllegalArgumentException("unknown seaction name:" + seactionName
                        + ", all seaction:" + getSeactions());
            }
            for (String line : lines) {
                byteOs.write(line.getBytes("utf-8"));
                byteOs.write(lineSeparator.getBytes("utf-8"));
            }
            byteOs.flush();
            return new InputStreamReader(new ByteArrayInputStream(byteOs.toByteArray()), "utf-8");
        }
        finally {
            byteOs.close();
        }
    }

    public List<String> getSeactions() {
        return new ArrayList<>(seactionMap.keySet());
    }

}
