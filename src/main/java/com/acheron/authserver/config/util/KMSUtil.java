package com.acheron.authserver.config.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;

import java.util.Base64;

@RequiredArgsConstructor
@Component
public class KMSUtil {
    private final KmsClient kmsClient;
    @Value("${aws.arn}")
    private String arn;

    public String kmsEncrypt(String plainText) {
        EncryptRequest encryptRequest = buildEncryptRequest(plainText);
        EncryptResponse encryptResponse = kmsClient.encrypt(encryptRequest);
        SdkBytes cipherTextBytes = encryptResponse.ciphertextBlob();
        byte[] base64EncodedValue = Base64.getEncoder().encode(cipherTextBytes.asByteArray());
        return new String(base64EncodedValue);
    }

    public String kmsDecrypt(String base64EncodedValue) {
        DecryptRequest decryptRequest = buildDecryptRequest(base64EncodedValue);
        DecryptResponse decryptResponse = kmsClient.decrypt(decryptRequest);
        return decryptResponse.plaintext().asUtf8String();
    }

    private EncryptRequest buildEncryptRequest(String plainText) {
        SdkBytes plainTextBytes = SdkBytes.fromUtf8String(plainText);
        return EncryptRequest.builder().keyId(arn)
                .plaintext(plainTextBytes).build();
    }

    private DecryptRequest buildDecryptRequest(String base64EncodedValue) {
        SdkBytes encryptBytes = SdkBytes.fromByteArray(Base64.getDecoder().decode(base64EncodedValue));
        return DecryptRequest.builder().keyId(arn)
                .ciphertextBlob(encryptBytes).build();
    }
}