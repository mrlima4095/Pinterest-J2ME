-injars  build/standalone/Pinterest_midlet.jar
-outjars dist/Pinterest.jar

-libraryjars lib/cldc_1.1.jar
-libraryjars lib/midp_2.0.jar
-libraryjars lib/jsr75_1.0.jar

-microedition

-keep public class * extends javax.microedition.midlet.MIDlet
-keep public class pinterest.** { *; }

-dontoptimize

-repackageclasses ''
-dontusemixedcaseclassnames
-allowaccessmodification
-printmapping out.map
-dontnote
-dontwarn
