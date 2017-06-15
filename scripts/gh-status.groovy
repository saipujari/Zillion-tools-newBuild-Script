#!/usr/bin/env groovy
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.2')

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.TEXT

def error(String msg) {
    println "ERROR: ${msg}"
    System.exit(1)
}

// allowed github status states
def states = ['pending', 'success', 'error', 'failure']

def cli = new CliBuilder(usage:'groovy ghs.groovy [options]')
cli.with {
    c longOpt:'comment', args:1, argName:'text', required:false, 'comment to add (pull request only)'
    d longOpt:'dry', required:false, 'dry run, no status is set'
    f longOpt:'ref', args:2, argName:'type=id', valueSeparator:'=', required:true, 'artifact reference\ntype: pull, branch, commit'
    h longOpt:'help', 'display this message'
    r longOpt:'repo', args:1, argName:'repo', required:true, 'repository name'
    s longOpt:'status', args:2, argName:'context=state', valueSeparator:'=', required:true, "status to set\nstate: ${states.join(', ')}"
    t longOpt:'token', args:1, argName:'token', required:false, 'access token, or picks ~/.github-token'
}
def opts = cli.parse(args)

if (!opts) {
    System.exit(1)
}
if (opts.h) {
    cli.usage()
    System.exit(0)
}

// validate status state, github only supports few
if (!opts.ss || opts.ss.size() != 2) {
    error "Status should be given as context=state"
}
if ( !(opts.ss[1] in states) ) {
    error "Unsupported status state [${opts.ss[1]}]"
}

// validate comment, only allowed for pull request reference
if (opts.c && opts.fs[0] != 'pull') {
    error "comment is only allowed for pull request reference"
} 

// figure what access token to use
def token = opts.t
if (!token) {
    def file = new File("${System.getProperty('user.home')}/.github-token")
    try {
        token = file.text.trim()
    } catch (Throwable ex) {
        error "Unable to read local token [${ex.message}]"
    }
}

def http = new HTTPBuilder('https://api.github.com')
http.headers['User-Agent'] = 'zillion-group'
http.headers['Authorization'] = "token ${token}"

// figure the target commit hash based on passed reference
def hash
switch (opts.fs[0]) {

    // lookup pull request with given number and pick the head commit
    case 'pull': 
        http.request(GET, JSON) { req ->
            uri.path = "/repos/ZillionGroup/${opts.r}/pulls/${opts.fs[1]}"
            response.success = { resp, data ->
                hash = data?.head?.sha
            }
            response.failure = { resp ->
                error "Pull request HEAD lookup failed [id:${opts.fs[1]}, ${resp.statusLine}]"
            }
        }
        break

    // lookup the head commit on a given branch    
    case 'branch': 
        http.request(GET, JSON) { req ->
            uri.path = "/repos/ZillionGroup/${opts.r}/git/refs/heads/${opts.fs[1]}"
            response.success = { resp, data ->
                if (data?.object?.type == 'commit') {
                    hash = data?.object?.sha
                } else {
                    error "Branch head is not a commit [ref:${opts.fs[1]}]"
                }
            }
            response.failure = { resp ->
                error "Branch head lookup failed [ref:${opts.fs[1]}, ${resp.statusLine}]"
            }
        }
        break

    // commit hash is 40 characters, if less given treat is a reference and lookup full hash
    case 'commit': 
        if (opts.fs[1].size() < 40) {
            http.request(GET, TEXT) { req ->
                uri.path = "/repos/ZillionGroup/${opts.r}/commits/${opts.fs[1]}"
                headers.Accept = 'application/vnd.github.v3.sha'
                response.success = { resp, data ->
                    hash = data.text.trim()
                }
                response.failure = { resp ->
                    error "Commit reference lookup failed [ref:${opts.fs[1]}, ${resp.statusLine}]"
                }
            }
        } else {
            hash = opts.fs[1]
        }    
        break
    
    // unsupported reference type given
    default:
        error "Unknown artifact type [${opts.fs[0]}]"
}

if (!hash) {
    error "Unable to obtains commit hash [type:${opts.fs[0]}, id:${opts.fs[1]}]"
}

println "Commit:  ${hash}"
println "Context: ${opts.ss[0]}"
println "State:   ${opts.ss[1]}"

if (!opts.d) {
    // set specified status on evaluated commit
    http.request(POST, JSON) { req ->
        uri.path = "/repos/ZillionGroup/${opts.r}/statuses/${hash}"
        body = [ context: opts.ss[0], state: opts.ss[1] ]
        response.'201' = { resp ->
            println "Status set successfully"
        }
        response.success = { resp ->
            error "Unexpected OK response while setting status [${resp.statusLine}]"
        }
        response.failure = { resp ->
            error "Unable to set status [${resp.statusLine}]"
        }
    }

    // add comment if specified, check reference type just to be sure 
    if (opts.c && opts.fs[0] == 'pull') {
        http.request(POST, JSON) { req ->
            uri.path = "/repos/ZillionGroup/${opts.r}/issues/${opts.fs[1]}/comments"
            body = [ body: opts.c ]
            response.'201' = { resp ->
                println "Comment added"
            }
            response.success = { resp ->
                error "Unexpected OK response adding comment [${resp.statusLine}]"
            }
            response.failure = { resp ->
                error "Unable to add comment [${resp.statusLine}]"
            }
        }
    }

} else {
    println "Dry run only, nothing submitted"
}

