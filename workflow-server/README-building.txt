#
# Building distribution packages
#

You need Java, Apache Ant and Apache Maven 3.

The following commands create the distribution packages

# deb
 mvn package -DskipTests -Ppackman -Dpackage.type=deb -Ddistribution=Debian

# rpm 
 mvn package -DskipTests -Ppackman -Dpackage.type=rpm -Ddistribution=RedHat

# binary tar.gz
 mvn package -DskipTests -Ppackman -Dpackage.type=bin.tar.gz


