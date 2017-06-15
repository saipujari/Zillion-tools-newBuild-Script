#!/usr/bin/env groovy

/**
 * Exports pipe-delimited list of issue for SonarQube project to stdout.
 *
 * Usage:
 *    groovy sonar-export.groovy [options]
 *
 * Options:
 *    -k, --key - SonarQube project key
 */

@Grapes([
    @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
])

import groovyx.net.http.RESTClient

def cli = new CliBuilder(usage:'groovy sonar-export.groovy [options]')
cli.with {
    k longOpt:'key', args:1, required:true, 'project key'
}
def opts = cli.parse(args)
if (!opts) System.exit(1)

def client = new RESTClient('http://ci.healthfleet.com:9000')

def page = 0
def count = 0
def total = -1

while (count != total) {
    page++
    def res = client.get(
        path: '/api/issues/search', 
        query: [ 
            componentRoots: opts.k, 
            statuses: 'OPEN', 
            sort: 'SEVERITY',
            asc: false,
            pageIndex: page, 
            pageSize: 500
        ]
    )

    if (total < 0) total = res.data.paging.total
    res.data.issues.each { issue ->
        count++
        def rule = res.data.rules.find{ it['key'] == issue.rule }
        println "${issue.severity}|${rule.name}|${issue.message}|${issue.component}|${issue.line}|${issue.key}"
    }
}
