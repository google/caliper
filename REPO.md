# Using this repo

* Current version is defined in *library/versiont.txt*

**Build a new SNAPSHOT**

    > ./gradlew spanner:build

**Installing a local SNAPSHOT**

    > ./gradlew spanner:publishToMavenLocal

**Publishing a remote SNAPSHOT**
 
   > ./gradlew spanner:build spanner:artifactoryPublish
   
**Release a new version to Bintray**

1) Will remove SNAPSHOT. Tag the commit and bump to next patch version

   > ./gradlew release 

2) Upload to bintray. TODO Find latest tag and use that as version number

   > ./gradlew bintrayUpload 
   
