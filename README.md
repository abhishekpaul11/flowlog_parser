# Flowlog Parser

The project parses a [file](/src/main/resources/flow_logs.txt) containing [flow logs](https://docs.aws.amazon.com/vpc/latest/userguide/flow-log-records.html), and maps each log to a given tag (based on the [tag lookup table](src/main/resources/tag_lookup.csv) provided).
It then generates an [output file](src/main/outputs/output.txt) which contains the count for each tag type and the number of logs for each destination port and protocol combination.

## Installation 

The project is designed in [Java](https://www.java.com/en/) with the [Gradle](https://gradle.org/) build tool. Follow these steps to install the necessary components to run the project.

### Install Java

1. Download Java from [Oracle's official site](https://www.oracle.com/java/technologies/downloads/) and install it.
2. Add Java to `PATH` (if not done automatically). <br>
   Windows: 

    ```bash
    setx JAVA_HOME "C:\Program Files\Java\<your-java-version>"
    setx PATH "%JAVA_HOME%\bin;%PATH%"
    ```
   MacOS and Linux:

    ```bash
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    export PATH=$JAVA_HOME/bin:$PATH
    ```
3. Verify the installation
    ```bash
    java -version
    ```
   
### Install Gradle (Java Build Automation Tool)

1. Download Gradle from [Gradle's official site](https://gradle.org/install/) and install it.
2. Add Gradle to `PATH` (if not done automatically). <br>
   Windows:

    ```bash
    setx GRADLE_HOME "C:\Gradle"
    setx PATH "%GRADLE_HOME%\bin;%PATH%"
    ```
   MacOS and Linux:

    ```bash
    export GRADLE_HOME=/path/to/gradle
    export PATH=$GRADLE_HOME/bin:$PATH
    ```
3. Verify the installation
    ```bash
    gradle -v
    ```
   
### Download The Repo

```bash
git clone https://github.com/abhishekpaul11/flowlog_parser
cd flowlog_parser
```

## Execution

### Clean the project

For Windows:
```bash
gradlew.bat clean
```

For MacOS and Linux:
```bash
./gradlew clean
```

### Build the project

For Windows:
```bash
gradlew.bat build
```

For MacOS and Linux:
```bash
./gradlew build
```

This builds a JAR file for the project and stores it as /build/libs/flowlog_parser-1.0-SNAPSHOT.jar

### Run the project

For Windows:
```bash
java -jar build\libs\flowlog_parser-1.0-SNAPSHOT.jar <path\to\flow-logs-file> <path\to\tag-lookup-file>
```

For MacOS and Linux:
```bash
java -jar build/libs/flowlog_parser-1.0-SNAPSHOT.jar <path/to/flow-logs-file> <path/to/tag-lookup-file>
```

Alternatively, you can bypass the build step and directly run the following command, without generating the jar file.

For Windows:
```bash
gradlew.bat run --args="<path\to\flow-logs-file> <path\to\tag-lookup-file>"
```

For MacOS and Linux:
```bash
./gradlew run --args="<path/to/flow-logs-file> <path/to/tag-lookup-file>"
```

This should ideally generate an output file with the details mentioned above and store it [here](src/main/outputs/output.txt).

## Tests

Unit tests namely, [UtilsTest](src/test/java/org/illumio/utils/UtilsTest.java) and [FlowLogParserTest](src/test/java/org/illumio/FlowLogParserTest.java) have been added to this project. Use the following command to execute the tests.

For Windows:
```bash
gradlew.bat test
```

For MacOS and Linux:
```bash
./gradlew test
```

## Features

1. To get the mapping of the protocol number from the flow log to the protocol keyword in the tag lookup table, we are fetching the [protocol_numbers.csv](/src/main/resources/protocol_numbers.csv) file
from [here](https://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml), which is regularly updated and maintained by [IANA](https://www.iana.org/). We use a BufferedStream with a 4KB buffer while fetching the [file](https://www.iana.org/assignments/protocol-numbers/protocol-numbers-1.csv). However, to avoid downloading the file everytime the project is run, we are storing the file along with the [timestamp](src/main/resources/protocol_numbers_updated_time.txt) of when it was downloaded.
We make the remote call only if the last updated time is more than a week old while continue to use the stored data if it was fetched within a week.

   Another alternative would be to use something like [Jsoup](https://jsoup.org/) and scrape the [Last Updated](https://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml#:~:text=Protocol%20Numbers-,Last%20Updated,-2025%2D01%2D08) field from this link, and then fetch the mapping file only if needed. But this would also mean we need to make a remote
call everytime the project is run. Hence, the data is refreshed once every 7 days. This is fine as we are only interested in the number to keyword mapping of the protocols, the likelihood of which being changed is very rare.

2. We read the [flow_logs.txt](/src/main/resources/flow_logs.txt) file and parse every row as a [FlowLog](/src/main/java/org/illumio/data/FlowLog.java) data class.
3. We use the [LogStatus](/src/main/java/org/illumio/data/LogStatus.java) and [Action](/src/main/java/org/illumio/data/Action.java) enums for those fields in a Flow Log.
4. We are using the [PortProtocolPair](/src/main/java/org/illumio/data/PortProtocolPair.java) data class to capture a port and protocol combination. We have overridden equals() and hashCode() methods, to ensure two different objects with same port and protocol fields are considered equal for mapping count purposes as well as put into the same hash bucket when using a hashMap.
5. We are using the [Tag](/src/main/java/org/illumio/data/Tag.java) record class to identify a specific tag. It has an original field which captures the actual tag text (prior to making it lowercase) and uses that for the output. Here too, the equals() and the hashCode() methods are overridden to ensure two Tags are matched **case insensitively**.
6. In the [FlowLogParser](/src/main/java/org/illumio/FlowLogParser.java) class, we use the [protocolMap](https://github.com/abhishekpaul11/flowlog_parser/blob/c3f49a39e863fab945111f7d1662436f83eba4a8/src/main/java/org/illumio/FlowLogParser.java#L87C9-L87C92) to store the protocol number to keyword mapping. It has an initial capacity of 340 (255 standard protocols * 0.75 load factor) to ensure there's no resizing and hence no delays.
7. We use the [tagMap](https://github.com/abhishekpaul11/flowlog_parser/blob/c3f49a39e863fab945111f7d1662436f83eba4a8/src/main/java/org/illumio/FlowLogParser.java#L115C13-L115C134) to store the port/protocol combination to tag mapping from the tag lookup table. It has an initial capacity of the number of rows in the lookup table with a load factor of 1 to ensure there's no resizing and hence no delays. It can safely and efficiently handle upto 10,000 mappings as mentioned in the specs.
8. We use the [tagCountMap](https://github.com/abhishekpaul11/flowlog_parser/blob/c3f49a39e863fab945111f7d1662436f83eba4a8/src/main/java/org/illumio/FlowLogParser.java#L229C9-L229C87) to store the count for each tag. It's initial capacity is one more than that of tagMap (max size of tagCountMap can be same as tagMap, if all tags are unique and present + 1 for "Untagged") with a load factor of 1, to ensure there's no resizing and hence no delays.
9. We use the [portProtocolPairCountMap](https://github.com/abhishekpaul11/flowlog_parser/blob/c3f49a39e863fab945111f7d1662436f83eba4a8/src/main/java/org/illumio/FlowLogParser.java#L232C9-L232C71) to store the count for each port/protocol combination. It has an initial capacity of 64667. We arrive at this number by estimating the size of each log from the data type of its individual elements, which comes to around 108 bytes per flow log.
It's given in the spec sheet the flow log file can go upto 10 mb, hence we can have a maximum of around 97,000 flow logs. But reserving this much everytime would lead to memory wastage as not every flow log file would be of 10 mb. Hence, an initial capacity of 64667 is a good middle ground which ensures that at most there's only 1 resize of the hashMap and not much delay. We picked this particular number by assuming the average flow log file size to be 5 mb and a load factor of 0.75.

## Assumptions

1. The IANA Protocol Numbers are not changed frequently and hence fetching the latest csv file upon every run is not critical.
2. The project can only handle version 2 of the flow logs.
3. The project can only handle the default flow log format and not custom.

## Analysis

To get an idea of how well-performant the code is, timestamps have been added at different checkpoints along the flow. This would give us an idea
of how much time each step of the process is taking and identify the bottlenecks (if any).

A sample output of the project is given below:

```text
Checking if latest Protocol Numbers need to be downloaded.
Error loading timestamp: src/main/resources/protocol_numbers_updated_time.txt (No such file or directory)
No stored timestamp found. So fetching Protocol Numbers from remote call.
File downloaded successfully.
Timestamp persisted successfully.
Time taken: 0.659s

Loading the Protocol Number to Keyword map onto memory.
Time taken: 0.005s

Loading the Tag lookup table onto memory.
Time taken: 0.001s

Processing the data to generate the output metrics.
Time taken: 0.009s

Output file successfully generated at /src/main/outputs/output.txt
Total time taken: 0.674s
```
