package com.beam.exception;

/**
 * 암호화 관련 예외
 */
public class EncryptionException extends ApplicationException {

    public EncryptionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EncryptionException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public EncryptionException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    public static EncryptionException keyGenerationFailed(Throwable cause) {
        return new EncryptionException(ErrorCode.CRYPTO_KEY_GENERATION_FAILED, null, cause);
    }

    public static EncryptionException keyGenerationFailed(String detail, Throwable cause) {
        return new EncryptionException(ErrorCode.CRYPTO_KEY_GENERATION_FAILED, detail, cause);
    }

    public static EncryptionException encryptionFailed(Throwable cause) {
        return new EncryptionException(ErrorCode.CRYPTO_ENCRYPTION_FAILED, null, cause);
    }

    public static EncryptionException encryptionFailed(String detail, Throwable cause) {
        return new EncryptionException(ErrorCode.CRYPTO_ENCRYPTION_FAILED, detail, cause);
    }

    public static EncryptionException decryptionFailed(Throwable cause) {
        return new EncryptionException(ErrorCode.CRYPTO_DECRYPTION_FAILED, null, cause);
    }

    public static EncryptionException decryptionFailed(String detail, Throwable cause) {
        return new EncryptionException(ErrorCode.CRYPTO_DECRYPTION_FAILED, detail, cause);
    }

    public static EncryptionException keyDerivationFailed(String keyType, Throwable cause) {
        return new EncryptionException(ErrorCode.CRYPTO_KEY_DERIVATION_FAILED, keyType, cause);
    }
}
