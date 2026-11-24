@echo off
setlocal
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
if not exist build\testes mkdir build\testes
(for %%F in (src\br\edu\estoquefacil\*.java) do @echo src/br/edu/estoquefacil/%%~nxF) > build\fontes.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d build\classes @build\fontes.txt
if errorlevel 1 goto erro
java -m jdk.jartool/sun.tools.jar.Main --create --file EstoqueFacil.jar --main-class br.edu.estoquefacil.Main -C build\classes .
if errorlevel 1 goto erro
(for %%F in (testes\br\edu\estoquefacil\*.java) do @echo testes/br/edu/estoquefacil/%%~nxF) > build\testes.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 17 -cp build\classes -d build\testes @build\testes.txt
if errorlevel 1 goto erro
java -Djava.awt.headless=true -cp "build\classes;build\testes" br.edu.estoquefacil.Testes evidencias\resultados_testes.json
if errorlevel 1 goto erro
echo Compilacao e testes concluidos.
pause
exit /b 0
:erro
echo Falha. Confira se o JDK 17 ou superior esta instalado e configurado no PATH.
pause
exit /b 1
