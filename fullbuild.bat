@echo off

echo --- compiling ---
del V.class
"c:\Program Files\Java\jdk1.5.0_10\bin\javac.exe" -deprecation V.java

echo --- making jar file with 7z ---
del V.old
ren V.jar V.old
"C:\Program Files\7-Zip\7z" a -tzip -mx=9 V.jar V.class META-INF/MANIFEST.MF

echo --- shrink jar with proguard ---
del P.old
ren P.jar P.old
java -jar proguard.jar @vr4k.pro

dir P.*

echo --- press key to continue ---
pause