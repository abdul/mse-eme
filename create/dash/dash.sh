#!/bin/bash 

# Copyright (c) 2014, CableLabs, Inc.
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice,
# this list of conditions and the following disclaimer.
#
# 2. Redistributions in binary form must reproduce the above copyright notice,
# this list of conditions and the following disclaimer in the documentation
# and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

function usage {
  echo ""
  echo "MP4Box DASH Segmenter/Packager"
  echo "usage:"
  echo "   dash.sh -o <output_dir> -p <onDemand|live> [-m <manifest_file>] [INPUT_MEDIA_DESC]..."
  echo ""
  echo "INPUT_MEDIA_DESC: Provide the same input media file descriptions as are"
  echo " supported by MP4Box.  Run 'MP4Box -h dash' to see the list of supported"
  echo " options.  If you have multiplexed input files, you must use the #video"
  echo " and #audio attributes to demux the input and provide separate audio and"
  echo " and video Representations in the manifest.  If you have demultiplexed"
  echo " files and you do not wish to modify any of the default manifest attributes,"
  echo " you can simply specify the list of files here."
  echo ""
  echo "OPTIONS"
  echo "  -o"
  echo "       The output directory where media files and DASH manifest will be written"
  echo ""
  echo "  -p"
  echo "       The DASH Profile to use.  'onDemand' will generate a single segment file"
  echo "       (fragmented MP4) for each representation. 'live' will generate multiple"
  echo "       segment files along with an initialization segment for each representation"
  echo ""
  echo "  -m"
  echo "       Optional manifest file name.  Default is 'dash.mpd'"
}

mpd_file="dash.mpd"

while getopts ":o:m:p:" opt; do
  case $opt in
    o)
      output_dir=$OPTARG
      ;;
    m)
      mpd_file=$OPTARG
      ;;
    p)
      profile=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      usage
      exit 1
      ;;
    :)
      echo "Missing options argument for -$OPTARG" >&2
      usage
      exit 1
      ;;
  esac
done
shift $((OPTIND - 1))

if [ -z $output_dir ]; then
  echo "Must provide output directory for DASH manifest and media files"
  usage
  exit 1
fi
if [ -z $profile ]; then
  echo "Must provide DASH profile"
  usage
  exit 1
fi
if [ ${#@} -eq 0 ]; then
  echo "No input media files specified!"
  usage
  exit 0
fi

mkdir -p $output_dir

MP4Box -dash 10000 -frag 5000 -rap -bs-switching no -sample-groups-traf \
  -profile $profile -out $output_dir/$mpd_file $@ 

