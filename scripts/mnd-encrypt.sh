#!/bin/sh

if [ -z "$MND_HOME" ]; then
    echo "ERROR: variable MND_HOME needs to specify path to MND (e.g. ~/mnd/exploded)"
    exit 1
fi 

if [ -z "$1" ]; then
    echo "Usage: mnd-encrypt.sh <plain-text> <crypter-passphrase>"
    exit 1
fi 

java -cp $MND_HOME/WEB-INF/classes com.company.mynetdiary.db.Crypter encrypt $2 $1