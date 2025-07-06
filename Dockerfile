FROM openjdk:8-jdk

# Set environment variables
ENV HADOOP_VERSION=3.4.1
ENV HADOOP_HOME=/opt/hadoop-$HADOOP_VERSION
ENV PATH=$PATH:$HADOOP_HOME/bin

# Copy and extract Hadoop
COPY hadoop-3.4.1.tar.gz /opt/
RUN tar -xzf /opt/hadoop-3.4.1.tar.gz -C /opt/ && \
    rm /opt/hadoop-3.4.1.tar.gz

RUN ln -s /opt/hadoop-$HADOOP_VERSION /opt/hadoop

# Copy your Hadoop config files (optional, if you have custom ones)
COPY core-site.xml hdfs-site.xml $HADOOP_HOME/etc/hadoop/

# Copy Java source and input files
COPY materials_Hadoop/TPIntroHadoop/src/Question0_0.java /job/Question0_0.java
COPY materials_Hadoop/TPIntroHadoop/src/StringAndInt.java /job/StringAndInt.java
COPY materials_Hadoop/TPIntroHadoop/src/Country.java /job/Country.java
COPY materials_Hadoop/TPIntroHadoop/src/FlickrTopTags.java /job/FlickrTopTags.java
COPY materials_Hadoop/TPIntroHadoop/src/FlickrTopTagsWithCombiner.java /job/FlickrTopTagsWithCombiner.java
COPY materials_Hadoop/TPIntroHadoop/bonjour.txt /job/bonjour.txt
COPY materials_Hadoop/TPIntroHadoop/flickrSample.txt /job/flickrSample.txt

WORKDIR /job

# Compile Java source and create JARs
RUN javac -cp "$HADOOP_HOME/share/hadoop/common/*:$HADOOP_HOME/share/hadoop/mapreduce/*:$HADOOP_HOME/share/hadoop/common/lib/*:$HADOOP_HOME/share/hadoop/mapreduce/lib/*" Question0_0.java && \
    jar cf Question0_0.jar Question0_0*.class

# Compile StringAndInt and Country first
RUN javac -cp "$HADOOP_HOME/share/hadoop/common/*:$HADOOP_HOME/share/hadoop/mapreduce/*:$HADOOP_HOME/share/hadoop/common/lib/*:$HADOOP_HOME/share/hadoop/mapreduce/lib/*" StringAndInt.java Country.java

# Then compile FlickrTopTags programs
RUN javac -cp "$HADOOP_HOME/share/hadoop/common/*:$HADOOP_HOME/share/hadoop/mapreduce/*:$HADOOP_HOME/share/hadoop/common/lib/*:$HADOOP_HOME/share/hadoop/mapreduce/lib/*:.:Country.class:StringAndInt.class" FlickrTopTags.java && \
    jar cf FlickrTopTags.jar FlickrTopTags*.class StringAndInt.class Country.class

RUN javac -cp "$HADOOP_HOME/share/hadoop/common/*:$HADOOP_HOME/share/hadoop/mapreduce/*:$HADOOP_HOME/share/hadoop/common/lib/*:$HADOOP_HOME/share/hadoop/mapreduce/lib/*:.:Country.class:StringAndInt.class" FlickrTopTagsWithCombiner.java && \
    jar cf FlickrTopTagsWithCombiner.jar FlickrTopTagsWithCombiner*.class StringAndInt.class Country.class

COPY materials_Hadoop/miserables /job/miserables

# Upload flickr sample data to HDFS  
COPY materials_Hadoop/TPIntroHadoop/flickrSample.txt /job/flickrSample.txt

# Automatically upload bonjour.txt to HDFS if NameNode is running
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]