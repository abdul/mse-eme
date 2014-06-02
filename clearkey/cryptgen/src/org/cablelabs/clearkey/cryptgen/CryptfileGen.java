
package org.cablelabs.clearkey.cryptgen;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.cablelabs.clearkey.cryptfile.ClearKeyJsonPSSH;
import org.cablelabs.clearkey.cryptfile.ClearKeyPSSH;
import org.cablelabs.clearkey.cryptfile.ClearKeyRemotePSSH;
import org.cablelabs.cryptfile.CryptKey;
import org.cablelabs.cryptfile.CryptTrack;
import org.cablelabs.cryptfile.CryptfileBuilder;
import org.cablelabs.cryptfile.DRMInfoPSSH;
import org.cablelabs.cryptfile.KeyPair;

/**
 * This utility will build a MP4Box CableLabs ClearKey cryptfile for a given piece of content.
 */
public class CryptfileGen {

    private static void usage() {
        System.out.println("CableLabs ClearKey MP4Box cryptfile generation tool.");
        System.out.println("");
        System.out.println("usage:  CryptfileGen [OPTIONS] <track_id>:{@<key_file>|<key_id>=<key>[,<key_id>:<key>...]} [<track_id>:{@<key_file>|<key_id>:<key>[,<key_id>:<key>...]}]...");
        System.out.println("");
        System.out.println("\t<track_id> is the track ID from the MP4 file to be encrypted.");
        System.out.println("\tAfter the '<track_id>:', you can specify either a file containing key/keyID pairs");
        System.out.println("\tOR a comma-separated list of keyID/key pairs separated by '='.  Key IDs are always");
        System.out.println("\trepresented in GUID form (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx).  Key values are");
        System.out.println("\talways in hexadecimal.  Multiple key IDs indicate the use of rolling keys.");
        System.out.println("");
        System.out.println("\t\t<keyid_file> is a file that contains a list of key pairs, one pair per line in");
        System.out.println("\t\tthe form <key_id>:<key>");
        System.out.println("");
        System.out.println("\t\t<keyid> is a key ID in GUID form.");
        System.out.println("");
        System.out.println("\t\t<key> is a 16-byte key value in hexadecimal (with or without the leading '0x').");
        System.out.println("");
        System.out.println("\tOPTIONS:");
        System.out.println("");
        System.out.println("\t-out <filename>");
        System.out.println("\t\tIf present, the cryptfile will be written to the given file. Otherwise output will be");
        System.out.println("\t\twritten to stdout.");
        System.out.println("");
        System.out.println("\t-remote <license_url>");
        System.out.println("\t\tIf present, the ClearKey PSSH for the content will indicate that the player should");
        System.out.println("\t\tcontact the ClearKey server at the given URL for keys.  The default behavior is to");
        System.out.println("\t\tgenerate ClearKey PSSH with JSON Web Keys embedded directly");
        System.out.println("");
        System.out.println("\t-roll <sample_count>");
        System.out.println("\t\tUsed for rolling keys only.  <sample_count> is the number of consecutive samples to be");
        System.out.println("\t\tencrypted with each key before moving to the next.");
    }
    
    private static class Track {
        List<KeyPair> keypairs = new ArrayList<KeyPair>();
        int id;
    }
    
    private static void invalidOption(String option) {
        usage();
        errorExit("Invalid argument specification for " + option);
    }
    
    // Check for the presence of an option argument and validate that there are enough sub-options to
    // satisfy the option's requirements
    private static String[] checkOption(String optToCheck, String[] args, int current,
            int minSubopts, int maxSubopts) {
        if (!args[current].equals(optToCheck))
            return null;
        
        // No sub-options required
        if (minSubopts == 0 && maxSubopts == 0)
            return new String[0];
        
        // Validate that the sub-options are present
        if (args.length < current + 1)
            invalidOption(optToCheck);
        
        // Check that the sub-options present satifsy the min/max requirements
        String[] subopts = args[current+1].split(",");
        if (subopts.length < minSubopts || subopts.length > maxSubopts)
            invalidOption(optToCheck);
        
        return subopts;
    }
    private static String[] checkOption(String optToCheck, String[] args, int current, int subopts) {
        return checkOption(optToCheck, args, current, subopts, subopts);
    }
    
    private static void errorExit(String errorString) {
        usage();
        System.err.println(errorString);
        System.exit(1);;
    }
    
    public static void main(String[] args) {

        // Rolling keys
        int rollingKeySamples = -1;
        
        String outfile = null;
        URL url = null;
        List<Track> tracks = new ArrayList<Track>();
        
        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            
            // Parse options
            if (args[i].startsWith("-")) {
                String[] subopts;
                if ((subopts = checkOption("-out", args, i, 1)) != null) {
                    outfile = subopts[0];
                    i++;
                }
                else if ((subopts = checkOption("-roll", args, i, 1)) != null) {
                    rollingKeySamples = Integer.parseInt(subopts[0]);
                    i++;
                }
                else if ((subopts = checkOption("-remote", args, i, 1)) != null) {
                    try {
                        url = new URL(subopts[0]);
                    }
                    catch (MalformedURLException e) {
                        errorExit("Illegal URL: " + e.getMessage());
                    }
                    i++;
                }
                else {
                    errorExit("Illegal argument: " + args[i]);
                }
                
                continue;
            }
            
            // Parse tracks
            String track_desc[] = args[i].split(":");
            if (track_desc.length != 2) {
                usage();
                System.exit(1);;
            }
            try {
                Track t = new Track();
                t.id = Integer.parseInt(track_desc[0]);
                
                // Read key pairs from file
                if (track_desc[1].startsWith("@")) {
                    String keyfile = track_desc[1].substring(1);
                    BufferedReader br = new BufferedReader(new FileReader(keyfile));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] keypair = line.split(":");
                        if (keypair.length != 2) {
                            errorExit("Illegal keypair from file: " + line);
                        }
                        t.keypairs.add(new KeyPair(keypair[0], keypair[1]));
                    }
                    br.close();
                }
                else { // Key pairs on command line
                    String[] keypairsarg = track_desc[1].split(",");
                    for (String keypairs : keypairsarg) {
                        String[] keypair = keypairs.split("=");
                        if (keypair.length != 2) {
                            errorExit("Illegal keypair : " + keypairs);
                        }
                        t.keypairs.add(new KeyPair(keypair[0], keypair[1]));
                    }
                }
                
                tracks.add(t);
            }
            catch (IllegalArgumentException e) {
                errorExit("Illegal track specification -- " + track_desc[1]);
            }
            catch (FileNotFoundException e) {
                errorExit("Key ID file not found: " + e.getMessage());
            }
            catch (IOException e) {
                errorExit("Error reading from Key ID file: " + e.getMessage());
            }
        }
        
        List<CryptTrack> cryptTracks = new ArrayList<CryptTrack>();
        List<KeyPair> keypairs = new ArrayList<KeyPair>(); // Need this for URL-based PSSH
        for (Track t : tracks) {
            List<CryptKey> cryptKeys = new ArrayList<CryptKey>();
            for (KeyPair key : t.keypairs) {
                cryptKeys.add(new CryptKey(key));
            }
            keypairs.addAll(t.keypairs);
            
            cryptTracks.add(new CryptTrack(t.id, 8, null, cryptKeys, rollingKeySamples));
        }
        
        ClearKeyPSSH pssh = null;
        
        // Remote
        if (url != null) {
            List<String> keyIDs = new ArrayList<String>();
            System.out.println("Ensure the following keys are installed on the ClearKey server:");
            for (KeyPair keypair : keypairs) {
                System.out.println("\t" + Hex.encodeHexString(keypair.getID()) +
                                   " : " + Hex.encodeHexString(keypair.getKey()));
                keyIDs.add(Hex.encodeHexString(keypair.getID()));
            }
            System.out.println("");
            pssh = new ClearKeyRemotePSSH(url, keyIDs);
        }
        else { // JSON
            pssh = new ClearKeyJsonPSSH(keypairs);
        }
        
        List<DRMInfoPSSH> psshList = new ArrayList<DRMInfoPSSH>();
        psshList.add(pssh);
        
        // Create the cryptfile builder
        CryptfileBuilder cfBuilder = new CryptfileBuilder(CryptfileBuilder.ProtectionScheme.AES_CTR,
                                                          cryptTracks, psshList);
        
        // Write the output
        cfBuilder.writeCryptfile(System.out);
        try {
            if (outfile != null) {
                System.out.println("Writing cryptfile to: " + outfile);
                cfBuilder.writeCryptfile(new FileOutputStream(outfile));
            }
        }
        catch (FileNotFoundException e) {
            errorExit("Could not open output file (" + outfile + ") for writing");
        }
    }

}
