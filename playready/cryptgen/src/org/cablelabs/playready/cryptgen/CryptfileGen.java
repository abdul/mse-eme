
package org.cablelabs.playready.cryptgen;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cablelabs.cryptfile.CryptKey;
import org.cablelabs.cryptfile.CryptTrack;
import org.cablelabs.cryptfile.CryptfileBuilder;
import org.cablelabs.cryptfile.DRMInfoPSSH;
import org.cablelabs.playready.PlayReadyKeyPair;
import org.cablelabs.playready.WRMHeader;
import org.cablelabs.playready.cryptfile.PlayReadyPSSH;

public class CryptfileGen {
    
    private static void usage() {
        System.out.println("usage:  CryptfileGen [OPTIONS] <track_id>:{@<keyid_file>|<key_id>[,<key_id>...]} [<track_id>:{@<keyid_file>|<key_id>[,<key_id>...]}]...");
        System.out.println("");
        System.out.println("\t<track_id> is the track ID from the MP4 file to be encrypted.");
        System.out.println("\tAfter the '<track_id>:', you can specify either a file containing key IDs OR a");
        System.out.println("\tcomma-separated list of key IDs.  Key IDs are always represented in GUID form");
        System.out.println("\t(xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx). Multiple key IDs indicate the use of");
        System.out.println("\trolling keys.");
        System.out.println("");
        System.out.println("\t\t<keyid_file> is a file that contains a list of key IDs, one key ID per line.");
        System.out.println("");
        System.out.println("\t\t<keyid> is a key ID in GUID form.");
        System.out.println("");
        System.out.println("\tOPTIONS:");
        System.out.println("");
        System.out.println("\t-out <filename>");
        System.out.println("\t\tIf present, the cryptfile will be written to the given file. Otherwise output will be");
        System.out.println("\t\twritten to stdout.");
        System.out.println("");
        System.out.println("\t-version {4000|4100}");
        System.out.println("\t\tIf present, specifies the WRMHeader version to generate.  Must be either '4000' for v4.0.0.0");
        System.out.println("\t\tor '4100' for v4.1.0.0.  Default is '4000'.");
        System.out.println("");
        System.out.println("\t-url <license_url>");
        System.out.println("\t\tIf present, specifies the license URL to embed in the WRMHeaders.  If not specified, will");
        System.out.println("\t\tuse the default url of:");
        System.out.println("\t\t'http://playready.directtaps.net/pr/svc/rightsmanager.asmx?PlayRight=1&UseSimpleNonPersistentLicense=1'");
        System.out.println("");
        System.out.println("\t-roll <sample_count>");
        System.out.println("\t\tUsed for rolling keys only.  <sample_count> is the number of consecutive samples to be");
        System.out.println("\t\tencrypted with each key before moving to the next.");
    }
    
    private static class Track {
        List<String> keyIDs = new ArrayList<String>();
        int id;
    }
    
    private static void invalidOption(String option) {
        usage();
        System.err.println("Invalid argument specification for " + option);
        System.exit(1);;
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
    
    public static void main(String[] args) {

        // Rolling keys
        int rollingKeySamples = -1;
        
        String outfile = null;
        String url = "http://playready.directtaps.net/pr/svc/rightsmanager.asmx?PlayRight=1&UseSimpleNonPersistentLicense=1";
        List<Track> tracks = new ArrayList<Track>();
        WRMHeader.Version headerVersion = WRMHeader.Version.V_4000;
        
        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            
            // Parse options
            if (args[i].startsWith("-")) {
                String[] subopts;
                if ((subopts = checkOption("-out", args, i, 1)) != null) {
                    outfile = subopts[0];
                    i++;
                }
                else if ((subopts = checkOption("-version", args, i, 1)) != null) {
                    if ("4000".equals(subopts[0])) {
                        headerVersion = WRMHeader.Version.V_4000;
                    }
                    else if ("4100".equals(subopts[0])) {
                        headerVersion = WRMHeader.Version.V_4000;
                    }
                    else {
                        usage();
                        System.err.println("Illegal WRMHeader version: " + subopts[0]);
                        System.exit(1);
                    }
                    i++;
                }
                else if ((subopts = checkOption("-roll", args, i, 1)) != null) {
                    rollingKeySamples = Integer.parseInt(subopts[0]);
                    i++;
                }
                else if ((subopts = checkOption("-url", args, i, 1)) != null) {
                    url = subopts[0];
                    i++;
                }
                else {
                    usage();
                    System.err.println("Illegal argument: " + args[i]);
                    System.exit(1);;
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
                
                // Read key IDs from file
                if (track_desc[1].startsWith("@")) {
                    String keyfile = track_desc[1].substring(1);
                    BufferedReader br = new BufferedReader(new FileReader(keyfile));
                    String line;
                    while ((line = br.readLine()) != null) {
                        t.keyIDs.add(line.trim());
                    }
                    br.close();
                }
                else { // Key IDs on command line
                    String[] keyIDs = track_desc[1].split(",");
                    for (String keyID : keyIDs) {
                        t.keyIDs.add(keyID);
                    }
                }
                
                tracks.add(t);
            }
            catch (IllegalArgumentException e) {
                usage();
                System.err.println("Illegal track_type -- " + track_desc[1]);
                System.exit(1);;
            }
            catch (FileNotFoundException e) {
                usage();
                System.err.println("Key ID file not found: " + e.getMessage());
                System.exit(1);;
            }
            catch (IOException e) {
                usage();
                System.err.println("Error reading from Key ID file: " + e.getMessage());
                System.exit(1);;
            }
        }
        
        List<WRMHeader> wrmHeaders = new ArrayList<WRMHeader>();
        List<CryptTrack> cryptTracks = new ArrayList<CryptTrack>();
        
        // Build one CryptTrack for every track and gather a list of all
        // WRMHeaders to put in one PSSH
        for (Track t : tracks) {
            List<CryptKey> cryptKeys = new ArrayList<CryptKey>();
            for (String keyID : t.keyIDs) {
                PlayReadyKeyPair prKey = new PlayReadyKeyPair(keyID);
                wrmHeaders.add(new WRMHeader(headerVersion, prKey, url));
                
                cryptKeys.add(new CryptKey(prKey));
            }
            cryptTracks.add(new CryptTrack(t.id, 8, null, cryptKeys, rollingKeySamples));
        }
        
        // Create our PSSH
        List<DRMInfoPSSH> psshList = new ArrayList<DRMInfoPSSH>();
        psshList.add(new PlayReadyPSSH(wrmHeaders));
        
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
            System.err.println("Could not open output file (" + outfile + ") for writing");
            System.exit(1);;
        }
    }
}
