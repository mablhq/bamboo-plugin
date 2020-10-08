package com.mabl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;


public class TestUtils {

    private TestUtils() {
    }

    static String readFileAsString(String fileName) {
        final StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get("src/test/resources/__files/" + fileName), StandardCharsets.UTF_8)) {
            stream.forEach(line -> contentBuilder.append(line.trim()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    static boolean fileEquals(String expected, File actual) {
        try {
            return com.google.common.io.Files.equal(actual,
                    new File(Paths.get("src/test/resources/__files/" + expected).toString()));
        } catch (IOException e) {
            System.err.println("Caught: " + e);
            return false;
        }
    }
}
