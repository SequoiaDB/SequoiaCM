#!/bin/bash

setLogPath(){
  logPath="$(pwd)/../log/"
  export logPath=$logPath
  mkdir -p $logPath
}

#deal with print msg, $1 is msg, $2 is log output file
printTimeAndMsg(){
  time=`date "+%Y-%m-%d %H:%M:%S"`
  if [ "$2" = "" ]; then
    echo "$time $1"
    return
  fi

  if [[ "$2" = "error.out" || "$2" = "clean.out" || "$2" = "zkClean.out" ]] ; then
    outputFile="$logPath$2"
    processOutFileSize $outputFile
    echo "$time $1" >> $outputFile
  else
    processOutFileSize $logPath"error.out"
    echo "$time $1,$2 file not this file" >> $logPath"error.out"
  fi
}

#find jar
findJar(){
  if [  "$1" = "clean" ] ; then
    res=$(find ../lib/ -maxdepth 1 -name 'sequoiacm-clean-file-*.jar')
  elif [ "$1" = "transfer" ]; then
    res=$(find ../lib/ -maxdepth 1 -name 'sequoiacm-transfer-file-*.jar')
  else
    echo "failed to find jar: $1 jar is a illegal parameter"
    return 1
  fi

  res_count=$(echo ${res} | wc -w)
  if [ $(echo $res_count) = '1' ] ;then
    isHF=$(echo $res | grep "(EGB)")
    if [ "$isHF" != "" ] ; then
      res=${res%%(EGB)*}"\(EGB\).jar"
    fi
    echo $res
    return 0
  else
    echo "failed to find jar: can't find sequoiacm-$1-file jar or jar not only"
    return 1
  fi
}

checkOutFileParameter(){
  if [[ ! ${maxFileNumber} =~ ^[0-9]+$ ]] ; then
    echo "maxFileNumber=${maxFileNumber},maxFileNumber must be a natural number"
    return 1
  fi

  if [[ ! ${maxFileSize} =~ ^[0-9]+$ ]] ; then
    echo "maxFileSize=${maxFileSize},maxFileSize must be a natural number"
    return 1
  fi

  return 0
}
#deal with clear.out and error.out size and number
processOutFileSize(){
  fileName=$1

  #file is not exit
  if [ ! -f $fileName ] ; then
    return 0
  fi

  fileSize=$(ls -l $fileName | awk '{print $5}')
  if (( fileSize < $((${maxFileSize}*1024*1024)) )) ; then
    return 0
  fi

  #check error.out or clean.out exist
  if [ -f $fileName ] ; then
    $(mv $fileName $fileName".$(date +%Y-%m-%d_%H-%M-%S)")
  fi

  files=$(ls -r $fileName.*)
  count=0
  for file in ${files[*]}
  do
    ((count++))
    if ((count>${maxFileNumber})); then
      $(rm -f $file)
    fi
  done
}

# if use option '--version/-v', print version information
processPrintVersionInfo(){
  # get the first argument as the jarType
  jarType=$1
  # get the rest arguments
  lastArgs="${@:2}"
  # detect --version / -v
  for i in $lastArgs; do
    if [[ "$i" == "--version" || "$i" == "-v" ]]; then
        jarPath=$(findJar $jarType)
        if [ ! $? -eq 0 ] ;then
          echo jarPath # print findJar error msg
          exit 1
        fi

        commandStr="java -jar $jarPath --version"
        eval $commandStr
        if [ ! $? -eq 0 ]; then
          echo "print version information failed"
          exit 1
        else
          exit 0
        fi
    fi
  done
}