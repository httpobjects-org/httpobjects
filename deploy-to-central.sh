#!/bin/bash
export GPG_TTY=$(tty)  
mvn clean source:jar javadoc:jar gpg:sign install deploy -T2C
