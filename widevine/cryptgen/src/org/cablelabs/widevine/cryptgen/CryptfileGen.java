
package org.cablelabs.widevine.cryptgen;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.cablelabs.cryptfile.CryptKey;
import org.cablelabs.cryptfile.CryptTrack;
import org.cablelabs.cryptfile.CryptfileBuilder;
import org.cablelabs.cryptfile.DRMInfoPSSH;
import org.cablelabs.widevine.Track;
import org.cablelabs.widevine.TrackType;
import org.cablelabs.widevine.cryptfile.WidevinePSSH;
import org.cablelabs.widevine.keyreq.KeyRequest;
import org.cablelabs.widevine.keyreq.ResponseMessage;
import org.cablelabs.widevine.proto.WidevinePSSHProtoBuf;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * This utility will build a MP4Box Widevine cryptfile for a given piece of content.  The steps
 * involved are:
 * <ol>
 *   <li>Request encryption keys/keyIDs from the Widevine key server for each track</li>
 *   <li>Build the Widevine PSSH</li>
 *   <li>Generate the MP4Box cryptfile</li>
 * </ol>
 *
 */
public class CryptfileGen {

    private static void usage() {
        System.out.println("usage:  WidevineKeyRequest [OPTIONS] <cryptfile> <content_id> <track_id>:<track_type>] [<track_id>:<track_type>]...");
        System.out.println("");
        System.out.println("\t<cryptfile> ");
        System.out.println("\t<content_id> is a unique string representing the content to be encrypted");
        System.out.println("\t<track_id> is the track ID from the MP4 file to be encrypted");
        System.out.println("\t<track_type> is one of HD, SD, or AUDIO describing the type of the associated track");
        System.out.println("");
        System.out.println("\tOPTIONS:");
        System.out.println("");
        System.out.println("\t-out <filename>");
        System.out.println("\t\tIf present, the cryptfile will be written to the given file. Otherwise output will be");
        System.out.println("\t\twritten to stdout");
        System.out.println("");
        System.out.println("\t-sign <sign_props_file>");
        System.out.println("\t\tIf present, key requests will be signed with the given key information.  <sign_props_file> is");
        System.out.println("\t\ta Java properties file with the following properties:");
        System.out.println("\t\t\turl:      Your assigned key server URL");
        System.out.println("\t\t\tkey:      Your assigned 32-byte signing key, hexadecimal notation");
        System.out.println("\t\t\tiv:       Your assigned 16-byte initialization vector, hexadecimal notation");
        System.out.println("\t\t\tprovider: Your assigned provider name");
        System.out.println("\t\tIf this argument is not present, the requests will be unsigned and the");
        System.out.println("\t\t\"widevine_test\" provider and URL will be used");
        System.out.println("");
        System.out.println("\t-roll <start_time>,<key_count>,<sample_count>");
        System.out.println("\t\tUsed for rolling keys only.  <start_time> is the integer time basis for the first");
        System.out.println("\t\trequested key.  Could be epoch or media time or anything else meaningful.  <key_count>");
        System.out.println("\t\tis the integer number of keys requested.  <sample_count> is the number of consecutive");
        System.out.println("\t\tsamples to be encrypted with each key before moving to the next.");
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

        // Track list -- one slot for each track type
        Track[] track_args = new Track[TrackType.NUM_TYPES.ordinal()];
        
        // Should the request be signed
        String signingFile= null;
        
        // Rolling keys
        int rollingKeyStart = -1;
        int rollingKeyCount = -1;
        int rollingKeySamples = -1;
        
        String outfile = null;
        
        // Parse arguments
        String content_id_str = null;
        for (int i = 0; i < args.length; i++) {
            
            // Parse options
            if (args[i].startsWith("-")) {
                String[] subopts;
                if ((subopts = checkOption("-out", args, i, 1)) != null) {
                    outfile = subopts[0];
                    i++;
                }
                else if ((subopts = checkOption("-sign", args, i, 1)) != null) {
                    signingFile = subopts[0];
                    i++;
                }
                else if ((subopts = checkOption("-roll", args, i, 3)) != null) {
                    rollingKeyStart = Integer.parseInt(subopts[0]);
                    rollingKeyCount = Integer.parseInt(subopts[1]);
                    rollingKeySamples = Integer.parseInt(subopts[2]);
                    i++;
                }
                else {
                    usage();
                    System.err.println("Illegal argument: " + args[i]);
                    System.exit(1);;
                }
                
                continue;
            }
            
            // Get content ID
            if (content_id_str == null) {
                content_id_str = args[i];
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
                TrackType trackType = TrackType.valueOf(track_desc[1]);
                t.type = trackType;
                t.id = Integer.parseInt(track_desc[0]);
                track_args[t.type.ordinal()] = t;
            }
            catch (IllegalArgumentException e) {
                usage();
                System.err.println("Illegal track_type -- " + track_desc[1]);
                System.exit(1);;
            }
        }
        
        // Request keys
        List<Track> trackList = new ArrayList<Track>();
        for (Track t : track_args) {
            if (t != null)
                trackList.add(t);
        }
        KeyRequest request = (rollingKeyCount != -1 && rollingKeyStart != -1) ?
            new KeyRequest(content_id_str, trackList, rollingKeyStart, rollingKeyCount) :
            new KeyRequest(content_id_str, trackList);
        if (signingFile != null) {
            try {
                request.setSigningProperties(signingFile);
            }
            catch (Exception e) {
                usage();
                System.err.println("Error in signing file: " + e.getMessage());
                System.exit(1);;
            }
        }
        ResponseMessage m = request.requestKeys();
        if (m.status != ResponseMessage.StatusCode.OK) {
            System.err.println("Received error from key server! Code = " + m.status.toString());
            System.exit(1);
        }
    
        // The Widevine key server provides the PSSH data directly to us.  Optionally, we could
        // build our own WidevineCencHeader protobuf object from the information in the response.
        // For rolling keys it might be a better solution to build our own, since the widevine server
        // currently sends a new PSSH for every key in every track.  Building our own, we could keep
        // a single PSSH per track
        
        List<DRMInfoPSSH> psshList = new ArrayList<DRMInfoPSSH>();
        List<CryptTrack> cryptTracks = new ArrayList<CryptTrack>();
        
        // Build PSSH's (has to be one for each track in the Widevine world)
        for (ResponseMessage.Track track : m.tracks) {
            for (ResponseMessage.Track.PSSH pssh : track.pssh) {
                
                // Only widevine DRM for now
                if (!pssh.drm_type.equalsIgnoreCase("widevine"))
                    continue;
                
                WidevinePSSHProtoBuf.WidevineCencHeader wvPSSH = null;
                try {
                    wvPSSH = WidevinePSSHProtoBuf.WidevineCencHeader.parseFrom(Base64.decodeBase64(pssh.data));
                }
                catch (InvalidProtocolBufferException e) {
                    System.err.println("Could not parse PSSH protobuf from key response message");
                    System.exit(1);;
                }
                psshList.add(new WidevinePSSH(wvPSSH));
            }
                
            // Get the keys for this track and add to our cryptfile
            List<CryptKey> keyList = new ArrayList<CryptKey>();
            keyList.add(new CryptKey(Base64.decodeBase64(track.key_id), Base64.decodeBase64(track.key)));
            cryptTracks.add(new CryptTrack(track_args[track.type.ordinal()].id, 8, null,
                                           keyList, rollingKeySamples));
        }
        
        CryptfileBuilder cfBuilder = new CryptfileBuilder(CryptfileBuilder.ProtectionScheme.AES_CTR,
                                                          cryptTracks, psshList);
        
        // Write the output
        try {
            OutputStream os = (outfile == null) ? System.out : new FileOutputStream(outfile);
            cfBuilder.writeCryptfile(os);;
        }
        catch (FileNotFoundException e) {
            System.err.println("Could not open output file (" + outfile + ") for writing");
            System.exit(1);;
        }
        
    }
}
