
package org.cablelabs.clearkey;

/**
 * Represents a JSON Web Key as required by W3C Encrypted Media Extensions
 */
public class ClearKeyJWK {
    
    public static class Key {
        public String kty = "oct";
        public String alg = "A128GCM";
        public String kid;
        public String k;
    }
    
    public Key[] keys;
}
