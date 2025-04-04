package org.illumio;

import org.illumio.data.Action;
import org.illumio.data.FlowLog;
import org.illumio.data.LogStatus;
import org.illumio.data.PortProtocolPair;
import org.illumio.utils.Constants;
import org.illumio.utils.FileDownloader;
import org.illumio.utils.Utils;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FlowLogParser {

    private Map<Integer, String> protocolMap;
    private Map<PortProtocolPair, String> tagMap;

    private Map<String, Long> tagCountMap;
    private Map<PortProtocolPair, Long> portProtocolPairCountMap;

    private void downloadProtocolNumbersCSV() {
        String lastUpdatedProtocolNumbers = Utils.loadTimestamp(Constants.PROTOCOL_NUMBERS_LAST_UPDATED_TIME_FILE_PATH);

        if (lastUpdatedProtocolNumbers == null ||
                Utils.isMoreThanWeekOld(Utils.loadTimestamp(Constants.PROTOCOL_NUMBERS_LAST_UPDATED_TIME_FILE_PATH))) {
            boolean isSuccess = FileDownloader.download(Constants.PROTOCOL_NUMBERS_CSV_DOWNLOAD_URL, Constants.PROTOCOL_NUMBERS_FILE_PATH);
            if (isSuccess) {
                Utils.persistTimestamp(Constants.PROTOCOL_NUMBERS_LAST_UPDATED_TIME_FILE_PATH);
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
    }

    private void loadProtocolMap() {
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

    private void loadTagLookup(String tagLookupFilePath) {
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
                            String tag = values[2].trim();

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

    private void processFlowLog(FlowLog flowLog) {
        int destinationPort = flowLog.getDestinationPort();
        String protocol = protocolMap.getOrDefault(flowLog.getProtocol(), "");

        if (protocol.isEmpty()) {
            System.err.println("Invalid protocol: " + protocol + ". Skipping log.");
            return;
        }

        PortProtocolPair portProtocolPair = new PortProtocolPair(destinationPort, protocol);

        String tag = tagMap.getOrDefault(portProtocolPair, "Untagged");
        tagCountMap.put(tag, tagCountMap.getOrDefault(tag, 0L) + 1);

        portProtocolPairCountMap.put(portProtocolPair, portProtocolPairCountMap.getOrDefault(portProtocolPair, 0L) + 1);
    }

    private void persistProcessedOutput(String outputFilePath){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            writer.write("Tag Counts:\n");
            if (tagCountMap.isEmpty()) {
                writer.write("No matching tags found.\n");
            }
            else {
                writer.write("Tag,Count\n");
                for (Map.Entry<String, Long> entry : tagCountMap.entrySet()) {
                    writer.write(entry.getKey() + "," + entry.getValue() + "\n");
                }
            }

            writer.write("\n");

            writer.write("Port/Protocol Combination Counts:\n");
            writer.write("Port,Protocol,Count\n");
            for (Map.Entry<PortProtocolPair, Long> entry : portProtocolPairCountMap.entrySet()) {
                PortProtocolPair pair = entry.getKey();
                writer.write(pair.getDestinationPort() + "," + pair.getProtocol() + "," + entry.getValue() + "\n");
            }

        } catch (IOException e) {
            System.err.println("Error writing to output file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Invalid arguments.\nUsage: gradle run --args=\"<flow logs file path> <tag lookup file path>\"");
            System.exit(1);
        }

        String flowLogsFilePath = args[0];
        String tagLookupFilePath = args[1];

        FlowLogParser flowLogParser = new FlowLogParser();

        flowLogParser.downloadProtocolNumbersCSV();
        flowLogParser.loadProtocolMap();
        flowLogParser.loadTagLookup(tagLookupFilePath);

        // max size of tagCountMap would be same as tagMap (if all tags are unique) + 1 for untagged
        flowLogParser.tagCountMap = new HashMap<>(flowLogParser.tagMap.size() + 1, 1);

        // flow log file can go up to 10 mb (would need max 1 resize with a load factor of 0.75)
        flowLogParser.portProtocolPairCountMap = new HashMap<>(64667);

        try (BufferedReader br = new BufferedReader(new FileReader(flowLogsFilePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split("\\s+");;

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
            flowLogParser.persistProcessedOutput(Constants.OUTPUT_FILE_PATH);
        }
        else {
            System.err.println("Failed to generate the output file. Please ensure the input files are passed correctly.");
        }
    }
}