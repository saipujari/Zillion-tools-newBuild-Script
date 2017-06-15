## Zillion Tools: Tracker Data Creation

This utilility creates tracker data (e.g. weight or steps) records for specified member ID.

Build with maven:

```
mvn clean
mvn package
```

Configure database connection details in `./target/conf/env.properties`:
    
| Property                | Example |
|:------------------------|:--------|
| `{env}_ZIL_CONNECTION`  | `jdbc:oracle:thin:@//172.31.18.165:1521/RADEV` |
| `{env}_ZIL_DB_USER`     | `user` |
| `{env}_ZIL_DB_PASSWORD` | `pswd` |

Execute from `./target` sub-directory.

```
java -jar CreateDataUtil.jar {env} [steps|weight] {member-id} {num-of-months}
```