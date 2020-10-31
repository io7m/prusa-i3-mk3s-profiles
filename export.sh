#!/bin/sh -e

JAR_NAME="target/com.io7m.prusa-i3-mk3s-profiles-0.0.1-SNAPSHOT-main.jar"
JAVA="java -jar ${JAR_NAME}"
EXPORT="${JAVA} interpolate --file"

PROFILES="target/profiles"
FILAMENT="${PROFILES}/filament"
PRINT="${PROFILES}/print"
rm -rfv "${PROFILES}"

mkdir -p "${FILAMENT}"
mkdir -p "${PRINT}"

${EXPORT} "filament/PETG_Eryone_2020-10-26.ini"         > "${FILAMENT}/PETG_Eryone_2020-10-26.ini"
${EXPORT} "filament/PETG_Eryone_2020-10-26_Fan15.ini"   > "${FILAMENT}/PETG_Eryone_2020-10-26_Fan15.ini"
${EXPORT} "filament/PETG_Eryone_2020-10-26_Fanless.ini" > "${FILAMENT}/PETG_Eryone_2020-10-26_Fanless.ini"

${EXPORT} "print/LH0.3_N0.4_FastPLA.ini"        > "${PRINT}/LH0.3_N0.4_FastPLA.ini"
${EXPORT} "print/LH0.3_N0.4_PETG.ini"           > "${PRINT}/LH0.3_N0.4_PETG.ini"
${EXPORT} "print/LH0.3_N0.4_PETGStrong.ini"     > "${PRINT}/LH0.3_N0.4_PETGStrong.ini"
${EXPORT} "print/LH0.2_N0.4_PETG.ini"           > "${PRINT}/LH0.2_N0.4_PETG.ini"
${EXPORT} "print/LH0.2_N0.4_PETGStrong.ini"     > "${PRINT}/LH0.2_N0.4_PETGStrong.ini"
