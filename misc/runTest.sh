set -ex

rnd() {
  echo `od if=/dev/urandom count=1 2>/dev/null | sha256sum | cut -f1  -d' '`
}

INIT_DIR=`pwd`

# Install fonts
mkdir -p ~/.fonts
cd ~/.fonts
wget -nc "https://github.com/gngrOrg/grinder/releases/download/v1.0/css-testsuite-fonts-v2.zip"
unzip -n css-testsuite-fonts-v2.zip
rm -rf AhemExtra/
fc-cache -f

cd $INIT_DIR

GRINDER_KEY="$$$(rnd)"
TEST_DIR="$HOME/grinderTest"
PROFILE_DIR="$TEST_DIR/testProfile"

#TARGET="run"
TARGET="run-harness"

ant -f src/build.xml build

xvfb-run -s "-dpi 96 -screen 0 900x900x24+32" ant -f src/build.xml -Dgngr.profile="$PROFILE_DIR" -Dgngr.grinder.key="$GRINDER_KEY" $TARGET &> /dev/null  &

mkdir -p $TEST_DIR
cd $TEST_DIR
GRINDER_VERSION="1.4.2"
GRINDER_JAR="grinder-assembly-$GRINDER_VERSION.jar"
wget -nc "https://github.com/gngrOrg/grinder/releases/download/v${GRINDER_VERSION}/grinder-assembly-${GRINDER_VERSION}.jar"

rm -rf grinderBaselines
git clone --depth=1 https://github.com/UprootStaging/grinderBaselines.git grinderBaselines

ln -s -f grinderBaselines/nightly-unstable ./

python3 -m http.server 8000 &> /dev/null &

java -jar ${GRINDER_JAR} prepare
java -jar ${GRINDER_JAR} compare gngr $GRINDER_KEY --baseLine=$TEST_DIR/grinderBaselines/gngr --profileDir="$PROFILE_DIR" --uploadImg=y
java -jar ${GRINDER_JAR} checkBase data ../grinderBaselines/gngr
