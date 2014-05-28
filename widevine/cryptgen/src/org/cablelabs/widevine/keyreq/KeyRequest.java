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
    private String license_url;
    private byte[] sign_key;
    private byte[] sign_iv;
    private String provider;
    
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
        sign_key = Base64.decodeBase64(prop);
        if (sign_key.length != 32)
            throw new IllegalArgumentException("Request signing key is not 32 bytes in length");
        
        // Signing initialization vector
        if ((prop = props.getProperty(SIGN_PROPS_IV)) == null)
            throw new IllegalArgumentException("'iv' property not found in request signing properties file");
        sign_iv = Base64.decodeBase64(prop);
        if (sign_iv.length != 16)
            throw new IllegalArgumentException("Request initialization vector is not 16 bytes in length");
        
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
        
        String test = "{\"payload\":\"CAES77+9Cwrvv70KCAES77+9CQrvv70CCAISE0Nocm9tZUNETS1MaW51eC14ODYY0Kjvv73vv70FIu+/vQIw77+9AQoC77+9AQEA77+977+977+9Ahbvv70CZUrvv70e77+977+977+9Iu+/vSMyLRB0cHUBYG0SOu+/ve+/vXIq77+9dnk5e++/ve+/ve+/vVxp77+9e3FL77+9TFx877+9De+/vQl0PRZlR++/ve+/ve+/vRkH77+9QhYkVu+/ve+/vcuX77+9TO+/vWxW77+977+977+977+977+977+9cgvvv73vv73vv70w77+9Pe+/vQM5YO+/ve+/vSBBLO+/vRvvv70PNHE7ee+/vSvvv70yG++/ve+/vShc77+9b3d7Ni1L77+9Be+/vTzvv71sE1Yy77+977+9D++/vX1jZG/Oie+/ve+/ve+/ve+/ve+/ve+/vTJ3Fe+/ve+/vX5A77+977+977+977+9b++/ve+/ve+/vQfvv70n77+977+9OO+/ve+/ve+/vSANG3/vv73vv70e77+977+977+977+9FTXvv73vv73LlCLvv73vv70e77+9ZQhNEV9PJ++/vV1N77+9Su+/ve+/ve+/ve+/ve+/vcOlAe+/ve+/ve+/vRPVpO+/vQom77+9Y++/ve+/ve+/ve+/vU0seO+/ve+/ve+/vS4M77+9KTEL77+977+977+9eO+/vVnvv73vv73vv73vv71jQe+/vQIDAQABKO+/vSAS77+9Au+/vQMqcO+/vcOmCu+/ve+/ve+/ve+/vXDvv70R77+977+9U3Lvv71r77+9I++/vXUd77+9IO+/vV5q77+9Ou+/ve+/vSxjOXUW77+9Gnzvv70Y77+9YO+/vS9vE++/vRJpZAHvv71P77+977+977+977+9EO+/vRE177+9bu+/vWh277+977+977+9Ee+/vVYp77+9eO+/vWJZYyDvv71eNhVG77+9De+/vTBzA++/ve+/ve+/ve+/ve+/vU7vv70u77+977+9Q++/ve+/vS3ZqF7vv70+77+977+977+977+977+967mO77+9Du+/vS5W77+9Ge+/ve+/ve+/ve+/ve+/ve+/vUXvv73vv71w1KJe77+9XO+/vUFOyL1877+9HjHvv73vv73vv73vv73vv71V77+9KUkPA++/vVfvv714J++/vTxw7Kaf77+977+977+9Kj7vv70E77+9CStrHe+/vUQsQu+/ve+/ve+/vUnvv73vv70x77+9L++/vUHvv73vv71BOwN177+9LHkDBjRh77+977+9N++/vQXvv73Xje+/vSs/77+9X++/ve+/vQAPa++/ve+/vWB077+9TO+/vVZCA0Tvv71077+9eyR1Q++/vUXvv70oGu+/vQUK77+9AggBEhDvv71477+9JO+/vULvv73vv70sRGTvv70Z77+977+9Ghjvv73vv73vv73vv70FIu+/vQIw77+9AQoC77+9AQEA77+9Ne+/vXIadu+/vTAkcE9meOOjte+/vVLvv73Dge+/vRdfJO+/vXs6Y++/vVbvv70P77+9fgjvv73vv73vv73vv70277+977+977+977+9Mu+/ve+/vRRv77+977+9Te+/ve+/ve+/vRYzaV/klYYLFg1Q77+977+9Cu+/ve+/vU/vv70o77+9QEQYZGfvv71t77+977+9Okrvv73vv71D77+977+977+9WkHvv70X77+9SAVqZDDvv71I77+9bArvv70YdO+/ve+/vXHvv73vv73vv71T77+9G++/vXQ477+977+9VGZ877+977+9BhVD77+977+9Fe+/ve+/ve+/vTYsJ++/ve+/vT9B77+9fu+/vQDvv73vv70RYkPvv71J77+9Hu+/ve+/ve+/vUjIj3bvv71Y77+9ARfvv70h77+9agZFAe+/ve+/vW/vv71M77+977+9WwMJZDDvv70CA8iwPSHvv71k77+9Te+/vXsQ77+9fyrvv703bUHvv70u77+9LO+/vRcJL++/ve+/vUpt77+9Re+/vSzvv71DB++/vW7vv73vv71W77+9PlXvv73vv73vv70177+977+9C++/vXvvv73vv708bO+/vQdSMe+/vQIDAQABKO+/vSAS77+9A++/ve+/ve+/ve+/ve+/ve+/vVXvv71g77+977+9Du+/ve+/vWkJE++/ve+/ve+/vQTvv70qxqbvv70EAO+/vXrvv71pFO+/vVBOFu+/ve+/vQld77+9Vxbvv70t77+977+977+9Eu+/ve+/ve+/vUzvv73vv73vv73vv71+Me+/ve+/ve+/ve+/ve+/ve+/ve+/ve+/ve+/vTlb77+9MXPvv73vv70/OhXvv73vv73vv71Qa++/ve+/vXzvv73vv70L77+9N37vv73vv71l77+977+9VWvvv73vv71a77+9F++/vcyp77+9Fe+/ve+/vTDvv71TYe+/ve+/ve+/ve+/ve+/ve+/vRfvv71x77+977+9Re+/vVjvv71Bazrvv73Vp++/vXM077+9BxTvv70BDWxh77+977+9J9qP77+92oLvv73vv71cAAHvv73vv73vv71qYu+/vT43AO+/vQsE77+9Gu+/ve+/vXXvv73vv73vv73vv71NYQHvv71xG++/vQLvv71sDRTvv73vv73vv70TVe+/vQzvv71YYkvvv70pXhIh77+9DO+/vd6b77+9RlTvv73vv73vv71p77+9SzJ6ZO+/vX5v77+9bn9Vxpfvv73vv73vv71z77+9CO+/ve+/vQw4BO+/vS1LC++/vUbUme+/vXRZ77+9LAN877+9fj7vv73vv71kEBrvv73vv71s77+9z7bvv70h77+9Ju+/vUzvv73vv73vv70H77+90rzvv71+I2Tvv71pUdyK77+977+9et6Ufe+/vS7vv73vv70m77+977+9Me+/vTHvv73vv73vv73vv73vv73vv71U77+9Lu+/vTPvv73QuO+/vVFb77+9NO+/vQdqX3nvv70eNhkK77+9U++/ve+/ve+/ve+/ve+/ve+/ve+/ve+/ve+/vTXvv70177+9e++/vW84NxlA77+9Lu+/vVTvv73vv71AQCTvv73vv70M77+9Ewzvv71HPjkaGwoRYXJjaGl0ZWN0dXJlX25hbWUSBng4Ni02NBoWCgxjb21wYW55X25hbWUSBkdvb2dsZRoXCgptb2RlbF9uYW1lEglDaHJvbWVDRE0aFgoNcGxhdGZvcm1fbmFtZRIFTGludXgyCAgBEAEYASAAEkYKRAouCAESEO+/ve+/ve+/vQlr77+9We+/ve+/vVPvv71r77+9fe+/vTcaCWNhYmxlbGFicyIHYWJjZGVmZyoCSEQyABABGhDvv70J77+9dXDvv73vv73vv71K1aTvv73vv71tClgYASDvv73fk++/vQUwFRrvv70C77+977+9b++/ve+/ve+/ve+/ve+/ve+/ve+/vU4H77+977+977+977+9Uu+/ve+/vU3vv73vv711W++/vQ/vv71UEO+/ve+/ve+/vUcw77+977+977+9GO+/vdWk77+977+977+9Q2Xvv73vv73vv70CXnl177+977+9ZjfTre+/ve+/vXbvv71X77+9JHfquKjvv70aFgbvv73vv70F77+9KhpUQe+/ve+/vVpWDnVtNe+/ve+/ve+/ve+/vRpD77+9IS3vv71m77+977+977+9Le+/vT8H77+9y5Pvv73vv71+77+977+977+9LCHvv73vv70R77+977+977+9SlocfO+/vSFjYu+/ve+/vWrvv71XTG/vv70ha++/ve+/vS8/PF7vv704Ee+/ve+/vc2BKhTvv73vv709SO+/vRoF77+977+977+977+9Ve+/vceT3Jbvv70USF9YE++/vWXvv71KY1Lvv73vv70teO+/ve+/ve+/ve+/ve+/ve+/ve+/ve+/vU3vv71677+977+977+977+9ei/vv70dMFwW3aBa77+9bu+/vThm77+9Ze+/vVRxdu+/ve+/ve+/vTxATO+/vR/vv71jVR4t77+9Je+/vVfYru+/vR1A77+9JxDvv73vv70J77+9\",\"provider\":\"cablelabs\",\"allowed_track_types\":\"SD_HD\"}";
        jsonRequestMessage = test; 
        jsonRequestMessageB64 = Base64.encodeBase64String(jsonRequestMessage.getBytes());
        
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
