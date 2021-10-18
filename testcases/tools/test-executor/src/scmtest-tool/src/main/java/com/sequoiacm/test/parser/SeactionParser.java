package com.sequoiacm.test.parser;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class SeactionParser {

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

            if (Pattern.matches("^\\[[A-Za-z0-9-]+\\]$", line)) {
                currentSeaction = line.substring(1, line.length() - 1);
                if (seactionMap.containsKey(currentSeaction)) {
                    throw new IOException("Duplicate seaction:" + line);
                }
                seactionMap.put(currentSeaction, new ArrayList<String>());
                continue;
            }

            if (currentSeaction == null) {
                throw new IOException("Failed to parse conf file, missing seaction line:" + line
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
                throw new IllegalArgumentException("Unknown seaction name:" + seactionName
                        + ", all seaction:" + getSeactionList());
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

    public List<String> getSeactionList() {
        return new ArrayList<>(seactionMap.keySet());
    }

}
