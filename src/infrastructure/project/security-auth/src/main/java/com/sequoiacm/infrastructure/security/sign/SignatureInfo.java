package com.sequoiacm.infrastructure.security.sign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.security.auth.RestField;

public class SignatureInfo {
    private String secretKeyPrefix = "";
    private String algorithm = "HmacSHA256";
    private String accessKey;
    private String signatureEncoder = "base64";
    private String signature = "";
    private List<String> stringToSign = Collections.emptyList();

    public SignatureInfo(BSONObject bson) {
        this.algorithm = BsonUtils.getStringOrElse(bson, RestField.SIGNATURE_INFO_ALGORITHM,
                algorithm);
        this.accessKey = BsonUtils.getStringChecked(bson, RestField.SIGNATURE_INFO_ACCESSKEY);
        this.signature = BsonUtils.getStringOrElse(bson, RestField.SIGNATURE_INFO_SINAGTURE,
                signature);
        this.signatureEncoder = BsonUtils.getStringOrElse(bson,
                RestField.SIGNATURE_INFO_SINAGTURE_ENCODER, signatureEncoder);
        this.secretKeyPrefix = BsonUtils.getStringOrElse(bson,
                RestField.SIGNATURE_INFO_SECREKEY_PREFIX, secretKeyPrefix);
        BasicBSONList bsonArr = BsonUtils.getArray(bson, RestField.SIGNATURE_INFO_STRING_TO_SIGN);
        if (bsonArr != null) {
            stringToSign = new ArrayList<>();
            for (Object s : bsonArr) {
                stringToSign.add((String) s);
            }
        }
    }

    public SignatureInfo(String algorithm, String accessKey, String secretKeyPrefix, String signature,
            String signatureEncoder, List<String> stringToSign) {
        this.algorithm = algorithm;
        this.accessKey = accessKey;
        this.signature = signature;
        this.stringToSign = stringToSign;
        this.secretKeyPrefix = secretKeyPrefix;
        this.signatureEncoder = signatureEncoder;
    }

    public String getSignatureEncoder() {
        return signatureEncoder;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getSignature() {
        return signature;
    }

    public List<String> getStringToSign() {
        return stringToSign;
    }

    public String getSecretKeyPrefix() {
        return secretKeyPrefix;
    }

    @Override
    public String toString() {
        return "SignatureInfo [secretKeyPrefix=" + secretKeyPrefix + ", algothm=" + algorithm
                + ", accessKey=" + accessKey + ", signature=" + signature + ", stringToSign="
                + stringToSign + "]";
    }

    public BSONObject toBSON() {
        BasicBSONObject bson = new BasicBSONObject();
        bson.put(RestField.SIGNATURE_INFO_ALGORITHM, algorithm);
        bson.put(RestField.SIGNATURE_INFO_ACCESSKEY, accessKey);
        bson.put(RestField.SIGNATURE_INFO_SECREKEY_PREFIX, secretKeyPrefix);
        bson.put(RestField.SIGNATURE_INFO_SINAGTURE, signature);
        bson.put(RestField.SIGNATURE_INFO_STRING_TO_SIGN, stringToSign);
        bson.put(RestField.SIGNATURE_INFO_SINAGTURE_ENCODER, signatureEncoder);
        return bson;
    }

}
