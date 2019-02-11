#!/bin/bash

SCRIPT=$0
WORK_DIR=snf4j
REPO_NAME=snf4j
VERSION=$1
JARS=( core core-slf4j example )
JARS_LEN=${#JARS[@]}
DOCS=( core )
DOCS_LEN=${#DOCS[@]}
export TRAVIS=true
export SNF4J_SKIP_TEST_LOGGING=true

if [ ¨$VERSION¨ == ¨¨ ]; then
  echo Usage: release.sh VERSION
  exit 1
fi

echo Enter passphrase:
read -s PASS

echo ${PASS} | gpg --batch --no-tty --yes --passphrase-fd 0 -ab $SCRIPT
[ $? -eq 0 ] || exit $?
rm -f $SCRIPT.asc 

mkdir $WORK_DIR
[ $? -eq 0 ] || exit $?
echo $REPO_NAME working directory created successfully

cd $WORK_DIR
git clone https://github.com/snf4j/snf4j.git
[ $? -eq 0 ] || exit $?
echo $REPO_NAME cloned successfully

cd $REPO_NAME
git checkout -b bld$VERSION v$VERSION
[ $? -eq 0 ] || exit $?
echo $REPO_NAME checked out the tag v$VERSION successfully

mvn install -DskipTests
[ $? -eq 0 ] || exit $?
echo $REPO_NAME installed successfully

mvn package
[ $? -eq 0 ] || exit $?
echo $REPO_NAME built successfully

mvn javadoc:jar
[ $? -eq 0 ] || exit $?
echo $REPO_NAME javadoc created successfully

mvn source:jar
[ $? -eq 0 ] || exit $?
echo $REPO_NAME source created successfully

for (( i=0; i<$JARS_LEN; i++)); do
  FILE=$REPO_NAME-${JARS[${i}]}/target/$REPO_NAME-${JARS[${i}]}-$VERSION
  echo ${PASS} | gpg --batch --no-tty --yes --passphrase-fd 0 -ab $FILE.jar
  [ $? -eq 0 ] || exit $?
  echo $FILE.jar signed successfully
  echo ${PASS} | gpg --batch --no-tty --yes --passphrase-fd 0 -ab $FILE-sources.jar
  [ $? -eq 0 ] || exit $?
  echo $FILE-sources.jar signed successfully
  cp $REPO_NAME-${JARS[${i}]}/pom.xml $FILE.pom
  echo ${PASS} | gpg --batch --no-tty --yes --passphrase-fd 0 -ab $FILE.pom
  [ $? -eq 0 ] || exit $?
  echo $FILE.pom signed successfully
done

for (( i=0; i<$DOCS_LEN; i++)); do
  FILE=$REPO_NAME-${DOCS[${i}]}/target/$REPO_NAME-${DOCS[${i}]}-$VERSION
  echo ${PASS} | gpg --batch --no-tty --yes --passphrase-fd 0 -ab $FILE-javadoc.jar
  [ $? -eq 0 ] || exit $?
  echo $FILE-javadoc.jar signed successfully
done

echo $REPO_NAME released successfully

done

echo $REPO_NAME released successfully
