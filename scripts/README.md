## Zillion Tools: Scripts

* [apipwd.groovy](#apipwdgroovy)
* [copydb](#copydb)
* [db-sniff.groovy](#db-sniffgroovy)
* [gh-status.groovy](#gh-statusgroovy)
* [mnd-encrypt.sh](#mnd-encryptsh)
* [sonar-export.groovy](#sonar-exportgroovy)

---

#### apipwd.groovy

Generates password hash for API user account (stored in `AUTH_USER_CREDENTIALS` table). Supports plain-text password entry via command line or interactively. Note that multiple script executions for the _same_ password will produce _different_ hashes. This is due to use of salted algorithm to improve security.

Options:

* `-h`, `--help`
        
    Show usage description.

* `-p`, `--password`
        
    Expects password to be supplied as a command line parameter.

* `-i`, `--interactive`

    User will be prompted to enter password during script execution.


Execution:
```
groovy apipwd -p Healthfleet2015
groovy apipwd -i
```

Prerequisites: [Groovy](http://www.groovy-lang.org/) (via [SdkMan](http://sdkman.io/))

---

### copydb

Copies a remote Prometheus API database into a local [Oracle DB virtual appliance](http://www.oracle.com/technetwork/database/enterprise-edition/databaseappdev-vm-161299.html). 

Options:

* `-h`
        
    Show usage description.

* `-s`
        
    Source database URL, should start with `//`.

* `-u`

    Source database username.

* `-p`

    Source database password.

* `-i`

    Data pump job identifier intended to prevent script users to step on each other toes. Per script user uniqueness should be enough, so name could be a good ID.

* `-c`

    Optional attribute for fine-grained controll. Expects command name `setup`, `export`, `move`, `import` or `cleanup`. Defaults to `all` which runs the full sequence.

Execution:
```
./copydb -s //sorbet.c2nmjbfxq71p.us-east-1.rds.amazonaws.com:1521/SORBET -u PROMETHEUS_DB_SORBET -p PrOm_SorBeT_DB -i JESSE
```

---

#### db-sniff.groovy

Executes given SQL query in specified list of Oracle databases.

The configuration file (e.g. `db-sniff.conf`) should contain a JSON object with the following structure. The example shows single database setup, but multiple entries are supported.
```
[
    {
        "name": "database name",
        "url": "db-host:db-port:db-schema",
        "user": "db-user",
        "password": "db-password"
    }
]
```
Execution:
```
groovy db-sniff.groovy -v -c ~/db-sniff.conf -q "SELECT CURRENT_TIMESTAMP FROM DUAL"
```

Prerequisites: [Groovy](http://www.groovy-lang.org/) (via [SdkMan](http://sdkman.io/))

---

#### gh-status.groovy

Sets GitHub [status](https://developer.github.com/v3/repos/statuses/) on a specified commit.

Options:

* `-h`, `--help`
        
    Show usage description.

* `-d`, `--dry`
        
    Dry run, evaluates commit reference but does not set the status.

* `-r`, `--repo`

    Repository name (without `.git` extension).

* `-f`, `--ref`

    Commit reference as `type=id`, where `type` is `pull`, `branch` or `commit`. For `pull` the `id` is a pull request number and HEAD commit will be used. For `branch` the `id` is a branch name and HEAD commit will be used. For `commit` the `id` is either full SHA hash or shorter reference (first N digits).

* `-s`, `--status`

    Status to set in format `context=state`, where `context` is pre-arranged string and `state` is one of the following: `pending`, `success`, `error`, `failure`.


* `-t`, `--token`

    GitHub [access token](https://github.com/settings/tokens) to authenticate. The script will try to read from `~/.github-token` if this option is not used. The file should only contain the plain text token, e.g.:
    ```
    b989c210ab6ba09365crtd8bwq2c938a553e003
    ```

* `-c`, `--comment`
    
    Comment to add, double-quoted. Supported for _pull request_ reference only.

Execution:
```
groovy gh-status -r prometheus -f pull=33 -s code-review=success -c ":+1: LGTM @alesbukovsky"
```

Prerequisites: [Groovy](http://www.groovy-lang.org/) (via [SdkMan](http://sdkman.io/))

---

#### mnd-encrypt.sh

Encrypts given plain string for use in MyNetDiary server configuration files.

```
mnd-encrypt.sh <plain-text> <crypter-passphrase>
```

Prerequisites: Environment variable `MND_HOME` should be set to a exploded MyNetDiary distribution (e.g. `~/mnd/exploded`).

---

#### publish-s3.gradle

Publishes given file (e.g. JAR) into internal Zillion Maven repository.

```
gradle -b publish-s3.gradle -PmvnGroupId=oracle -PmvnArtifactId=jdbc -PmvnVersion=12.1.0.2 -PmvnFile=ojdbc7.jar
```

Prerequisites: [Gradle](http://gradle.org/) (via [SdkMan](http://sdkman.io/))

---

#### sonar-export.groovy

Exports pipe-delimited list of issue for given SonarQube project to `stdout`.

```
groovy sonar-export.groovy -k v0:PROMETHEUS_SECURITY_1_4_3
```

Prerequisites: [Groovy](http://www.groovy-lang.org/) (via [SdkMan](http://sdkman.io/))
