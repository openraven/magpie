clear
mvn clean package install && mvn --projects magpie-cli assembly:single
cd  magpie-cli/target || exit
unzip magpie-0.1.0-SNAPSHOT.zip 
cd magpie-0.1.0-SNAPSHOT/ || exit
./magpie

