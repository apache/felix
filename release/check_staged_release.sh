#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# This script verifies the signatures and checksums of a release.
#
# This script can be used to check the signatures and checksums of staged 
# Apache Felix Dependency Manager release using gpg.
# Usage:
#
#   check_staged_dependencymanager.sh <version> [<temp-dir>]
# 
# Where:
#   <version> represents the staged release version, e.g., 2.0.0;
#   <temp-dir> represents the location where the release artifacts
#              should be stored, defaults to /tmp/felix-staging if
#              omitted.


version=${1}
tmpDir=${2:-/tmp/felix-staging}

if [ ! -d "${tmpDir}" ]; then
    mkdir "${tmpDir}"
fi

if [ -z "${version}" -o ! -d "${tmpDir}" ]; then
    echo "Usage: check_staged_dependencymanager.sh <release-version> [temp-directory]"
    exit
fi

checkSig() {
    sigFile="$1.asc"
    if [ ! -f $sigFile ]; then
        echo "$sigFile is missing!!!"
        exit 1
    fi

    gpg --verify $sigFile 2>/dev/null >/dev/null
    if [ "$?" = "0" ]; then echo "OK"; else echo "BAD!!!"; fi
}

checkSum() {
    archive=$1
    sumFile=$2
    alg=$3
    if [ ! -f $sumFile ]; then
        echo "$sumFile is missing!!!"
        exit 1
    fi

    orig=`cat $sumFile | sed 's/.*: *//' | tr -d ' \t\n\r'`
    actual=`gpg --print-md $alg $archive | sed 's/.*: *//' | tr -d ' \t\n\r'`
    if [ "$orig" = "$actual" ]; then echo "OK"; else echo "BAD!!!"; fi
}

KEYS_URL="http://www.apache.org/dist/felix/KEYS"
REL_URL="https://dist.apache.org/repos/dist/dev/felix/org.apache.felix.dependencymanager-${version}/"
PWD=`pwd`

echo "################################################################################"
echo "                               IMPORTING KEYS                                   "
echo "################################################################################"
if [ ! -e "${tmpDir}/KEYS" ]; then
    wget --no-check-certificate -P "${tmpDir}" $KEYS_URL 
fi
gpg --import "${tmpDir}/KEYS" 

if [ ! -e "${tmpDir}/org.apache.felix.dependencymanager-${version}" ]
then
    echo "################################################################################"
    echo "                           DOWNLOAD STAGED REPOSITORY                           "
    echo "################################################################################"

    wget \
      -e "robots=off" --wait 1 -r -np "--reject=html,txt" "--follow-tags=" \
      -P "${tmpDir}/org.apache.felix.dependencymanager-${version}" -nH "--cut-dirs=5" --ignore-length --no-check-certificate \
      $REL_URL
else
    echo "################################################################################"
    echo "                       USING EXISTING STAGED REPOSITORY                         "
    echo "################################################################################"
    echo "${tmpDir}/org.apache.felix.dependencymanager-${version}"
fi

echo "################################################################################"
echo "                          CHECK SIGNATURES AND DIGESTS                          "
echo "################################################################################"

cd ${tmpDir}/org.apache.felix.dependencymanager-${version}
for f in `find . -type f | grep -v '\.\(asc\|sha\?\|md5\)$'`; do
    echo "checking $f" 

    echo -e "    ASC: \c"
    checkSig $f
    echo -e "    MD5: \c"
    checkSum $f "$f.md5" MD5
    echo -e "    SHA: \c"
    checkSum $f "$f.sha" SHA512
    echo ""
done

cd $PWD
echo "################################################################################"

