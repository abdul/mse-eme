#!/bin/bash 

# Confidential material under the terms of the Limited Distribution Non-disclosure
# Agreement between CableLabs and Comcast

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
  echo " and video Representations in the manifest"
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

MP4Box -dash 10000 -rap -bs-switching no -sample-groups-traf \
  -profile $profile -out $output_dir/$mpd_file $@ 

