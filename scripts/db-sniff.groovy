#!/usr/bin/env groovy

/**
 * Execute given SQL query in specified list of Oracle databases.
 *
 * Usage:
 *    groovy db-sniff.groovy [options]
 *
 * Options:
 *    -c, --config  - data source config file (see db-sniff.conf for template)
 *    -q, --query   - SQL query to execute
 *    -v, --verbose - enables full result set output
 */

@GrabResolver(name='zillion', root='https://zillion-maven.s3.amazonaws.com/')
@GrabConfig(systemClassLoader=true)
@Grab(group='oracle', module='oracle-jdbc', version='12.1.0.2')

import groovy.json.JsonSlurper
import groovy.sql.Sql

def cli = new CliBuilder(usage:'groovy db-sniff.groovy [options]')
cli.with {
    c longOpt:'config', args:1, argName:'file', required:true, 'data source configuration file'
    q longOpt:'query', args:1, argName:'sql', required:true, 'SQL query to execute'
    v longOpt:'verbose', args:0, required:false, 'full result set output'
}
def opts = cli.parse(args)
if (!opts) System.exit(1)

// load data source configuration 
def sources = new JsonSlurper().parse(new File(opts.c))

// calculate maximum string sizes for display padding
def sizes = ['name', 'url', 'user'].collectEntries { p ->
    [ (p) : sources.max{ it[p].length() }[p].length() ]
}

println "QUERY: ${opts.q}"

// execute query for each data source
sources.each { s ->
    ['name', 'url', 'user'].each {
        print s[it].padRight(sizes[it] + 2)
    }

    def sql
    try {
        sql = Sql.newInstance("jdbc:oracle:thin:@${s.url}", s.user, s.password, 'oracle.jdbc.driver.OracleDriver')
        def rows = sql.rows(opts.q)
        
        println rows.size
        if (opts.v) {
            rows.each { r -> 
                println r.values().join(', ') 
            }
        }

    } catch (Throwable thr) {
        println "ERROR [${thr.message.take(25)}]"        
    } finally {
        if (sql) sql.close()
    } 
}
