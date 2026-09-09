# ojAlgo-xpress

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-xpress</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [FICO Xpress](https://www.fico.com/en/products/fico-xpress-optimization). Use Xpress as a solver from `ExpressionsBasedModel`.

## Prerequisites

1. **Xpress installed and licensed.** Download from [FICO](https://www.fico.com/en/products/fico-xpress-optimization) and activate a license.

2. **The Xpress Java API JAR in your local Maven repository.** FICO does not publish `xprs.jar` to Maven Central. Install it from your Xpress installation:

```bash
mvn install:install-file \
  -Dfile=/path/to/xpressmp/lib/xprs.jar \
  -DgroupId=com.fico.xpress -DartifactId=xprs \
  -Dversion=47.01 -Dpackaging=jar
```

Adjust the file path and version to match your installation.

3. **Native library on the library path.** When you run your program, the JVM must be able to find the Xpress native library. Pass the path to the library directory:

```
-Djava.library.path=/path/to/xpressmp/lib
```

4. **Environment variables.** Xpress requires two environment variables to be set at runtime:

| Variable | Value | Purpose |
| --- | --- | --- |
| `XPRESSDIR` | `/path/to/xpressmp` | Xpress installation directory |
| `XPAUTH_PATH` | `/path/to/xpressmp/bin/xpauth.xpr` | License file |

### Typical installation paths

| Platform | Installation directory |
| --- | --- |
| macOS | `/Applications/FICO Xpress/xpressmp` |
| Linux | `/opt/xpressmp` |
| Windows | `C:\xpressmp` |

The JAR and native libraries are in `lib/` within the installation directory. The license file is at `bin/xpauth.xpr`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverXpress.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverXpress.INTEGRATION);
```
