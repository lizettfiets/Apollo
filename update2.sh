#!/bin/bash

# first parameter is a current directory, where wallet is executing now (directory, which we should update)
# second parameter is a update directory which contains unpacked jar for update
# third parameter is a boolean flag, which indicates desktop mode
if  [ -d $1 ]
then
    
    unamestr=`uname`
    
      VERSION=$(cat VERSION)

    rm -rf $1/jre

    
    if [[ "$unamestr" == 'Linux' ]]; then
	curl -o jre.tar.gz https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_linux-x64_bin.tar.gz
	tar -zxvf jre.tar.gz
	mv jdk-11.0.1 jre
    fi
    
    if [[ "$unamestr" == 'Darwin' ]]; then
	curl -o https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_osx-x64_bin.tar.gz
	tar -zxvf jre.tar.gz
	mv jdk-11.0.1/Contents/Home jre
	rm -rf jdk-11.0.1
    fi
    
    rm jre.tar.gz

    if [[ "$unamestr" == 'Darwin' ]]; then
	chmod 755 $1/jre/bin/* $1/jre/lib/lib*
	chmod 755 $1/jre/lib/jspawnhelper $1/jre/lib/jli/* $1/jre/lib/lib*
    elif [[ "$unamestr" == 'Linux' ]]; then
	chmod 755 $1/jre/bin/*
    fi
    
    curl -o libs.tzr.gz https://s3.amazonaws.com/updates.apollowallet.org/libs/ApolloWallet-$VERSION-libs.tar.gz
    tar -zxvf ApolloWallet-$VERSION-libs.tar.gz
    mv ApolloWallet-$VERSION-libs lib

else
    echo Invalid input parameters $1
fi
