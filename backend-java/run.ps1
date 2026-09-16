$env:GCLOUD_PROJECT = "internalmarketplace-d0b5e"
$env:FIREBASE_STORAGE_BUCKET = "internalmarketplace-d0b5e.firebasestorage.app"
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\Users\DhanashreeDavidasChi\Desktop\internal-main\backend-java\adminsdk.json"

$mvnHome = "C:\Users\DhanashreeDavidasChi\.m2\wrapper\dists\apache-maven-3.9.14\ed7edd442f634ac1c1ef5ba2b61b6d690b5221091f1a8e1123f5fadcc967520d"
& "$mvnHome\bin\mvn.cmd" spring-boot:run
