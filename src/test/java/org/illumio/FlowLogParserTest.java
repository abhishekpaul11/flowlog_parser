package org.illumio;

import org.illumio.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class FlowLogParserTest {

    private FlowLogParser parser;
    private File tempProtocolFile;
    private File tempOutputFile;

    @BeforeEach
    void setUp(@TempDir File tempDir) {
        parser = new FlowLogParser();

        tempProtocolFile = new File(tempDir, "protocol_numbers.csv");
        tempOutputFile = new File(tempDir, "output.txt");

        parser.setProtocolMap(new HashMap<>());
        parser.setTagMap(new HashMap<>());
        parser.setTagCountMap(new HashMap<>());
        parser.setPortProtocolPairCountMap(new HashMap<>());
    }

    @Test
    void testLoadProtocolMap() throws IOException {
        String csvContent = """
            Decimal,Keyword
            6,tcp
            17,udp""";
        Files.writeString(tempProtocolFile.toPath(), csvContent);

        parser.loadProtocolMap(tempProtocolFile.getAbsolutePath());

        assertEquals(2, parser.getProtocolMap().size());
        assertEquals("tcp", parser.getProtocolMap().get(6));
        assertEquals("udp", parser.getProtocolMap().get(17));
    }

    @Test
    void testLoadTagLookup(@TempDir File tempDir) throws IOException {
        File tagLookupFile = new File(tempDir, "lookup.csv");
        String csvContent = """
            dstport,protocol,tag
            80,tcp,http
            443,tcp,https""";
        Files.writeString(tagLookupFile.toPath(), csvContent);

        parser.loadTagLookup(tagLookupFile.getAbsolutePath());

        assertEquals(2, parser.getTagMap().size());
        assertEquals(new Tag("http"),
                parser.getTagMap().get(new PortProtocolPair(80, "tcp")));
        assertEquals(new Tag("https"),
                parser.getTagMap().get(new PortProtocolPair(443, "tcp")));
    }

    @Test
    void testProcessFlowLog() {
        parser.getProtocolMap().put(6, "tcp");
        parser.getTagMap().put(new PortProtocolPair(80, "tcp"), new Tag("http"));
        FlowLog flowLog = new FlowLog(2, "account", "eni", "src", "dst",
            12345, 80, 6, 10L, 1000L, 1234567890L, 1234567891L,
            Action.ACCEPT, LogStatus.OK);

        parser.processFlowLog(flowLog);

        assertEquals(1L, parser.getTagCountMap().get(new Tag("http")).longValue());
        assertEquals(1L, parser.getPortProtocolPairCountMap().get(
                new PortProtocolPair(80, "tcp")).longValue());
    }

    @Test
    void testProcessFlowLogUntagged() {
        parser.getProtocolMap().put(6, "tcp");
        FlowLog flowLog = new FlowLog(2, "account", "eni", "src", "dst",
            12345, 9999, 6, 10L, 1000L, 1234567890L, 1234567891L,
            Action.ACCEPT, LogStatus.OK);

        parser.processFlowLog(flowLog);

        assertEquals(1L, parser.getTagCountMap().get(new Tag("Untagged")).longValue());
        assertEquals(1L, parser.getPortProtocolPairCountMap().get(
                new PortProtocolPair(9999, "tcp")).longValue());
    }

    @Test
    void testPersistProcessedOutput() throws IOException {
        parser.getTagCountMap().put(new Tag("http"), 5L);
        parser.getTagCountMap().put(new Tag("Untagged"), 2L);
        parser.getPortProtocolPairCountMap().put(new PortProtocolPair(80, "tcp"), 5L);
        parser.getPortProtocolPairCountMap().put(new PortProtocolPair(9999, "tcp"), 2L);

        boolean result = parser.persistProcessedOutput(tempOutputFile.getAbsolutePath());

        assertTrue(result);
        String content = Files.readString(tempOutputFile.toPath());
        String expected = """
            Tag Counts:
            Tag,Count
            http,5
            Untagged,2
            
            Port/Protocol Combination Counts:
            Port,Protocol,Count
            80,tcp,5
            9999,tcp,2
            """;
        assertEquals(expected, content);
    }
}