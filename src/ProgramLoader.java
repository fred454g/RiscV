import java.io.FileInputStream;
import java.io.IOException;

/**
 * A helper class to load a binary RISC-V program
 * into the simulator's memory.
 */
public class ProgramLoader {
    /**
     * Loads a .bin file into the given memory array.
     * @param memory The simulator's memory (a byte array).
     * @param filePath The path to the .bin file to be loaded.
     * @return The number of bytes that were loaded (the program size).
     * @throws IOException If the file is not found, or if the program is too large.
     */
    public static int loadProgram(byte[] memory, String filePath)throws IOException {

        // Safely open the file using "try-with-resources"
        try (FileInputStream fis = new FileInputStream(filePath)) {
            int address = 0;
            int byteRead;

            // Read the file byte by byte and put it directly into our simulated memory
            while ((byteRead = fis.read()) != -1){
                if (address >= memory.length) {
                    // Throw an error that 'main' can catch
                    throw new IOException("The program is too big for the memory size at hand (" + memory.length + " bytes).");
                }
                memory[address] = (byte) byteRead;
                address++;
            }

            return address // Return the program's size in bytes
        }
        // 'fis' is automatically closed here
    }
    
}
