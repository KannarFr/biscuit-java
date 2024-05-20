package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema;
import biscuit.format.schema.Schema.PublicKey.Algorithm;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.builder.Utils;
import com.google.protobuf.ByteString;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;


public class PublicKey {

    public final java.security.PublicKey key;
    public final Algorithm algorithm;

    private static final List<Algorithm> SUPPORTED_ALGORITHMS = List.of(Algorithm.Ed25519, Algorithm.SECP256R1);

    public PublicKey(Algorithm algorithm, java.security.PublicKey public_key) {
        this.key = public_key;
        this.algorithm = algorithm;
    }

    public PublicKey(Algorithm algorithm, byte[] data) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (algorithm == Algorithm.Ed25519) {
            this.key = Ed25519KeyPair.generatePublicKey(data);
        } else if (algorithm == Algorithm.SECP256R1) {
            this.key = SECP256R1KeyPair.generatePublicKey(data);
        } else {
            throw new IllegalArgumentException("Invalid algorithm");
        }
        this.algorithm = algorithm;
    }

    public byte[] toBytes() {
        if (key instanceof EdDSAPublicKey) {
            return ((EdDSAPublicKey) key).getAbyte();
        }
        return key.getEncoded();
    }

    public String toHex() {
        return Utils.byteArrayToHexString(this.toBytes());
    }

    public PublicKey(Algorithm algorithm, String hex) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] data = Utils.hexStringToByteArray(hex);
        if (algorithm == Algorithm.Ed25519) {
            this.key = Ed25519KeyPair.generatePublicKey(data);
        } else if (algorithm == Algorithm.SECP256R1) {
            this.key = SECP256R1KeyPair.generatePublicKey(data);
        } else {
            throw new IllegalArgumentException("Invalid algorithm");
        }
        this.algorithm = algorithm;
    }

    public Schema.PublicKey serialize() {
        Schema.PublicKey.Builder publicKey = Schema.PublicKey.newBuilder();
        publicKey.setKey(ByteString.copyFrom(this.toBytes()));
        publicKey.setAlgorithm(this.algorithm);
        return publicKey.build();
    }

    static public PublicKey deserialize(Schema.PublicKey pk) throws Error.FormatError.DeserializationError {
        if(!pk.hasAlgorithm() || !pk.hasKey()) {
            throw new Error.FormatError.DeserializationError("Invalid public key");
        }
        if (!SUPPORTED_ALGORITHMS.contains(pk.getAlgorithm())) {
            throw new Error.FormatError.DeserializationError("Invalid public key algorithm");
        }
        try {
            return new PublicKey(pk.getAlgorithm(), pk.getKey().toByteArray());
        } catch (NoSuchAlgorithmException e) {
            throw new Error.FormatError.DeserializationError("No such algorithm");
        } catch (InvalidKeySpecException e) {
            throw new Error.FormatError.DeserializationError("Invalid key spec");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PublicKey publicKey = (PublicKey) o;

        return key.equals(publicKey.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        if (algorithm == Algorithm.Ed25519) {
            return "ed25519/" + toHex().toLowerCase();
        } else if (algorithm == Algorithm.SECP256R1) {
            return "secp256r1/" + toHex().toLowerCase();
        } else {
            return null;
        }
    }
}
