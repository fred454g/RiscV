import java.util.*;
import java.io.IOException;
import java.util.Scanner;

public class ISAsim {
    
    static int pc;
    static int reg[] = new int[32];
    static byte[] memory = new byte[1024 * 1024]; // 1 MB of simulated RAM

    // --- Program State ---
    static int programSizeInBytes = 0; // The total size of the loaded program in bytes. Used for bounds checking.

    /**
     * Main entry point for the simulator.
     * Loads a binary program into memory and starts the simulation loop.
     * @param args Command line arguments, expects the path to the .bin file at index 0.
     */
    public static void main(String[] args) {
        
        // --- PROGRAM LOADING ---
        Scanner scanner = new Scanner(System.in); // Create a new scanner to read from the console
        System.out.println("Enter the path to the .bin file (e.g., tests/task1/addi.bin):");
        String binFile = scanner.nextLine(); // Read the file path from the user

        try {
            // Attempt to load the program from the .bin file using the ProgramLoader helper
            programSizeInBytes = ProgramLoader.loadProgram(memory, binFile);
            System.out.println("Loaded " + programSizeInBytes + " bytes from " + binFile);

        } catch (IOException e) {
            // Handle errors (e.g., file not found, program too large)
            System.out.println("Error loading file: " + e.getMessage());
            scanner.close(); // Close the scanner on error
            return;
        }

        scanner.close(); // Close the scanner after we're done with it

        // --- SIMULATION START ---
        System.out.println("Starting simulation!");

        pc = 0;                    // Set program Counter to the start of the program
        Arrays.fill(reg, 0); // Initiate all registers to 0
        reg[2] = memory.length; // Initialize Stack Pointer (x2) to the end of memory
        
        // --- SIMULATION LOOP ---
        boolean simulation_running = true; // ECALL can stop the simulation

        while (simulation_running) {

            reg[0] = 0; // Ensure x0 is always 0.

            if (pc >= programSizeInBytes) {
                System.out.println("PC out of bounds. Halting.");
                break;
            }

            int instr = readWord(pc);
            DecodedInstruction decoded = new DecodedInstruction(instr);

            // A flag to check if the PC was changed by a branch or jump.
            boolean pc_changed = false;

            switch (decoded.type) {
                case R:
                    switch (decoded.r.funct3()) {
                        case 0x0: // ADD or SUB
                            if (decoded.r.funct7() == 0x00) { // ADD
                                reg[decoded.r.rd()] = reg[decoded.r.rs1()] + reg[decoded.r.rs2()];
                            } else if (decoded.r.funct7() == 0x20) { // SUB
                                reg[decoded.r.rd()] = reg[decoded.r.rs1()] - reg[decoded.r.rs2()];
                            } else {
                                System.out.println("Unknown R-type with funct3=0 and funct7=" + decoded.r.funct7());
                            }
                            break;
                        
                        case 0x1: // SLL (Shift Left Logical)
                            reg[decoded.r.rd()] = reg[decoded.r.rs1()] << (reg[decoded.r.rs2()] & 0x1F);
                            break;
                        
                        case 0x2: // SLT (Set Less Than)
                            reg[decoded.r.rd()] = (reg[decoded.r.rs1()] < reg[decoded.r.rs2()]) ? 1 : 0;
                            break;

                        case 0x3: // SLTU (Set Less Than Unsigned)
                            reg[decoded.r.rd()] = (Integer.compareUnsigned(reg[decoded.r.rs1()], reg[decoded.r.rs2()]) < 0) ? 1 : 0;
                            break;
                        
                        case 0x4: // XOR
                            reg[decoded.r.rd()] = reg[decoded.r.rs1()] ^ reg[decoded.r.rs2()];
                            break;
                        
                        case 0x5: // SRL or SRA
                            int shiftAmount = reg[decoded.r.rs2()] & 0x1F;
                            if (decoded.r.funct7() == 0x00) { // SRL (Shift Right Logical)
                                reg[decoded.r.rd()] = reg[decoded.r.rs1()] >>> shiftAmount;
                            } else if (decoded.r.funct7() == 0x20) { // SRA (Shift Right Arithmetic)
                                reg[decoded.r.rd()] = reg[decoded.r.rs1()] >> shiftAmount;
                            } else {
                                System.out.println("Unknown R-type with funct3=5 and funct7=" + decoded.r.funct7());
                            }
                            break;
                        
                        case 0x6: // OR
                            reg[decoded.r.rd()] = reg[decoded.r.rs1()] | reg[decoded.r.rs2()];
                            break;

                        case 0x7: // AND
                            reg[decoded.r.rd()] = reg[decoded.r.rs1()] & reg[decoded.r.rs2()];
                            break;

                        default:
                            System.out.println("Unknown R-type with funct3=" + decoded.r.funct3());
                            break;
                    }
                    break;

                case I:
                    switch (decoded.opcode) {
                        case 0x13: // Immediate arithmetic
                            switch (decoded.i.funct3()) {
                                case 0x0: // ADDI
                                    reg[decoded.i.rd()] = reg[decoded.i.rs1()] + decoded.i.immediate();
                                    break;
                                case 0x1: // SLLI (Shift Left Logical Immediate)
                                    int shamt_slli = (decoded.instruction >> 20) & 0x1F;
                                    reg[decoded.i.rd()] = reg[decoded.i.rs1()] << shamt_slli;
                                    break;
                                case 0x2: // SLTI
                                    reg[decoded.i.rd()] = (reg[decoded.i.rs1()] < decoded.i.immediate()) ? 1 : 0;
                                    break;
                                case 0x3: // SLTIU
                                    reg[decoded.i.rd()] = (Integer.compareUnsigned(reg[decoded.i.rs1()], decoded.i.immediate()) < 0) ? 1 : 0;
                                    break;
                                case 0x4: // XORI
                                    reg[decoded.i.rd()] = reg[decoded.i.rs1()] ^ decoded.i.immediate();
                                    break;
                                case 0x5: // SRLI or SRAI
                                    int shamt_sr = (decoded.instruction >> 20) & 0x1F;
                                    int funct7_sr = (decoded.instruction >> 25) & 0x7F;
                                    if (funct7_sr == 0x00) { // SRLI (Shift Right Logical Immediate)
                                        reg[decoded.i.rd()] = reg[decoded.i.rs1()] >>> shamt_sr;
                                    } else if (funct7_sr == 0x20) { // SRAI (Shift Right Arithmetic Immediate)
                                        reg[decoded.i.rd()] = reg[decoded.i.rs1()] >> shamt_sr;
                                    } else {
                                        System.out.println("Unknown I-type shift with funct7=" + funct7_sr);
                                    }
                                    break;
                                case 0x6: // ORI
                                    reg[decoded.i.rd()] = reg[decoded.i.rs1()] | decoded.i.immediate();
                                    break;
                                case 0x7: // ANDI
                                    reg[decoded.i.rd()] = reg[decoded.i.rs1()] & decoded.i.immediate();
                                    break;
                                default:
                                    System.out.println("Unknown I-type (0x13) with funct3=" + decoded.i.funct3());
                                    break;
                            }
                            break;
                        case 0x03: // Load instructions
                            int mem_addr_load = reg[decoded.i.rs1()] + decoded.i.immediate();
                            switch (decoded.i.funct3()) {
                                case 0x0: // LB (Load Byte) - sign-extended
                                    reg[decoded.i.rd()] = readByte(mem_addr_load);
                                    break;
                                case 0x1: // LH (Load Halfword) - sign-extended
                                    reg[decoded.i.rd()] = readHalf(mem_addr_load);
                                    break;
                                case 0x2: // LW (Load Word)
                                    reg[decoded.i.rd()] = readWord(mem_addr_load);
                                    break;
                                case 0x4: // LBU (Load Byte Unsigned) - zero-extended
                                    reg[decoded.i.rd()] = readByte(mem_addr_load) & 0xFF;
                                    break;
                                case 0x5: // LHU (Load Halfword Unsigned) - zero-extended
                                    reg[decoded.i.rd()] = readHalf(mem_addr_load) & 0xFFFF;
                                    break;
                                default:
                                    System.out.println("Unknown Load instruction with funct3=" + decoded.i.funct3());
                                    break;
                            }
                            break;
                        case 0x67: // JALR (Jump and Link Register)
                            int target = reg[decoded.i.rs1()] + decoded.i.immediate();
                            reg[decoded.i.rd()] = pc + 4; // Store return address
                            pc = target;
                            pc_changed = true;
                            break;

                        case 0x73: // ECALL
                            // Check the service number in register a7 (x17)
                            int service = reg[17]; // a7 is x17
                            switch (service) {
                                case 1: // Service 1: Print Integer
                                    // The integer to print is in register a0 (x10)
                                    System.out.print(reg[10]);
                                    break;
                                
                                case 4: // Service 4: Print String
                                    // The address of the string is in a0 (x10)
                                    int addr = reg[10];
                                    StringBuilder sb = new StringBuilder();
                                    while (memory[addr] != 0) {
                                        sb.append((char)memory[addr]);
                                        addr++;
                                    }
                                    System.out.print(sb.toString());
                                    break;

                                case 10: // Service 10: Exit
                                    System.out.println("\n--- ECALL: Exit ---");
                                    simulation_running = false; // Set flag to terminate the loop
                                    pc_changed = true;
                                    break;
                                
                                case 93: // Service 93: Exit with code (alternative standard)
                                    System.out.println("\n--- ECALL: Exit with code " + reg[10] + " ---");
                                    simulation_running = false;
                                    pc_changed = true;
                                    break;

                                default:
                                    System.out.println("Unknown ECALL service number: " + service);
                                    break;
                            }
                            break; // End of ECALL case    
                    }
                    break;

                case S:
                    int mem_addr_store = reg[decoded.s.rs1()] + decoded.s.immediate();
                    switch (decoded.s.funct3()) {
                        case 0x0: // SB (Store Byte)
                            writeByte(mem_addr_store, reg[decoded.s.rs2()]);
                            break;
                        case 0x1: // SH (Store Halfword)
                            writeHalf(mem_addr_store, reg[decoded.s.rs2()]);
                            break;
                        case 0x2: // SW (Store Word)
                            writeWord(mem_addr_store, reg[decoded.s.rs2()]);
                            break;
                        default:
                            System.out.println("Unknown Store instruction with funct3=" + decoded.s.funct3());
                            break;
                    }
                    break;

                case B:
                    boolean conditionMet = false;
                    switch (decoded.b.funct3()) {
                        case 0x0: // BEQ (Branch if Equal)
                            conditionMet = (reg[decoded.b.rs1()] == reg[decoded.b.rs2()]);
                            break;
                        case 0x1: // BNE (Branch if Not Equal)
                            conditionMet = (reg[decoded.b.rs1()] != reg[decoded.b.rs2()]);
                            break;
                        case 0x4: // BLT (Branch if Less Than)
                            conditionMet = (reg[decoded.b.rs1()] < reg[decoded.b.rs2()]);
                            break;
                        case 0x5: // BGE (Branch if Greater Than or Equal)
                            conditionMet = (reg[decoded.b.rs1()] >= reg[decoded.b.rs2()]);
                            break;
                        case 0x6: // BLTU (Branch if Less Than, Unsigned)
                            conditionMet = (Integer.compareUnsigned(reg[decoded.b.rs1()], reg[decoded.b.rs2()]) < 0);
                            break;
                        case 0x7: // BGEU (Branch if Greater Than or Equal, Unsigned)
                            conditionMet = (Integer.compareUnsigned(reg[decoded.b.rs1()], reg[decoded.b.rs2()]) >= 0);
                            break;
                        default:
                            System.out.println("Unknown Branch instruction with funct3=" + decoded.b.funct3());
                            break;
                    }
                    if (conditionMet) {
                        pc += decoded.b.signedOffset();
                        pc_changed = true;
                    }
                    break;

                case U:
                    if (decoded.opcode == 0x37) { // LUI (Load Upper Immediate)
                        reg[decoded.u.rd()] = decoded.u.immediate();
                    } else if (decoded.opcode == 0x17) { // AUIPC (Add Upper Immediate to PC)
                        reg[decoded.u.rd()] = pc + decoded.u.immediate();
                    }
                    break;

                case J: // JAL (Jump and Link)
                    if (decoded.j.rd() != 0) { // Don't write link to x0
                        reg[decoded.j.rd()] = pc + 4; // Store return address
                    }
                    pc += decoded.j.signedOffset();
                    pc_changed = true;
                    break;

                default:
                    System.out.println("Opcode " + decoded.opcode + " not yet implemented");
                    break;
            }

            // Increment PC only if it wasn't a jump or a taken branch
            if (!pc_changed) {
                pc += 4;
            }
        }

        System.out.println("\n--- Simulation Finished ---");
        System.out.println("Final Register State (non-zero):");
        for (int i = 0; i < reg.length; ++i) {
            if (reg[i] != 0) {
                System.out.println("x" + i + ":\t" + reg[i]);
            }
        }
    }


    //================================
    // HELPER FUNCTIONS
    //================================

    /**
     * Writes a 32-bit integer to memory at a given address (Little Endian).
     */
    private static void writeWord(int address, int value) {
        memory[address]     = (byte) (value & 0xFF);
        memory[address + 1] = (byte) ((value >> 8) & 0xFF);
        memory[address + 2] = (byte) ((value >> 16) & 0xFF);
        memory[address + 3] = (byte) ((value >> 24) & 0xFF);
    }

    /**
     * Writes a 16-bit halfword to memory (Little Endian).
     */
    private static void writeHalf(int address, int value) {
        memory[address]     = (byte) (value & 0xFF);         // Lower byte
        memory[address + 1] = (byte) ((value >> 8) & 0xFF);  // Upper byte
    }

    /**
     * Writes a single byte to memory.
     */
    private static void writeByte(int address, int value) {
        memory[address] = (byte) (value & 0xFF);
    }

    /**
     * Reads a 32-bit integer from memory at a given address (Little Endian).
     */
    private static int readWord(int address) {
        return (memory[address] & 0xFF) |
            ((memory[address + 1] & 0xFF) << 8) |
            ((memory[address + 2] & 0xFF) << 16) |
            ((memory[address + 3] & 0xFF) << 24);
    }

    /**
     * Reads a signed 16-bit halfword from memory (Little Endian).
     */
    private static short readHalf(int address) {
        int lo = memory[address] & 0xFF;
        int hi = memory[address + 1] & 0xFF;
        return (short) ((hi << 8) | lo);
    }

    /**
     * Reads a signed 8-bit byte from memory.
     */
    private static byte readByte(int address) {
        return memory[address];
    }
}
