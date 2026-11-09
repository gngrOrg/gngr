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

mkdir -p ~/.gngr

ant -f src/build.xml build

xvfb-run -s "-dpi 96 -screen 0 900x900x24+32" ant -f src/build.xml -Dgngr.grinder.key="$GRINDER_KEY" run-harness &> /dev/null  &

mkdir -p ~/grinderTest
cd ~/grinderTest
wget -O grinder.jar "https://github.com/gngrOrg/grinder/releases/download/v1.4.0/grinder-assembly-1.4.0.jar"

git clone --depth=1 https://github.com/UprootStaging/grinderBaselines.git grinderBaselines

# TODO: Use soft link instead of copying?
#cp -r ~/grinderBaselines/nightly-unstable ~/grinderTest
ln -s grinderBaselines/nightly-unstable ./

cd ~/grinderTest
python3 -m http.server 8000 &> /dev/null &

java -jar grinder.jar prepare
java -jar grinder.jar compare gngr $GRINDER_KEY --baseLine=$HOME/grinderTest/grinderBaselines/gngr --uploadImg=y
java -jar grinder.jar checkBase data ../grinderBaselines/gngr
