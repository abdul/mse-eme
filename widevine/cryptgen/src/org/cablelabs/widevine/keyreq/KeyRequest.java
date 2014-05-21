// Confidential material under the terms of the Limited Distribution Non-disclosure
// Agreement between CableLabs and Comcast

package org.cablelabs.widevine.keyreq;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.cablelabs.widevine.Track;

public class KeyRequest {

    
    private static final String POLICY = "";
    private static final String CLIENT_ID = null;
    private static final String[] DRM_TYPES = { "WIDEVINE" };
    
    private static final String TEST_PROVIDER   = "widevine_test";
    private static final String TEST_SERVER_URL = "https://license.uat.widevine.com";
    
    private static final String SIGN_PROPS_URL      = "url";
    private static final String SIGN_PROPS_KEY      = "key";
    private static final String SIGN_PROPS_IV       = "iv";
    private static final String SIGN_PROPS_PROVIDER = "provider";
    
    private String content_id;
    private List<Track> tracks;
    
    private boolean sign_request = false;
    String license_url;
    byte[] sign_key;
    byte[] sign_iv;
    String provider;
    
    private int rollingKeyStart = -1;
    private int rollingKeyCount = -1;
    
    /**
     * Creates a new key request for the given list of tracks.  1 key per track
     * 
     * @param content_id the unique content ID
     * @param tracks the track list
     * @param sign_request true if the request should be signed, false otherwise
     * @throws IllegalArgumentException
     */
    public KeyRequest(String content_id, List<Track> tracks)
            throws IllegalArgumentException {
        
        // Validate arguments
        if (content_id == null || content_id.isEmpty())
            throw new IllegalArgumentException("Must provide a valide content ID: " + content_id);
        if (tracks == null || tracks.size() == 0)
            throw new IllegalArgumentException("Must provide a non-empty list of tracks: " +
                                               ((tracks == null) ? "null" : "empty list"));
            
        this.content_id = content_id;
        this.tracks = tracks;
    }
    
    /**
     * Creates a new key request for the given list of tracks.  Multiple (rolling) keys per
     * track
     * 
     * @param content_id the unique content ID
     * @param tracks the track list
     * @param sign_request true if the request should be signed, false otherwise
     * @param rollingKeyStart the start time of the first key
     * @param rollingKeyCount the number of keys
     * @throws IllegalArgumentException
     */
    public KeyRequest(String content_id, List<Track> tracks, int rollingKeyStart, int rollingKeyCount)
            throws IllegalArgumentException {
        this(content_id, tracks);
        if (rollingKeyCount == 0) 
            throw new IllegalArgumentException("Must provide a non-zero rolling key count: " + rollingKeyCount);
            
        this.rollingKeyStart = rollingKeyStart;
        this.rollingKeyCount = rollingKeyCount;
    }
    
    /**
     * Indicates that this request should be signed and that it should use the credentials
     * in the given properties file
     * <p>
     * The properties file contains the following properties:
     * <p>
     * <b>url</b> : The key server URL
     * <b>key</b> : The 32-byte signing key, hexadecimal notation
     * <b>url</b> : The 16-byte initialization vector, hexadecimal notation
     * <b>url</b> : The provider name
     * 
     * @param props_file the signing properties file
     * @throws IOException if there was an error reading from the properties file 
     * @throws FileNotFoundException if the properties file was not found
     */
    public void setSigningProperties(String props_file) throws FileNotFoundException, IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(props_file));
        
        String prop;
        
        // Key server URL
        if ((prop = props.getProperty(SIGN_PROPS_URL)) == null)
            throw new IllegalArgumentException("'url' property not found in request signing properties file");
        license_url = prop;
        
        // Signing key
        if ((prop = props.getProperty(SIGN_PROPS_KEY)) == null)
            throw new IllegalArgumentException("'key' property not found in request signing properties file");
        try {
            sign_key = Hex.decodeHex(prop.toCharArray());
            if (sign_key.length != 32)
                throw new IllegalArgumentException("Request signing key is not 32 bytes in length");
        }
        catch (DecoderException e) {
            throw new IllegalArgumentException("Request signing key could not be parsed");
        }
        
        // Signing initialization vector
        if ((prop = props.getProperty(SIGN_PROPS_IV)) == null)
            throw new IllegalArgumentException("'iv' property not found in request signing properties file");
        try {
            sign_iv = Hex.decodeHex(prop.toCharArray());
            if (sign_iv.length != 16)
                throw new IllegalArgumentException("Request initialization vector is not 16 bytes in length");
        }
        catch (DecoderException e) {
            throw new IllegalArgumentException("Request initialization vector could not be parsed");
        }
        
        // Provider name
        if ((prop = props.getProperty(SIGN_PROPS_PROVIDER)) == null)
            throw new IllegalArgumentException("'provider' property not found in request signing properties file");
        provider = prop;
        
        sign_request = true;
    }
    
    /**
     * Perform the key request.
     * 
     * @return the response message
     */
    public ResponseMessage requestKeys() {
        
        int i;
        
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        Gson prettyGson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        // Create request object
        RequestMessage requestMessage = new RequestMessage();
        requestMessage.content_id = Base64.encodeBase64String(content_id.getBytes());
        requestMessage.policy = POLICY;
        requestMessage.client_id = CLIENT_ID;
        requestMessage.drm_types = DRM_TYPES;
        
        // Add the track requests to the message
        requestMessage.tracks = new RequestMessage.Track[tracks.size()];
        i = 0;
        for (Track t : tracks) {
            RequestMessage.Track track = new RequestMessage.Track();
            track.type = t.type;
            requestMessage.tracks[i++] = track;
        }
        
        // Rolling keys
        if (rollingKeyCount != -1 && rollingKeyStart != -1) {
            requestMessage.crypto_period_count = rollingKeyCount;
            requestMessage.first_crypto_period_index = rollingKeyStart;
        }
        
        // Convert request message to JSON and base64 encode
        String jsonRequestMessage = gson.toJson(requestMessage);
        System.out.println("Request Message:");
        System.out.println(prettyGson.toJson(requestMessage));
        String jsonRequestMessageB64 = Base64.encodeBase64String(jsonRequestMessage.getBytes());
        
        // Create request JSON
        Request request = new Request();
        request.request = jsonRequestMessageB64;
        
        String serverURL = null;
        if (sign_request) {
            // Create message signature
            try {
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                sha1.update(jsonRequestMessage.getBytes());
                byte[] sha1_b = sha1.digest();
                System.out.println("SHA-1 hash of JSON request message = 0x" + Hex.encodeHexString(sha1_b));
                
                // Use AES/CBC/PKCS5Padding with CableLabs Key and InitVector
                SecretKeySpec keySpec = new SecretKeySpec(sign_key, "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(sign_iv);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                
                // Encrypt the SHA-1 hash of our request message
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
                byte[] encrypted = cipher.doFinal(sha1_b);
                System.out.println("AES/CBC/PKCS5Padding Encrypted SHA1-hash = 0x" + Hex.encodeHexString(encrypted));
                
                request.signer = provider;
                request.signature = Base64.encodeBase64String(encrypted);
                
                serverURL = license_url;
            }
            catch (Exception e) {
                System.out.println("Error performing message encryption!  Message = " + e.getMessage());
                System.exit(1);
            }
        } else {
            request.signer = TEST_PROVIDER;
            serverURL = TEST_SERVER_URL;
        }
        
        String jsonRequest = gson.toJson(request);
        System.out.println("Request:");
        System.out.println(prettyGson.toJson(request));
        
        String jsonResponseStr = null;
        try {
            
            // Create URL connection
            URL url = new URL(serverURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            
            System.out.println("Sending HTTP POST to " + serverURL);
            
            // Write POST data
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(jsonRequest);
            out.flush();
            out.close();
            
            // Wait for response
            int responseCode = con.getResponseCode();
            System.out.println("Received response code -- " + responseCode);
            
            // Read response data
            DataInputStream dis = new DataInputStream(con.getInputStream());
            int bytesRead;
            byte responseData[] = new byte[1024];
            StringBuffer sb = new StringBuffer();
            while ((bytesRead = dis.read(responseData)) != -1) {
                sb.append(new String(responseData,0,bytesRead));
            }
            jsonResponseStr = sb.toString();
        }
        catch (Exception e) {
            System.err.println("Error in HTTP communication! -- " + e.getMessage());
            System.exit(1);
        }
        
        Response response = gson.fromJson(jsonResponseStr, Response.class);
        System.out.println("Response:");
        System.out.println(prettyGson.toJson(response));
        
        String responseMessageStr = new String(Base64.decodeBase64(response.response));
        ResponseMessage responseMessage = gson.fromJson(responseMessageStr, ResponseMessage.class);
        System.out.println("ResponseMessage:");
        System.out.println(prettyGson.toJson(responseMessage));
        
        return responseMessage;
    }
}
