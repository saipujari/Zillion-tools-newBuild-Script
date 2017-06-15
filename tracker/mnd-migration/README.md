## Zillion Tools: MyNetDiary Migration

This utility migrates the data from MyNetDiary to new (Validic-based) tracker facility.

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
| `{env}_MND_DB_HOST`     | `10.167.87.10` |
| `{env}_MND_DB_NAME`     | `minderdb` |
| `{env}_MND_DB_USER`     | `minderapp` |
| `{env}_MND_DB_PASSWORD` | `minderpwd` |

Execute from `./target` sub-directory.

```
java -jar MigrateMNDUtil.jar {env}
```