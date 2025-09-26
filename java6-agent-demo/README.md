Java 6 Compatible Used Classes Agent Demo
========================================

Build
-----

1. Build the agent:

```
bash /workspace/java6-agent-demo/build.sh
```

2. Build the demo app:

```
bash /workspace/java6-agent-demo/demo/build.sh
```

Run
---

```
java -javaagent:/workspace/java6-agent-demo/out/used-classes-agent.jar=out=/workspace/java6-agent-demo/out/used-classes.csv -jar /workspace/java6-agent-demo/out/demo-app.jar
```

Output
------

The agent writes a CSV-like text file to the path you pass with `out=` (default `~/used-classes.csv`) with two sections:

```
# jars
<jar-or-classes-folder-urls>
# classes
<fully-qualified-class-names>
```

Notes
-----

- The agent filters out classes loaded by the bootstrap loader and typical JRE locations (`rt.jar`, `jre/lib`, `classes.jar`).
- Code uses only Java 6 APIs.
