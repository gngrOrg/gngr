#!/bin/zsh

if [[ $# -lt 3 ]] then
  echo Too few arguments;
  exit;
fi

_absolute_path () {
  echo -n `readlink -f $1`
}

BUILD_RESOURCES_DIR=$(_absolute_path $1)
BUILD_DIR=$(_absolute_path $2)
DIST_DIR=$(_absolute_path $3)

echo Build Res dir : $BUILD_RESOURCES_DIR
echo Build dir     : $BUILD_DIR
echo Dist  dir     : $DIST_DIR

set -e

ASSEMBLY_DIR=$BUILD_DIR/unoAssemblyDir

_expandJar () {
  jarFile=$1
  basename=`basename -s .jar $jarFile`
  subdir="uno\$\$\$$basename"
  mkdir $ASSEMBLY_DIR/$subdir
  ( cd $ASSEMBLY_DIR/$subdir; jar xf $jarFile )
  echo $basename >> $ASSEMBLY_DIR/unoConfig
}

rm -rf $ASSEMBLY_DIR
mkdir $ASSEMBLY_DIR
(cd $ASSEMBLY_DIR; jar xf $BUILD_RESOURCES_DIR/uno_0.0.2.jar)
cp $BUILD_RESOURCES_DIR/flat-extensions $ASSEMBLY_DIR/
echo org.lobobrowser.main.EntryPoint >> $ASSEMBLY_DIR/unoConfig
_expandJar $BUILD_DIR/core.jar
for jarFile in $DIST_DIR/libs/*.jar
do
  _expandJar $jarFile
done
(cd $DIST_DIR; jar cfe gngr.jar uno.Uno -C $ASSEMBLY_DIR ./)
rm -r $ASSEMBLY_DIR
echo Jar created at $DIST_DIR/gngr.jar
