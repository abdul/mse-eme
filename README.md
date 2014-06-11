# CableLabs MSE/EME Reference Tools

A reference toolkit for creating premium, adaptive bitrate (ABR) content for playback using the latest HTML5 extensions.  [Media Source Extensions (MSE)](http://www.w3.org/TR/media-source/) allow Javascript applications to deliver individual buffers of audio, video, and data to the browser's media pipeline which enables more flexible playback of ABR content such as [MPEG-DASH](http://mpeg.chiariglione.org/standards/mpeg-dash) or [Apple HLS](https://developer.apple.com/streaming/).  [Encrypted Media Extensions (EME)](http://www.w3.org/TR/encrypted-media/) allow Javascript applications to pass content license request/response messages between a DRM-specific Content Decryption Module (CDM) within the browser and a remote license server.

## Scope

The current CableLabs reference tools are designed to generate content using a specific set of industry standards that we will think capture a large footprint of needs amongst the user community.  We hope to expand the scope in the future to encompass additional codecs and container types.

* MP4 (ISOBMFF) Container
* AVC/H.264 Video Codec
* AAC Audio Codec
* MPEG-DASH Adaptive Bitrate Packaging
* ISO Common Encryption

## DRM

Our tools will include support for proprietary and open DRM systems as documentation and test servers are made available to us.  Here is a table that indicates the current status of each DRM with our tools.

| DRM | Status | Notes |
|-----|--------|-------|
|Microsoft PlayReady|Working|Uses the [PlayReady test server](http://playready.directtaps.net/pr/doc/customrights/)|
|Google Widevine|In Progress|Key request and message signing are working, but license request during playback is failing.  Users wishing to create signed requests will need to contact Widevine for a private test server and signing keys.|
|CableLabs ClearKey|Working|CableLabs-specific implementation of [ClearKey](http://www.w3.org/TR/encrypted-media/#simple-decryption-clear-key)|
|Adobe Access/PrimeTime|Not Started||
|Apple FairPlay|Not Started||

## HTML5 Player Application

For playback of encrypted DASH content using MSE/EME, we have augmented the dash.js player to support some additional DRMs.  Also, we have added an EME-specific logging window to highlight the EME process as it takes place.  Finally, we have improved support for playback of content that uses ISO Common Encryption.

## 3rd Party Acknowledgments

Our tools rely heavily on the following great open source and/or free library projects

* [LibAV](http://libav.org/): transcoding and adaptive bitrate generation
* [MP4Box](http://gpac.wp.mines-telecom.fr/mp4box/): encryption and MPEG-DASH packaging
* [x264](http://www.videolan.org/developers/x264.html): AVC/H.264 codec for video
* [libfdk_aac](http://www.iis.fraunhofer.de/en/bf/amm/implementierungen/fdkaaccodec.html): Fraunhofer FDK AAC codec for audio

