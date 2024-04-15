/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.net.apf;

import static android.net.apf.BaseApfGenerator.Rbit.Rbit0;
import static android.net.apf.BaseApfGenerator.Rbit.Rbit1;
import static android.net.apf.BaseApfGenerator.Register.R1;

import androidx.annotation.NonNull;

import com.android.net.module.util.HexDump;

import java.util.Objects;

/**
 * The abstract class for APFv6 assembler/generator.
 *
 * @param <Type> the generator class
 *
 * @hide
 */
public abstract class ApfV6GeneratorBase<Type extends ApfV6GeneratorBase<Type>> extends
        ApfV4GeneratorBase<Type> {

    /**
     * Creates an ApfV6GeneratorBase instance which is able to emit instructions for the specified
     * {@code version} of the APF interpreter. Throws {@code IllegalInstructionException} if
     * the requested version is unsupported.
     *
     */
    public ApfV6GeneratorBase() throws IllegalInstructionException {
        super(APF_VERSION_6);
    }

    /**
     * Add an instruction to the end of the program to increment the counter value and
     * immediately return PASS.
     *
     * @param cnt the counter number to be incremented.
     */
    public final Type addCountAndPass(int cnt) {
        checkRange("CounterNumber", cnt /* value */, 1 /* lowerBound */,
                1000 /* upperBound */);
        // PASS requires using Rbit0 because it shares opcode with DROP
        return append(new Instruction(Opcodes.PASSDROP, Rbit0).addUnsigned(cnt));
    }

    /**
     * Add an instruction to the end of the program to let the program immediately return DROP.
     */
    public final Type addDrop() {
        // DROP requires using Rbit1 because it shares opcode with PASS
        return append(new Instruction(Opcodes.PASSDROP, Rbit1));
    }

    /**
     * Add an instruction to the end of the program to increment the counter value and
     * immediately return DROP.
     *
     * @param cnt the counter number to be incremented.
     */
    public final Type addCountAndDrop(int cnt) {
        checkRange("CounterNumber", cnt /* value */, 1 /* lowerBound */,
                1000 /* upperBound */);
        // DROP requires using Rbit1 because it shares opcode with PASS
        return append(new Instruction(Opcodes.PASSDROP, Rbit1).addUnsigned(cnt));
    }

    /**
     * Add an instruction to the end of the program to call the apf_allocate_buffer() function.
     * Buffer length to be allocated is stored in register 0.
     */
    public final Type addAllocateR0() {
        return append(new Instruction(ExtendedOpcodes.ALLOCATE));
    }

    /**
     * Add an instruction to the end of the program to call the apf_allocate_buffer() function.
     *
     * @param size the buffer length to be allocated.
     */
    public final Type addAllocate(int size) {
        // Rbit1 means the extra be16 immediate is present
        return append(new Instruction(ExtendedOpcodes.ALLOCATE, Rbit1).addU16(size));
    }

    /**
     * Add an instruction to the beginning of the program to reserve the empty data region.
     */
    public final Type addData() throws IllegalInstructionException {
        return addData(new byte[0]);
    }

    /**
     * Add an instruction to the beginning of the program to reserve the data region.
     * @param data the actual data byte
     */
    public final Type addData(byte[] data) throws IllegalInstructionException {
        if (!mInstructions.isEmpty()) {
            throw new IllegalInstructionException("data instruction has to come first");
        }
        if (data.length > 65535) {
            throw new IllegalArgumentException("data size larger than 65535");
        }
        return append(new Instruction(Opcodes.JMP, Rbit1).addUnsigned(data.length)
                .setBytesImm(data).overrideImmSize(2));
    }

    /**
     * Add an instruction to the end of the program to transmit the allocated buffer without
     * checksum.
     */
    public final Type addTransmitWithoutChecksum() {
        return addTransmit(-1 /* ipOfs */);
    }

    /**
     * Add an instruction to the end of the program to transmit the allocated buffer.
     */
    public final Type addTransmit(int ipOfs) {
        if (ipOfs >= 255) {
            throw new IllegalArgumentException("IP offset of " + ipOfs + " must be < 255");
        }
        if (ipOfs == -1) ipOfs = 255;
        return append(new Instruction(ExtendedOpcodes.TRANSMIT, Rbit0).addU8(ipOfs).addU8(255));
    }

    /**
     * Add an instruction to the end of the program to transmit the allocated buffer.
     */
    public final Type addTransmitL4(int ipOfs, int csumOfs, int csumStart, int partialCsum,
                                        boolean isUdp) {
        if (ipOfs >= 255) {
            throw new IllegalArgumentException("IP offset of " + ipOfs + " must be < 255");
        }
        if (ipOfs == -1) ipOfs = 255;
        if (csumOfs >= 255) {
            throw new IllegalArgumentException("L4 checksum requires csum offset of "
                                               + csumOfs + " < 255");
        }
        return append(new Instruction(ExtendedOpcodes.TRANSMIT, isUdp ? Rbit1 : Rbit0)
                .addU8(ipOfs).addU8(csumOfs).addU8(csumStart).addU16(partialCsum));
    }

    /**
     * Add an instruction to the end of the program to write 1 byte value to output buffer.
     */
    public final Type addWriteU8(int val) {
        return append(new Instruction(Opcodes.WRITE).overrideImmSize(1).addU8(val));
    }

    /**
     * Add an instruction to the end of the program to write 2 bytes value to output buffer.
     */
    public final Type addWriteU16(int val) {
        return append(new Instruction(Opcodes.WRITE).overrideImmSize(2).addU16(val));
    }

    /**
     * Add an instruction to the end of the program to write 4 bytes value to output buffer.
     */
    public final Type addWriteU32(long val) {
        return append(new Instruction(Opcodes.WRITE).overrideImmSize(4).addU32(val));
    }

    /**
     * Add an instruction to the end of the program to encode int value as 4 bytes to output buffer.
     */
    public final Type addWrite32(int val) {
        return addWriteU32((long) val & 0xffffffffL);
    }

    /**
     * Add an instruction to the end of the program to write 4 bytes array to output buffer.
     */
    public final Type addWrite32(@NonNull byte[] bytes) {
        Objects.requireNonNull(bytes);
        if (bytes.length != 4) {
            throw new IllegalArgumentException(
                    "bytes array size must be 4, current size: " + bytes.length);
        }
        return addWrite32(((bytes[0] & 0xff) << 24)
                | ((bytes[1] & 0xff) << 16)
                | ((bytes[2] & 0xff) << 8)
                | (bytes[3] & 0xff));
    }

    /**
     * Add an instruction to the end of the program to write 1 byte value from register to output
     * buffer.
     */
    public final Type addWriteU8(Register reg) {
        return append(new Instruction(ExtendedOpcodes.EWRITE1, reg));
    }

    /**
     * Add an instruction to the end of the program to write 2 byte value from register to output
     * buffer.
     */
    public final Type addWriteU16(Register reg) {
        return append(new Instruction(ExtendedOpcodes.EWRITE2, reg));
    }

    /**
     * Add an instruction to the end of the program to write 4 byte value from register to output
     * buffer.
     */
    public final Type addWriteU32(Register reg) {
        return append(new Instruction(ExtendedOpcodes.EWRITE4, reg));
    }

    /**
     * Add an instruction to the end of the program to copy data from APF program/data region to
     * output buffer and auto-increment the output buffer pointer.
     * This method requires the {@code addData} method to be called beforehand.
     * It will first attempt to match {@code content} with existing data bytes. If not exist, then
     * append the {@code content} to the data bytes.
     */
    public final Type addDataCopy(@NonNull byte[] content) throws IllegalInstructionException {
        if (mInstructions.isEmpty()) {
            throw new IllegalInstructionException("There is no instructions");
        }
        Objects.requireNonNull(content);
        int copySrc = mInstructions.get(0).maybeUpdateBytesImm(content);
        return addDataCopy(copySrc, content.length);
    }

    /**
     * Add an instruction to the end of the program to copy data from APF program/data region to
     * output buffer and auto-increment the output buffer pointer.
     *
     * @param src the offset inside the APF program/data region for where to start copy.
     * @param len the length of bytes needed to be copied, only <= 255 bytes can be copied at
     *               one time.
     * @return the Type object
     */
    public final Type addDataCopy(int src, int len) {
        return append(new Instruction(Opcodes.PKTDATACOPY, Rbit1).addDataOffset(src).addU8(len));
    }

    /**
     * Add an instruction to the end of the program to copy data from input packet to output
     * buffer and auto-increment the output buffer pointer.
     *
     * @param src the offset inside the input packet for where to start copy.
     * @param len the length of bytes needed to be copied, only <= 255 bytes can be copied at
     *               one time.
     * @return the Type object
     */
    public final Type addPacketCopy(int src, int len) {
        return append(new Instruction(Opcodes.PKTDATACOPY, Rbit0).addPacketOffset(src).addU8(len));
    }

    /**
     * Add an instruction to the end of the program to copy data from APF program/data region to
     * output buffer and auto-increment the output buffer pointer.
     * Source offset is stored in R0.
     *
     * @param len the number of bytes to be copied, only <= 255 bytes can be copied at once.
     * @return the Type object
     */
    public final Type addDataCopyFromR0(int len) {
        return append(new Instruction(ExtendedOpcodes.EPKTDATACOPYIMM, Rbit1).addU8(len));
    }

    /**
     * Add an instruction to the end of the program to copy data from input packet to output
     * buffer and auto-increment the output buffer pointer.
     * Source offset is stored in R0.
     *
     * @param len the number of bytes to be copied, only <= 255 bytes can be copied at once.
     * @return the Type object
     */
    public final Type addPacketCopyFromR0(int len) {
        return append(new Instruction(ExtendedOpcodes.EPKTDATACOPYIMM, Rbit0).addU8(len));
    }

    /**
     * Add an instruction to the end of the program to copy data from APF program/data region to
     * output buffer and auto-increment the output buffer pointer.
     * Source offset is stored in R0.
     * Copy length is stored in R1.
     *
     * @return the Type object
     */
    public final Type addDataCopyFromR0LenR1() {
        return append(new Instruction(ExtendedOpcodes.EPKTDATACOPYR1, Rbit1));
    }

    /**
     * Add an instruction to the end of the program to copy data from input packet to output
     * buffer and auto-increment the output buffer pointer.
     * Source offset is stored in R0.
     * Copy length is stored in R1.
     *
     * @return the Type object
     */
    public final Type addPacketCopyFromR0LenR1() {
        return append(new Instruction(ExtendedOpcodes.EPKTDATACOPYR1, Rbit0));
    }

    /**
     * Appends a conditional jump instruction to the program: Jumps to {@code tgt} if the UDP
     * payload's DNS questions do NOT contain the QNAMEs specified in {@code qnames} and qtype
     * equals {@code qtype}. Examines the payload starting at the offset in R0.
     * R = 0 means check for "does not contain".
     * Drops packets if packets are corrupted.
     */
    public final Type addJumpIfPktAtR0DoesNotContainDnsQ(@NonNull byte[] qnames, int qtype,
                                                             @NonNull String tgt) {
        validateNames(qnames);
        return append(new Instruction(ExtendedOpcodes.JDNSQMATCH, Rbit0).setTargetLabel(tgt).addU8(
                qtype).setBytesImm(qnames));
    }

    /**
     * Same as {@link #addJumpIfPktAtR0DoesNotContainDnsQ} except passes packets if packets are
     * corrupted.
     */
    public final Type addJumpIfPktAtR0DoesNotContainDnsQSafe(@NonNull byte[] qnames, int qtype,
            @NonNull String tgt) {
        validateNames(qnames);
        return append(new Instruction(ExtendedOpcodes.JDNSQMATCHSAFE, Rbit0).setTargetLabel(
                tgt).addU8(qtype).setBytesImm(qnames));
    }

    /**
     * Appends a conditional jump instruction to the program: Jumps to {@code tgt} if the UDP
     * payload's DNS questions contain the QNAMEs specified in {@code qnames} and qtype
     * equals {@code qtype}. Examines the payload starting at the offset in R0.
     * R = 1 means check for "contain".
     * Drops packets if packets are corrupted.
     */
    public final Type addJumpIfPktAtR0ContainDnsQ(@NonNull byte[] qnames, int qtype,
                                                      @NonNull String tgt) {
        validateNames(qnames);
        return append(new Instruction(ExtendedOpcodes.JDNSQMATCH, Rbit1).setTargetLabel(tgt).addU8(
                qtype).setBytesImm(qnames));
    }

    /**
     * Same as {@link #addJumpIfPktAtR0ContainDnsQ} except passes packets if packets are
     * corrupted.
     */
    public final Type addJumpIfPktAtR0ContainDnsQSafe(@NonNull byte[] qnames, int qtype,
            @NonNull String tgt) {
        validateNames(qnames);
        return append(new Instruction(ExtendedOpcodes.JDNSQMATCHSAFE, Rbit1).setTargetLabel(
                tgt).addU8(qtype).setBytesImm(qnames));
    }

    /**
     * Appends a conditional jump instruction to the program: Jumps to {@code tgt} if the UDP
     * payload's DNS answers/authority/additional records do NOT contain the NAMEs
     * specified in {@code Names}. Examines the payload starting at the offset in R0.
     * R = 0 means check for "does not contain".
     * Drops packets if packets are corrupted.
     */
    public final Type addJumpIfPktAtR0DoesNotContainDnsA(@NonNull byte[] names,
                                                             @NonNull String tgt) {
        validateNames(names);
        return append(new Instruction(ExtendedOpcodes.JDNSAMATCH, Rbit0).setTargetLabel(tgt)
                        .setBytesImm(names));
    }

    /**
     * Same as {@link #addJumpIfPktAtR0DoesNotContainDnsA} except passes packets if packets are
     * corrupted.
     */
    public final Type addJumpIfPktAtR0DoesNotContainDnsASafe(@NonNull byte[] names,
            @NonNull String tgt) {
        validateNames(names);
        return append(new Instruction(ExtendedOpcodes.JDNSAMATCHSAFE, Rbit0).setTargetLabel(tgt)
                .setBytesImm(names));
    }

    /**
     * Appends a conditional jump instruction to the program: Jumps to {@code tgt} if the UDP
     * payload's DNS answers/authority/additional records contain the NAMEs
     * specified in {@code Names}. Examines the payload starting at the offset in R0.
     * R = 1 means check for "contain".
     * Drops packets if packets are corrupted.
     */
    public final Type addJumpIfPktAtR0ContainDnsA(@NonNull byte[] names,
                                                      @NonNull String tgt) {
        validateNames(names);
        return append(new Instruction(ExtendedOpcodes.JDNSAMATCH, Rbit1).setTargetLabel(
                tgt).setBytesImm(names));
    }

    /**
     * Same as {@link #addJumpIfPktAtR0ContainDnsA} except passes packets if packets are
     * corrupted.
     */
    public final Type addJumpIfPktAtR0ContainDnsASafe(@NonNull byte[] names,
            @NonNull String tgt) {
        validateNames(names);
        return append(new Instruction(ExtendedOpcodes.JDNSAMATCHSAFE, Rbit1).setTargetLabel(
                tgt).setBytesImm(names));
    }

    /**
     * Add an instruction to the end of the program to jump to {@code tgt} if the bytes of the
     * packet at an offset specified by register0 match {@code bytes}.
     * R=1 means check for equal.
     */
    public final Type addJumpIfBytesAtR0Equal(byte[] bytes, String tgt)
            throws IllegalInstructionException {
        return append(new Instruction(Opcodes.JNEBS, R1).addUnsigned(
                bytes.length).setTargetLabel(tgt).setBytesImm(bytes));
    }


    /**
     * Check if the byte is valid dns character: A-Z,0-9,-,_
     */
    private static boolean isValidDnsCharacter(byte c) {
        return (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '%';
    }

    private static void validateNames(@NonNull byte[] names) {
        final int len = names.length;
        if (len < 4) {
            throw new IllegalArgumentException("qnames must have at least length 4");
        }
        final String errorMessage = "qname: " + HexDump.toHexString(names)
                + "is not null-terminated list of TLV-encoded names";
        int i = 0;
        while (i < len - 1) {
            int label_len = names[i++];
            // byte == 0xff means it is a '*' wildcard
            if (label_len == -1) continue;
            if (label_len < 1 || label_len > 63) {
                throw new IllegalArgumentException(
                        "label len: " + label_len + " must be between 1 and 63");
            }
            if (i + label_len >= len - 1) {
                throw new IllegalArgumentException(errorMessage);
            }
            while (label_len-- > 0) {
                if (!isValidDnsCharacter(names[i++])) {
                    throw new IllegalArgumentException("qname: " + HexDump.toHexString(names)
                            + " contains invalid character");
                }
            }
            if (names[i] == 0) {
                i++; // skip null terminator.
            }
        }
        if (names[len - 1] != 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Override
    void addArithR1(Opcodes opcode) {
        append(new Instruction(opcode, R1));
    }

    /**
     * Add an instruction to the end of the program to increment the counter value and
     * immediately return PASS.
     *
     * @param counter the counter enum to be incremented.
     */
    @Override
    public final Type addCountAndPass(ApfCounterTracker.Counter counter) {
        checkPassCounterRange(counter);
        return addCountAndPass(counter.value());
    }

    /**
     * Add an instruction to the end of the program to increment the counter value and
     * immediately return DROP.
     *
     * @param counter the counter enum to be incremented.
     */
    @Override
    public final Type addCountAndDrop(ApfCounterTracker.Counter counter) {
        checkDropCounterRange(counter);
        return addCountAndDrop(counter.value());
    }

    @Override
    public final Type addCountAndDropIfR0Equals(long val, ApfCounterTracker.Counter cnt)
            throws IllegalInstructionException {
        checkDropCounterRange(cnt);
        final String tgt = getUniqueLabel();
        return addJumpIfR0NotEquals(val, tgt).addCountAndDrop(cnt).defineLabel(tgt);
    }

    @Override
    public final Type addCountAndPassIfR0Equals(long val, ApfCounterTracker.Counter cnt)
            throws IllegalInstructionException {
        checkPassCounterRange(cnt);
        final String tgt = getUniqueLabel();
        return addJumpIfR0NotEquals(val, tgt).addCountAndPass(cnt).defineLabel(tgt);
    }

    @Override
    public final Type addCountAndDropIfR0NotEquals(long val, ApfCounterTracker.Counter cnt)
            throws IllegalInstructionException {
        checkDropCounterRange(cnt);
        final String tgt = getUniqueLabel();
        return addJumpIfR0Equals(val, tgt).addCountAndDrop(cnt).defineLabel(tgt);
    }

    @Override
    public final Type addCountAndPassIfR0NotEquals(long val, ApfCounterTracker.Counter cnt)
            throws IllegalInstructionException {
        checkPassCounterRange(cnt);
        final String tgt = getUniqueLabel();
        return addJumpIfR0Equals(val, tgt).addCountAndPass(cnt).defineLabel(tgt);
    }

    @Override
    public final Type addCountAndDropIfR0LessThan(long val, ApfCounterTracker.Counter cnt)
            throws IllegalInstructionException {
        checkDropCounterRange(cnt);
        if (val <= 0) {
            throw new IllegalArgumentException("val must > 0, current val: " + val);
        }
        final String tgt = getUniqueLabel();
        return addJumpIfR0GreaterThan(val - 1, tgt).addCountAndDrop(cnt).defineLabel(tgt);
    }

    @Override
    public final Type addCountAndPassIfR0LessThan(long val, ApfCounterTracker.Counter cnt)
            throws IllegalInstructionException {
        checkPassCounterRange(cnt);
        if (val <= 0) {
            throw new IllegalArgumentException("val must > 0, current val: " + val);
        }
        final String tgt = getUniqueLabel();
        return addJumpIfR0GreaterThan(val - 1, tgt).addCountAndPass(cnt).defineLabel(tgt);
    }

    @Override
    public final Type addCountAndDropIfBytesAtR0NotEqual(byte[] bytes,
            ApfCounterTracker.Counter cnt) throws IllegalInstructionException {
        checkDropCounterRange(cnt);
        final String tgt = getUniqueLabel();
        return addJumpIfBytesAtR0Equal(bytes, tgt).addCountAndDrop(cnt).defineLabel(tgt);
    }

    @Override
    public final Type addCountAndPassIfBytesAtR0NotEqual(byte[] bytes,
            ApfCounterTracker.Counter cnt) throws IllegalInstructionException {
        checkPassCounterRange(cnt);
        final String tgt = getUniqueLabel();
        return addJumpIfBytesAtR0Equal(bytes, tgt).addCountAndPass(cnt).defineLabel(tgt);
    }

    @Override
    public final Type addLoadCounter(Register register, ApfCounterTracker.Counter counter)
            throws IllegalInstructionException {
        return append(new Instruction(Opcodes.LDDW, register).addUnsigned(counter.value()));
    }

    @Override
    public final Type addStoreCounter(ApfCounterTracker.Counter counter, Register register)
            throws IllegalInstructionException {
        return append(new Instruction(Opcodes.STDW, register).addUnsigned(counter.value()));
    }

    /**
     * This method is noop in APFv6.
     */
    @Override
    public final Type addCountTrampoline() {
        return self();
    }
}