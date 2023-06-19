#!/bin/bash

javaVersion="1.8.0"
pythonVersion="2.7"
upPythonVersion="2.8"
mavenVersion="3.3.9"
nodejsVersion="10.0.0"
version="0"

function checkJava()
{
	if [ -n "$JAVA_HOME" ] ; then
		java -version > /dev/null 2>&1
		ret=$?
	else
        echo "error:failed to get JAVA_HOME"
        exit 1
	fi
	if [ $ret -eq 0 ] ; then
		version=`java -version 2>&1 | awk -F[\"_] 'NR==1{print $2}'`
		if version_lt $version $javaVersion ; then
            echo "error:java version is $version ,too low"
			exit 1
		else
			echo "java version is $version "
        fi
	else
        echo "error:failed to exec 'java -version'"
        exit 1
	fi
}

function checkPython()
{
	python -V > /dev/null 2>&1
	ret=$?
	if [ $ret -eq 0 ] ; then
		version=`python -V 2>&1 | awk '{print $2}'`
        if [ $version \< $pythonVersion -o $version \> $upPythonVersion -o $version == $upPythonVersion ] ; then
            echo "error:python version is $version ,out of range"
			exit 1
		else
			echo "python version is $version "
        fi
	else
        echo "error:failed to exec 'python -V'"
		exit 1
	fi
}

function checkMaven()
{
	mvn -version > /dev/null 2>&1
	ret=$?
	if [ $ret -eq 0 ] ; then
		version=`mvn -version 2>&1 | awk 'NR==1{print $3}'`
        if version_lt $version $mavenVersion ; then
            echo "error:maven version is $version ,too low"
			exit 1
        else
			echo "maven version is $version "
		fi
	else
        echo "error:failed to exec 'mvn -version'"
		exit 1
	fi
}

function checkNodejs()
{
    node -v > /dev/null 2>&1
    ret=$?
    if [ $ret -eq 0 ] ; then
        version=`node -v 2>&1 | awk -F[\"v] 'NR==1{print $2}'`
        if version_lt $version $nodejsVersion ; then
            echo "error:nodejs version is $version ,too low"
            exit 1
        else
            echo "nodejs version is $version "
        fi
    else
        echo "error:failed to exec 'node -v'"
        exit 1
    fi
}

function version_lt() {
    test "$(echo "$@" | tr " " "\n" | sort -rV | head -n 1)" != "$1";
}

echo "***********************"
echo "package scm environmen:"
echo "java >= 1.8.0"
echo "python == 2.7.x"
echo "maven >= 3.3.9"
echo "nodejs >= 10.0.0"
echo "***********************"
echo "start to check"

checkJava
checkPython
checkMaven
checkNodejs

echo "success:check package environment success"