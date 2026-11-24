#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes build/testes
find src -name '*.java' > build/fontes.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d build/classes @build/fontes.txt
java -m jdk.jartool/sun.tools.jar.Main --create --file EstoqueFacil.jar --main-class br.edu.estoquefacil.Main -C build/classes .
find testes -name '*.java' > build/testes.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -cp build/classes -d build/testes @build/testes.txt
java -Djava.awt.headless=true -cp build/classes:build/testes br.edu.estoquefacil.Testes evidencias/resultados_testes.json
