package com.sequoiacm.infrastructure.common;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class XMLUtils {
    private static final Logger logger = LoggerFactory.getLogger(XMLUtils.class);

    public static BSONObject xmlToBSONObj(InputStream xmlInputStram) throws Exception {
        String xmlString = parseXmlToString(xmlInputStram);
        JSONObject obj = XML.toJSONObject(xmlString);
        return (BSONObject) JSON.parse(obj.toString());
    }

    private static String parseXmlToString(InputStream xmlInputStream)
            throws TransformerException, ParserConfigurationException, IOException, SAXException {
        StringWriter writer = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlInputStream);
            DOMSource domSource = new DOMSource(doc);
            writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        }
        finally {
            IOUtils.close(writer);
        }
    }
}
