#!/usr/bin/env groovy
@Grab(group='org.jasypt', module='jasypt', version='1.9.0')

import org.jasypt.util.password.BasicPasswordEncryptor

def error(String msg) {
    println "ERROR: ${msg}"
    System.exit(1)
}

def cli = new CliBuilder(usage:'apipwd [options]')
cli.with {
    i longOpt:'interactive', 'hash password entered interactively'
    p longOpt:'password', args:1, argName:'plain', 'hash supplied plain-text password'
    h longOpt:'help', 'display this message'
}
def opts = cli.parse(args)

if (!opts) {
    System.exit(1)
}
if ((!opts.p && !opts.i) || opts.h) {
    cli.usage()
    System.exit(opts.h ? 0 : 1)
}

def pwd = null

// command line supplied plain-text password
if (opts.p) {
    pwd = opts.p
}

// interactively supplied plain-text password
if (opts.i) {
    pwd = new String(System.console().readPassword('Type password: '))
    def pwd2 = new String(System.console().readPassword('Once again: '))
    if (pwd != pwd2) error 'Entries do not match'
} 

def enc = new BasicPasswordEncryptor()
def hash = enc.encryptPassword(pwd)

// sanity check, just to be sure
if (!enc.checkPassword(pwd, hash)) {
    error 'Something is wrong, generated hash failed sanity check'
}

// spit out verified hash
println hash
