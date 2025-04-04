package org.illumio;

import org.illumio.data.*;
import org.illumio.utils.Constants;
import org.illumio.utils.FileDownloader;
import org.illumio.utils.Utils;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FlowLogParser {

    private Map<Integer, String> protocolMap;
    private Map<PortProtocolPair, Tag> tagMap;

    private Map<Tag, Long> tagCountMap;
    private Map<PortProtocolPair, Long> portProtocolPairCountMap;

    // for tracking the time taken for each step
    private long timeCheckpoint;
    private long totalTime;

    Map<Integer, String> getProtocolMap() {
        return protocolMap;
    }

    void setProtocolMap(Map<Integer, String> protocolMap) {
        this.protocolMap = protocolMap;
    }

    Map<PortProtocolPair, Tag> getTagMap() {
        return tagMap;
    }

    void setTagMap(Map<PortProtocolPair, Tag> tagMap) {
        this.tagMap = tagMap;
    }

    Map<Tag, Long> getTagCountMap() {
        return tagCountMap;
    }

    void setTagCountMap(Map<Tag, Long> tagCountMap) {
        this.tagCountMap = tagCountMap;
    }

    Map<PortProtocolPair, Long> getPortProtocolPairCountMap() {
        return portProtocolPairCountMap;
    }

    void setPortProtocolPairCountMap(Map<PortProtocolPair, Long> portProtocolPairCountMap) {
        this.portProtocolPairCountMap = portProtocolPairCountMap;
    }

    void downloadProtocolNumbersCSV() {
        String lastUpdatedProtocolNumbers = Utils.loadTimestamp(Constants.PROTOCOL_NUMBERS_LAST_UPDATED_TIME_FILE_PATH);

        if (lastUpdatedProtocolNumbers == null) {
            System.out.println("No stored timestamp found. So fetching Protocol Numbers from remote call.");
        }

        if (lastUpdatedProtocolNumbers == null || Utils.isOlderThanAWeek(lastUpdatedProtocolNumbers)) {
            boolean isSuccess = FileDownloader.download(Constants.PROTOCOL_NUMBERS_CSV_DOWNLOAD_URL, Constants.PROTOCOL_NUMBERS_FILE_PATH);
            if (isSuccess) {
                Utils.persistTimestamp(Constants.PROTOCOL_NUMBERS_LAST_UPDATED_TIME_FILE_PATH);
                postTimeTaken();
            }
            else {
                if (lastUpdatedProtocolNumbers != null) {
                    System.err.println("Failed to download latest protocol numbers." +
                            " Falling back to the last file downloaded at: " + lastUpdatedProtocolNumbers);
                }
                else {
                    System.err.println("Failed to download latest protocol numbers. " +
                            "Checking if any earlier downloaded file is present.");
                }
            }
        }
        else {
            System.out.println("Using the saved Protocol Numbers fetched within the past week." );
        }
    }

    void loadProtocolMap() {
        String protocolNumbersFilePath = Constants.PROTOCOL_NUMBERS_FILE_PATH;
        protocolMap = new HashMap<>(340); // 255 standard protocols (with 0.75 load factor)

        try (BufferedReader br = new BufferedReader(new FileReader(protocolNumbersFilePath))) {
            String line;
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", -1);

                if (values.length >= 2) {
                    try {
                        int protocolNumber = Integer.parseInt(values[0].trim());
                        String protocolType = values[1].trim().toLowerCase(Locale.ROOT);

                        protocolMap.put(protocolNumber, protocolType);
                    } catch (NumberFormatException e) {
                        // Invalid protocol number format. Skipping line.
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the protocol numbers csv file: " + e.getMessage());
            System.exit(1);
        }
    }

    void loadTagLookup(String tagLookupFilePath) {
        try (BufferedReader brCount = new BufferedReader(new FileReader(tagLookupFilePath))) {
            tagMap = new HashMap<>((int) brCount.lines().count(), 1); // setting initial capacity based on the number of tag mappings

            try (BufferedReader brRead = new BufferedReader(new FileReader(tagLookupFilePath))) {
                String line;
                brRead.readLine();

                while ((line = brRead.readLine()) != null) {
                    String[] values = line.split(",", -1);

                    if (values.length >= 3) {
                        try {
                            int destinationPort = Integer.parseInt(values[0].trim());
                            String protocol = values[1].trim().toLowerCase(Locale.ROOT);
                            Tag tag = new Tag(values[2].trim());

                            tagMap.put(new PortProtocolPair(destinationPort, protocol), tag);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid destination port format: " + values[0] + ". Skipping line.");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the tag lookup csv file: " + e.getMessage());
            System.exit(1);
        }
    }

    void processFlowLog(FlowLog flowLog) {
        int destinationPort = flowLog.getDestinationPort();
        String protocol = protocolMap.getOrDefault(flowLog.getProtocol(), "");

        if (protocol.isEmpty()) {
            System.err.println("Invalid protocol: " + flowLog.getProtocol() + ". Skipping log.");
            return;
        }

        PortProtocolPair portProtocolPair = new PortProtocolPair(destinationPort, protocol);

        Tag tag = tagMap.getOrDefault(portProtocolPair, new Tag("Untagged"));
        tagCountMap.put(tag, tagCountMap.getOrDefault(tag, 0L) + 1);

        portProtocolPairCountMap.put(portProtocolPair, portProtocolPairCountMap.getOrDefault(portProtocolPair, 0L) + 1);
    }

    boolean persistProcessedOutput(String outputFilePath){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("Tag Counts:\n");
            if (tagCountMap.isEmpty()) {
                writer.write("No matching tags found.\n");
            }
            else {
                writer.write("Tag,Count\n");
                for (Map.Entry<Tag, Long> entry : tagCountMap.entrySet()) {
                    if (entry.getKey().original().equalsIgnoreCase("Untagged")) {
                        continue; // to ensure Untagged is listed at the end
                    }
                    writer.write(entry.getKey().original() + "," + entry.getValue() + "\n");
                }
                if (tagCountMap.getOrDefault(new Tag("Untagged"), 0L) > 0) {
                    writer.write("Untagged," + tagCountMap.get(new Tag("Untagged")) + "\n");
                }
            }

            writer.write("\n");

            writer.write("Port/Protocol Combination Counts:\n");
            writer.write("Port,Protocol,Count\n");
            for (Map.Entry<PortProtocolPair, Long> entry : portProtocolPairCountMap.entrySet()) {
                PortProtocolPair pair = entry.getKey();
                writer.write(pair.getDestinationPort() + "," + pair.getProtocol() + "," + entry.getValue() + "\n");
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error writing to output file: " + e.getMessage());
            return false;
        }
    }

    void postTimeTaken() {
        long currentTime = System.currentTimeMillis();
        long timeTaken = (currentTime - timeCheckpoint);
        System.out.println("Time taken: " + timeTaken / 1000.0 + "s");
        timeCheckpoint = currentTime;
        totalTime += timeTaken;
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Invalid arguments.\nUsage: gradle run --args=\"<flow logs file path> <tag lookup file path>\"");
            System.exit(1);
        }

        String flowLogsFilePath = args[0];
        String tagLookupFilePath = args[1];

        FlowLogParser flowLogParser = new FlowLogParser();

        flowLogParser.totalTime = 0;
        flowLogParser.timeCheckpoint = System.currentTimeMillis();

        System.out.println("\nChecking if latest Protocol Numbers need to be downloaded.");
        flowLogParser.downloadProtocolNumbersCSV();

        System.out.println("\nLoading the Protocol Number to Keyword map onto memory.");
        flowLogParser.loadProtocolMap();
        flowLogParser.postTimeTaken();

        System.out.println("\nLoading the Tag lookup table onto memory.");
        flowLogParser.loadTagLookup(tagLookupFilePath);
        flowLogParser.postTimeTaken();

        // max size of tagCountMap would be same as tagMap (if all tags are unique) + 1 for untagged
        flowLogParser.tagCountMap = new HashMap<>(flowLogParser.tagMap.size() + 1, 1);

        // flow log file can go up to 10 mb (would need max 1 resize with a load factor of 0.75)
        flowLogParser.portProtocolPairCountMap = new HashMap<>(64667);

        System.out.println("\nProcessing the data to generate the output metrics.");
        try (BufferedReader br = new BufferedReader(new FileReader(flowLogsFilePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split("\\s+");

                if (values.length >= 14) {
                    try {
                        FlowLog flowLog = new FlowLog(
                            Integer.parseInt(values[0].trim()),                 // version
                            values[1].trim(),                                   // accountId
                            values[2].trim(),                                   // eniId
                            values[3].trim(),                                   // sourceIp
                            values[4].trim(),                                   // destinationIp
                            Integer.parseInt(values[5].trim()),                 // sourcePort
                            Integer.parseInt(values[6].trim()),                 // destinationPort
                            Integer.parseInt(values[7].trim()),                 // protocol
                            Long.parseLong(values[8].trim()),                   // packets
                            Long.parseLong(values[9].trim()),                   // bytes
                            Long.parseLong(values[10].trim()),                  // startTime
                            Long.parseLong(values[11].trim()),                  // endTime
                            Action.valueOf(values[12].trim().toUpperCase()),    // action
                            LogStatus.valueOf(values[13].trim().toUpperCase())  // logStatus
                        );
                        flowLogParser.processFlowLog(flowLog);

                    }
                    catch (IllegalArgumentException e) {
                        System.err.println("Invalid data format. Skipping log.");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the flow log file: " + e.getMessage());
            System.exit(1);
        }

        if (!flowLogParser.portProtocolPairCountMap.isEmpty()) {
            boolean isSuccess = flowLogParser.persistProcessedOutput(Constants.OUTPUT_FILE_PATH);
            if (isSuccess) {
                flowLogParser.postTimeTaken();

                System.out.println("\nOutput file successfully generated at /src/main/outputs/output.txt");
                System.out.println("Total time taken: " + flowLogParser.totalTime / 1000.0 + "s");
            }
        }
        else {
            System.err.println("Failed to generate the output file. Please ensure the input files are passed correctly.");
        }
    }
}