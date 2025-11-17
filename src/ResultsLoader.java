// File: ResultsLoader.java
// --- CORRECTED VERSION ---

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper class to load expected register states from a .res file.
 */
public class ResultsLoader {

    /**
     * Loads the expected register values from a .res file.
     * The file is assumed to contain 32 little-endian 32-bit integers (128 bytes total).
     *
     * @param filePath The path to the .res file.
     * @return An array of 32 integers representing the expected final register state.
     * @throws IOException If there's an error reading the file or it's the wrong size.
     */
    public static int[] loadExpectedResults(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);

        if (fileBytes.length != 128) {
            throw new IOException("Results file is not the correct size. Expected 128 bytes, found " + fileBytes.length);
        }

        int[] expectedRegs = new int[32];
        for (int i = 0; i < 32; i++) {
            int offset = i * 4;
            
            // Assemble the 4 bytes into an integer in LITTLE-ENDIAN order.
            // The byte order is reversed from the previous version to match the file format.
            // This logic now matches your ISAsim.readWord() method.
            expectedRegs[i] = ((fileBytes[offset + 3] & 0xFF) << 24) |
                              ((fileBytes[offset + 2] & 0xFF) << 16) |
                              ((fileBytes[offset + 1] & 0xFF) << 8)  |
                              ((fileBytes[offset]     & 0xFF));
        }
        return expectedRegs;
    }
}