#!/bin/sh
echo Updating Javatools from SVN
svn update javatools
echo Updating Basics from SVN 
svn update basics2s
echo Updating YAGO2s from SVN 
svn update yago2s
echo Updating Converters from SVN
svn update converters2s

echo Compiling Javatools
find javatools/src/ -name *.java > sources_list.txt
 /usr/lib/jvm/java-7-sun/bin/javac -d "javatools/bin" @sources_list.txt

echo Compiling Basics
find basics2s/src/ -name *.java > sources_list.txt
 /usr/lib/jvm/java-7-sun/bin/javac -cp "javatools/bin" -d "basics2s/bin" @sources_list.txt

echo Compiling Converters
# find converters2s/src/ -name *.java > sources_list.txt
# /usr/lib/jvm/java-7-sun/bin/javac -cp "basics2s/bin:javatools/bin"  -d "converters2s/bin" @sources_list.txt
 /usr/lib/jvm/java-7-sun/bin/javac -cp "basics2s/bin:javatools/bin"  -d "converters2s/bin" converters2s/src/converters/TsvConverter.java

echo Compiling YAGO2s
find yago2s/src/ -name *.java > sources_list.txt
 /usr/lib/jvm/java-7-sun/bin/javac -cp "basics2s/bin:javatools/bin"  -d "yago2s/bin" @sources_list.txt
