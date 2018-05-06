@echo off

echo --- shrink jar with proguard ---
del P.old
ren P.jar P.old
java -jar proguard.jar @vr4k.pro

echo --- press key to continue ---
pause