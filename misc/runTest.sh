set -e

rnd() {
  echo `od if=/dev/urandom count=1 2>/dev/null | sha256sum | cut -f1  -d' '`
}

INIT_DIR=`pwd`

# Install fonts
mkdir -p ~/.fonts
cd ~/.fonts
wget "https://github.com/UprootLabs/grinder/releases/download/v1.0/css-testsuite-fonts-v2.zip"
unzip css-testsuite-fonts-v2.zip
rm -rf AhemExtra/
fc-cache -f

cd $INIT_DIR

GRINDER_KEY="$$$(rnd)"

mkdir ~/.gngr

ant -f src/build.xml build

xvfb-run -s "-dpi 96 -screen 0 900x900x24+32" ant -f src/build.xml -Dgngr.grinder.key="$GRINDER_KEY" run &> /dev/null  &

mkdir ~/grinder
cd ~/grinder
wget -O grinder.jar "https://github.com/UprootLabs/grinder/releases/download/v1.4.0/grinder-assembly-1.4.0.jar"

git clone --depth=1 https://github.com/UprootStaging/grinderBaselines.git ~/grinderBaselines

# TODO: Use soft link instead of copying?
cp -r ~/grinderBaselines/nightly-unstable ~/grinder

cd ~/grinder
python -m SimpleHTTPServer 8000 &> /dev/null &

java -jar grinder.jar prepare
java -jar grinder.jar compare gngr $GRINDER_KEY --baseLine=$HOME/grinderBaselines/gngr --uploadImg=y
java -jar grinder.jar checkBase data ../grinderBaselines/gngr
