/* Copyright (c) 2014, CableLabs, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.cablelabs.playready.cryptgen;

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
import org.cablelabs.clearkey.cryptfile.ClearKeyRemotePSSH;
import org.cablelabs.cryptfile.CryptKey;
import org.cablelabs.cryptfile.CryptTrack;
import org.cablelabs.cryptfile.CryptfileBuilder;
import org.cablelabs.cryptfile.DRMInfoPSSH;
import org.cablelabs.cryptfile.KeyPair;
import org.cablelabs.playready.PlayReadyKeyPair;
import org.cablelabs.playready.WRMHeader;
import org.cablelabs.playready.cryptfile.PlayReadyPSSH;

public class CryptfileGen {
    
    private static void usage() {
        System.out.println("Microsoft PlayReady MP4Box cryptfile generation tool.");
        System.out.println("");
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
        System.out.println("\t-help");
        System.out.println("\t\tDisplay this usage message.");
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
        System.out.println("");
        System.out.println("\t-ck_remote <url>");
        System.out.println("\t\tAdd CableLabs 'Remote' ClearKey PSSH to the cryptfile.  <url> is the ClearKey server");
        System.out.println("\t\tURL.");
        System.out.println("");
        System.out.println("\t-ck_json");
        System.out.println("\t\tAdd CableLabs 'JSON' ClearKey PSSH to the cryptfile.");
    }
    
    private static class Track {
        List<String> keyIDs = new ArrayList<String>();
        int id;
    }
    
    private static void invalidOption(String option) {
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
        String url = "http://playready.directtaps.net/pr/svc/rightsmanager.asmx?PlayRight=1&UseSimpleNonPersistentLicense=1";
        List<Track> tracks = new ArrayList<Track>();
        WRMHeader.Version headerVersion = WRMHeader.Version.V_4000;
        
        // Clearkey
        boolean clearkey = false;
        URL clearkey_url = null;
        
        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            
            // Parse options
            if (args[i].startsWith("-")) {
                String[] subopts;
                if ((subopts = checkOption("-help", args, i, 0)) != null) {
                    usage();
                    System.exit(0);
                }
                else if ((subopts = checkOption("-out", args, i, 1)) != null) {
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
                        errorExit("Illegal WRMHeader version: " + subopts[0]);
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
                else if ((subopts = checkOption("-ck_json", args, i, 0)) != null) {
                    clearkey = true;
                }
                else if ((subopts = checkOption("-ck_remote", args, i, 1)) != null) {
                    try {
                        clearkey_url = new URL(subopts[0]);
                        clearkey = true;
                    }
                    catch (MalformedURLException e) {
                        errorExit("Illegal clearkey URL: " + e.getMessage());
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
                errorExit("Illegal track specification: " + args[i]);
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
                errorExit("Illegal track_type -- " + track_desc[1]);
            }
            catch (FileNotFoundException e) {
                errorExit("Key ID file not found: " + e.getMessage());
            }
            catch (IOException e) {
                errorExit("Error reading from Key ID file: " + e.getMessage());
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
        
        // Add clearkey PSSH if requested
        if (clearkey) {
            if (clearkey_url != null) {
                // Build list of all key IDs
                List<String> keyIDs = new ArrayList<String>();
                System.out.println("Ensure the following keys are installed on the ClearKey server:");
                for (CryptTrack t : cryptTracks) {
                    for (CryptKey key : t.getKeys()) {
                        System.out.println("\t" + Hex.encodeHexString(key.getKeyPair().getID()) +
                                           " : " + Hex.encodeHexString(key.getKeyPair().getKey()));
                        keyIDs.add(Hex.encodeHexString(key.getKeyPair().getID()));
                    }
                }
                System.out.println("");
                psshList.add(new ClearKeyRemotePSSH(clearkey_url, keyIDs));
            }
            else {
                // Build list of all key pairs
                List<KeyPair> keys = new ArrayList<KeyPair>();
                for (CryptTrack t : cryptTracks) {
                    for (CryptKey key : t.getKeys()) {
                        keys.add(key.getKeyPair());
                    }
                }
                psshList.add(new ClearKeyJsonPSSH(keys));
            }
        }
        
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
