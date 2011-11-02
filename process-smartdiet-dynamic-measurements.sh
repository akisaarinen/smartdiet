#!/bin/bash
BASE_DIR=`dirname $0`
SCRIPT_DIR="${BASE_DIR}/energyanalysis/scripts"
SRC_DIR="${BASE_DIR}/energyanalysis/src"

# Colors
DULL=0
BRIGHT=1
FG_RED=31
FG_GREEN=32
FG_YELLOW=33
BG_NULL=00
ESC="\033"
NORMAL="$ESC[m\]"
RESET="$ESC[${DULL};${FG_WHITE};${BG_NULL}m"
BRIGHT_RED="$ESC[${BRIGHT};${FG_RED}m"
BRIGHT_GREEN="$ESC[${BRIGHT};${FG_GREEN}m"
BRIGHT_YELLOW="$ESC[${BRIGHT};${FG_YELLOW}m"

function show_error() {
    echo -en "${BRIGHT_RED}ERROR: ${RESET}"
    echo -e "$1"
    exit 1
}

function show_warning() {
    echo -en "=> ${BRIGHT_YELLOW}Warning: ${RESET}"
    echo -e "$1"
}

function verify_dmtracedump() {
    echo -e "* Checking for Android SDK${RESET} in \$ANDROID_SDK, set to '${ANDROID_SDK}'"
    if [[ ! -d "$ANDROID_SDK/tools" ]]; then
        show_error "No Android_SDL tools found from '${ANDROID_SDK}/tools', remember to set ANDROID_SDK environment variable"
    fi
    DMTRACEDUMP="${ANDROID_SDK}/tools/dmtracedump"
    echo -e "* Checking dmtracedump from '${DMTRACEDUMP}'"
    if [[ ! -f "$DMTRACEDUMP" ]]; then
        show_error "No dmtracedump tool found from android sdk"
    fi
}

function verify_ruby() {
    echo -e "* Checking ruby installation"
    RUBY=`which ruby`
    if [[ ! -n "$RUBY" ]]; then
        show_error "No ruby installation found in PATH"
    fi

}

function verify_smartdiet_scripts() {
    echo -e "* Checking SmartDiet scripts"
    if [[ ! -d "$SRC_DIR" ]]; then
        show_error "No script sources found from '${SRC_DIR}'"
    fi
}

function verify_analyzer() {
    echo -e "* Checking SmartDiet analyzer program"
    ANALYZER="${SRC_DIR}/analyzer.rb"
    if [[ ! -f "$ANALYZER" ]]; then
        show_error "Analyzer script not found from '${ANALYZER}'"
    fi
}

function verify_r() {
    echo -e "* Checking R installation"
    R_SCRIPT=`which Rscript` 
    if [[ ! -n "$R_SCRIPT" ]]; then
        show_error "No Rscript binary found from PATH"
    fi
}

function verify_target_dir() {
    echo -e "* Checking for target dir '${TARGET_DIR}'"
    if [[ ! -d "$TARGET_DIR" ]]; then
        show_error "Target dir not found"
    fi
}

function verify_file() {
    filename="$1"
    echo "* Checking for '${filename}'"
    if [[ ! -f "$filename" ]]; then
        show_error "File '${filename}' not found"
    fi
}

function optional_file() {
    filename="$1"
    echo "* Checking for optional file '${filename}'"
    if [[ ! -f "$filename" ]]; then
        show_warning "File '${filename}' not found, all features might not be usable"
    fi
}

if [[ ! -n "$1" ]]; then
    show_error "Usage: `basename $0` <measurement_directory>"
fi


echo -e "${BRIGHT_GREEN}Checking prequisites...${RESET}"
verify_dmtracedump
verify_ruby
verify_r
verify_smartdiet_scripts
verify_analyzer

TARGET_DIR="$1"
FILE_DMESG="${TARGET_DIR}/dmesg.out"
FILE_TRACE="${TARGET_DIR}/program.trace"
FILE_PT4="${TARGET_DIR}/power.pt4"

echo -e "${BRIGHT_GREEN}Checking for measurement files...${RESET}"
verify_target_dir
verify_file "$FILE_DMESG"
verify_file "$FILE_TRACE"
optional_file "$FILE_PT4"

echo -e "${BRIGHT_GREEN}Processing...${RESET}"

FILE_NW="${TARGET_DIR}/program.nw"
FILE_DUMP="${TARGET_DIR}/program.dump"
FILE_ABS="${TARGET_DIR}/program.abs"
FILE_NW_ABS="${TARGET_DIR}/program-network.abs"
FILE_TS="${TARGET_DIR}/program.timestamp"

echo "== Preprocessing raw files with ruby and bash =="
"$RUBY" "${SRC_DIR}/convert-dmesg.rb" < "$FILE_DMESG" > "$FILE_NW"
"$DMTRACEDUMP" -o "$FILE_TRACE" > "$FILE_DUMP" 
"$RUBY" "${SRC_DIR}/convert-to-abs-time.rb" "${TARGET_DIR}/program" > "$FILE_ABS"
"${SCRIPT_DIR}/grep-network.sh" "$FILE_ABS" > "$FILE_NW_ABS"
echo "timestamp" > "$FILE_TS"
grep "profiling-start-time" "$FILE_TRACE" | cut -c22- >> "$FILE_TS"

echo "== Running R scripts to match packet traces to method traces =="
FIND_MATCHING_METHODS_R="${SCRIPT_DIR}/find-matching-methods.r"
"$R_SCRIPT" "$FIND_MATCHING_METHODS_R" "${TARGET_DIR}/program" || show_error "Error running R script from '${FIND_MATCHING_METHODS_R}'"

echo "== Run scripts to generate graphs based on analysis == "
"$RUBY" "$ANALYZER" -net "${TARGET_DIR}/program"
