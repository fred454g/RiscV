/**
 * A class to decode a 32-bit RISC-V instruction.
 * It identifies the instruction format (R, I, S, B, U, J) and extracts
 * all relevant fields into format-specific nested records.
 */
public class DecodedInstruction {

    /**
     * Enum to represent the instruction format type.
     */
    public enum InstructionType {
        R, I, S, B, U, J, UNKNOWN
    }

    // --- Nested Records for each format ---
    // Each record is a self-contained data structure for a specific instruction format.
    // The constructor of each record is responsible for decoding its own fields from the raw instruction.

    /**
     * Decodes R-type (Register-Register) instructions.
     * Used for operations like ADD, SUB, SLT.
     */
    public record R(int funct7, int rs2, int rs1, int funct3, int rd) {
        public R(int instr) {
            this(
                (instr >> 25) & 0x7F,
                (instr >> 20) & 0x1F,
                (instr >> 15) & 0x1F,
                (instr >> 12) & 0x7,
                (instr >> 7)  & 0x1F
            );
        }
    }

    /**
     * Decodes I-type (Register-Immediate) instructions.
     * Used for operations like ADDI, LW, JALR.
     */
    public record I(int immediate, int rs1, int funct3, int rd) {
        public I(int instr) {
            this(
                instr >> 20, // Immediate is sign-extended automatically by the '>>' operator
                (instr >> 15) & 0x1F,
                (instr >> 12) & 0x7,
                (instr >> 7)  & 0x1F
            );
        }
    }

    /**
     * Decodes S-type (Store) instructions.
     * Used for operations like SW, SH, SB.
     */
    public record S(int immediate, int rs2, int rs1, int funct3) {
        public S(int instr) {
            this(calculateImmediate(instr), (instr >> 20) & 0x1F, (instr >> 15) & 0x1F, (instr >> 12) & 0x7);
        }

        private static int calculateImmediate(int instr) {
            // Re-assemble the S-type immediate from its two parts
            int imm11_5 = (instr >> 25) & 0x7F;
            int imm4_0  = (instr >> 7)  & 0x1F;
            int imm = (imm11_5 << 5) | imm4_0;
            // Manually sign-extend the 12-bit immediate to 32 bits
            return (imm << 20) >> 20;
        }
    }

    /**
     * Decodes B-type (Branch) instructions.
     * Used for operations like BEQ, BNE.
     */
    public record B(int signedOffset, int rs2, int rs1, int funct3) {
        public B(int instr) {
            this(calculateSignedOffset(instr), (instr >> 20) & 0x1F, (instr >> 15) & 0x1F, (instr >> 12) & 0x7);
        }

        private static int calculateSignedOffset(int instr) {
            int imm12   = (instr >> 31) & 1;
            int imm11   = (instr >> 7)  & 1;
            int imm10_5 = (instr >> 25) & 0x3F;
            int imm4_1  = (instr >> 8)  & 0xF;
            int offset = (imm12 << 12) | (imm11 << 11) | (imm10_5 << 5) | (imm4_1 << 1);
            // Sign-extend the 13-bit offset to a 32-bit integer
            return (offset << 19) >> 19;
        }
    }

    /**
     * Decodes U-type (Upper Immediate) instructions.
     * Used for LUI and AUIPC.
     */
    public record U(int immediate, int rd) {
        public U(int instr) {
            this(
                instr & 0xFFFFF000, // The immediate is the upper 20 bits, lower 12 are 0
                (instr >> 7) & 0x1F
            );
        }
    }

    /**
     * Decodes J-type (Jump) instructions.
     * Used for JAL.
     */
    public record J(int signedOffset, int rd) {
        public J(int instr) {
            this(calculateSignedOffset(instr), (instr >> 7) & 0x1F);
        }

        private static int calculateSignedOffset(int instr) {
            int imm20   = (instr >> 31) & 1;
            int imm10_1 = (instr >> 21) & 0x3FF;
            int imm11   = (instr >> 20) & 1;
            int imm19_12= (instr >> 12) & 0xFF;
            int offset = (imm20 << 20) | (imm19_12 << 12) | (imm11 << 11) | (imm10_1 << 1);
            // Sign-extend the 21-bit offset to a 32-bit integer
            return (offset << 11) >> 11;
        }
    }


    // --- Main Class Fields ---
    public final int instruction;
    public final int opcode;
    public final InstructionType type;

    // These will hold the format-specific data. Only one will be non-null.
    public final R r;
    public final I i;
    public final S s;
    public final B b;
    public final U u;
    public final J j;

    /**
     * The constructor acts as the main decoder.
     * It figures out the type based on the opcode and populates the correct nested record.
     */
    public DecodedInstruction(int instruction) {
        this.instruction = instruction;
        this.opcode = instruction & 0x7F;

        // Determine the type and instantiate the correct record
        switch (this.opcode) {
            case 0x33: // R-type (e.g., ADD, SUB, XOR)
                this.type = InstructionType.R;
                this.r = new R(instruction);
                this.i = null; this.s = null; this.b = null; this.u = null; this.j = null;
                break;
            
            case 0x13: // I-type (e.g., ADDI, SLTI)
            case 0x03: // I-type (e.g., LW, LB)
            case 0x67: // I-type (JALR)
            case 0x73: // I-type (ECALL, EBREAK)
                this.type = InstructionType.I;
                this.i = new I(instruction);
                this.r = null; this.s = null; this.b = null; this.u = null; this.j = null;
                break;

            case 0x23: // S-type (e.g., SW, SB)
                this.type = InstructionType.S;
                this.s = new S(instruction);
                this.r = null; this.i = null; this.b = null; this.u = null; this.j = null;
                break;

            case 0x63: // B-type (e.g., BEQ, BNE)
                this.type = InstructionType.B;
                this.b = new B(instruction);
                this.r = null; this.i = null; this.s = null; this.u = null; this.j = null;
                break;

            case 0x37: // U-type (LUI)
            case 0x17: // U-type (AUIPC)
                this.type = InstructionType.U;
                this.u = new U(instruction);
                this.r = null; this.i = null; this.s = null; this.b = null; this.j = null;
                break;

            case 0x6F: // J-type (JAL)
                this.type = InstructionType.J;
                this.j = new J(instruction);
                this.r = null; this.i = null; this.s = null; this.b = null; this.u = null;
                break;

            default:
                this.type = InstructionType.UNKNOWN;
                this.r = null; this.i = null; this.s = null; this.b = null; this.u = null; this.j = null;
                break;
        }
    }
}