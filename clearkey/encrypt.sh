#!/bin/bash

function usage {
  echo ""
  echo "PlayReady Encryption Script"
  echo "usage:"
  echo "   encrypt.sh -o <output_directory> -v [4000|4100] [INPUT_FILE]..."
}

while getopts ":o:v:" opt; do
  case $opt in
    o)
      output_dir=$OPTARG
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
  echo "Must provide output directory for encrypted media files"
  usage
  exit 1
fi
if [ -z $@ ]; then
  echo "No media files specified!"
  usage
  exit 0
fi

mkdir -p $output_dir
for file in $@; do
  MP4Box -crypt clearkey_cenc.xml $file -out $output_dir/`basename $file`
done

