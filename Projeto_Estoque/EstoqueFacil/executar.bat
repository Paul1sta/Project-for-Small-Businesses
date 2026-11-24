@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>&1
if errorlevel 1 (
 echo Java nao encontrado. Instale o Java 17 ou superior e configure o PATH.
 echo Consulte LEIA_ME.md para instrucoes.
 pause
 exit /b 1
)
java -jar EstoqueFacil.jar %*
if errorlevel 1 (
 echo Nao foi possivel iniciar. Verifique a mensagem acima e a versao do Java.
 pause
 exit /b 1
)
