#!/bin/bash 

# Confidential material under the terms of the Limited Distribution Non-disclosure
# Agreement between CableLabs and Comcast

function usage {
  echo ""
  echo "Encryption Script"
  echo "usage:"
  echo "   encrypt.sh -o <output_directory> -c <cryptfile> [-i <input_directory>] [INPUT_FILE]..."
  echo ""
  echo "OPTIONS"
  echo "  -o"
  echo "       The output directory where encrypted files will be written"
  echo ""
  echo "  -c"
  echo "       The cryptfile.  XML MP4Box input file used to define the encryption."
  echo ""
  echo "  -i"
  echo "       All files in the given directory will be encrypted.  Can be used instead of or in addition"
  echo "       to the list of files at the end of the command"
}

while getopts ":o:c:i:" opt; do
  case $opt in
    o)
      output_dir=$OPTARG
      ;;
    c)
      cryptfile=$OPTARG
      ;;
    i)
      input_dir=$OPTARG
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

if [ -z $cryptfile ]; then
  echo "Must provide cryptfile argument!"
  usage
  exit 1
fi
if [ -z $output_dir ]; then
  echo "Must provide output directory for encrypted media files!"
  usage
  exit 1
fi
if [ -z $input_dir -a ${#@} -eq 0 ]; then
  echo "No input media files specified!"
  usage
  exit 0
fi

mkdir -p $output_dir

# Encrypt all files in optional input directory
if [ ! -z $input_dir ]; then
  for file in `ls $input_dir`; do
    MP4Box -crypt $cryptfile $input_dir/$file -out $output_dir/`basename $file`
  done
fi

# Encrypt individual files specified at the end of the command
for file in $@; do
  MP4Box -crypt $cryptfile $file -out $output_dir/`basename $file`
done

