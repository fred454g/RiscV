# RiscV simulator
Instruction set simulator for RV32I programs, implemented through Java.

## Classes

### ISAsim
Main class, identifies the operations necessary to follow the input instructions.

### DecodedInstruction
Identifies the instruction type from Opcode and stores the values from the 32-bit instruction.

### ProgramLoader
Loads a binary (.bin) file into the virtual memory in ISAsim, for the Program Counter to cycle though and ISAsim to interpret.

### ResultsLoader
Used for testing, loads a .res file in Big-endian format and converts it to little-endian for comparrison with the register-array.