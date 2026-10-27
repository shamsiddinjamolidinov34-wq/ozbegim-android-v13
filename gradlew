#!/bin/sh
# O'zbegim: Gradle ishga tushirgich (gradlew).
#
# 1) gradle/wrapper/gradle-wrapper.jar bo'lsa, standart Gradle Wrapper ishlatiladi.
# 2) Bo'lmasa, tizimdagi Gradle 8.9 yoki undan yangisi ishlatiladi (AGP 8.7.3 uchun kamida 8.9 kerak).
# 3) U ham bo'lmasa, Gradle 8.9 rasmiy manzildan (HTTPS) yuklab olinadi va ishlatiladi (internet kerak).
# Android Studio'da bu fayl kerak emas. Rasmiy wrapper hosil qilish: gradle wrapper --gradle-version 8.9

GRADLE_VERSION=8.9
APP_HOME=$(cd "$(dirname "$0")" > /dev/null 2>&1 && pwd -P) || exit 1
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# 1) rasmiy wrapper
if [ -f "$WRAPPER_JAR" ]; then
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        JAVACMD=java
    fi
    exec "$JAVACMD" -Dorg.gradle.appname=gradlew -classpath "$WRAPPER_JAR" \
        org.gradle.wrapper.GradleWrapperMain "$@"
fi

# Gradle versiyasi 8.9 yoki undan yuqorimi?
gradle_ok() {
    major=${1%%.*}
    rest=${1#*.}
    minor=${rest%%.*}
    case "$major$minor" in
        "" | *[!0-9]*) return 1 ;;
    esac
    if [ "$major" -gt 8 ]; then return 0; fi
    if [ "$major" -eq 8 ] && [ "$minor" -ge 9 ]; then return 0; fi
    return 1
}

# 2) tizim Gradle'i
if command -v gradle > /dev/null 2>&1; then
    SYS_VER=$(gradle --version 2> /dev/null | sed -n 's/^Gradle \([0-9][0-9.]*\).*$/\1/p' | head -n 1)
    if [ -n "$SYS_VER" ] && gradle_ok "$SYS_VER"; then
        exec gradle "$@"
    fi
fi

# 3) Gradle'ni yuklab olish
BASE="${GRADLE_USER_HOME:-${HOME:-/tmp}/.gradle}/ozbegim-gradle"
DIST="$BASE/gradle-$GRADLE_VERSION"
if [ ! -x "$DIST/bin/gradle" ]; then
    URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    ZIP="$BASE/gradle-$GRADLE_VERSION-bin.zip"
    mkdir -p "$BASE" || { echo "XATO: $BASE papkasini yaratib bo'lmadi" >&2; exit 1; }
    echo "Gradle $GRADLE_VERSION yuklab olinmoqda: $URL" >&2
    rm -f "$ZIP.part"
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL "$URL" -o "$ZIP.part" || { rm -f "$ZIP.part"; echo "XATO: Gradle yuklab olinmadi (internet yo'qmi?)" >&2; exit 1; }
    elif command -v wget > /dev/null 2>&1; then
        wget -q "$URL" -O "$ZIP.part" || { rm -f "$ZIP.part"; echo "XATO: Gradle yuklab olinmadi (internet yo'qmi?)" >&2; exit 1; }
    else
        echo "XATO: Gradle $GRADLE_VERSION yoki yangisi o'rnatilmagan, yuklab olish uchun curl yoki wget ham yo'q" >&2
        exit 1
    fi
    mv "$ZIP.part" "$ZIP" || exit 1
    if command -v unzip > /dev/null 2>&1; then
        unzip -q -o "$ZIP" -d "$BASE" || { echo "XATO: arxivni ochib bo'lmadi" >&2; exit 1; }
    elif command -v python3 > /dev/null 2>&1; then
        python3 -m zipfile -e "$ZIP" "$BASE" || { echo "XATO: arxivni ochib bo'lmadi" >&2; exit 1; }
    elif command -v jar > /dev/null 2>&1; then
        (cd "$BASE" && jar xf "$ZIP") || { echo "XATO: arxivni ochib bo'lmadi" >&2; exit 1; }
    else
        echo "XATO: arxivni ochish uchun unzip, python3 yoki jar kerak" >&2
        exit 1
    fi
    chmod +x "$DIST/bin/gradle" 2> /dev/null
    if [ ! -x "$DIST/bin/gradle" ]; then
        echo "XATO: $DIST/bin/gradle topilmadi" >&2
        exit 1
    fi
fi
exec "$DIST/bin/gradle" "$@"
