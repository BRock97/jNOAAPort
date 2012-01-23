/**
 * Copyright (c) 2012 Bryan Rockwood
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 **/

package org.rockhouse.jnoaaport.readnoaaport;

/**
 * Decodes the product definition header which is the second header in any packet.
 * 
 * This code is based on C code provided by Unidata and available online at 
 * the link below.
 *   
 * @see <a href="https://github.com/semmerson/NOAAPORT">Unidata NOAAPort Source</a>
 * 
 * @author Bryan Rockwood
 *
 */
public class ProductDefinitionHeader {

	private int version;
	private int length;
	private int transferType;
	private int specificHeaderLength;
	private short blockNumber;
	private int blockOffset;
	private int blockSize;
	private int recordsPerBlock;
	private int blocksPerRecord;
	private int sequenceNumber;
	private byte[] header;

	public ProductDefinitionHeader() {
		header = new byte[16];
	}

	public boolean readHeader(byte[] packet, int offset) {
		System.arraycopy(packet, offset, header, 0, 16);
		version = (header[0] >> 4) & 0x0F;
		length = (header[0] & 15) * 4;
		transferType = (header[1] & 0xFF);
		specificHeaderLength = (((header[2] & 0xFF) << 8) + (header[3] & 0xFF))
				- length;
		blockNumber = (short)(((header[4] & 0xFF) << 8) + (header[5] & 0xFF));
		blockOffset = ((header[6] & 0xFF) << 8) + (header[7] & 0xFF);
		blockSize = ((header[8] & 0xFF) << 8) + (header[9] & 0xFF);
		recordsPerBlock = (header[10] & 0xFF);
		blocksPerRecord = (header[11] & 0xFF);
		sequenceNumber = (((((((header[12] & 0xFF) << 8) + (header[13] & 0xFF)) << 8) + (header[14] & 0xFF)) << 8) + (header[15] & 0xFF));
		return true;
	}

	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * @return the transferType
	 */
	public int getTransferType() {
		return transferType;
	}

	/**
	 * @return the headerLength
	 */
	public int getSpecificHeaderLength() {
		return specificHeaderLength;
	}

	/**
	 * @return the blockNumber
	 */
	public short getBlockNumber() {
		return blockNumber;
	}

	/**
	 * @return the blockOffset
	 */
	public int getBlockOffset() {
		return blockOffset;
	}

	/**
	 * @return the blockSize
	 */
	public int getBlockSize() {
		return blockSize;
	}

	/**
	 * @return the recordsPerBlock
	 */
	public int getRecordsPerBlock() {
		return recordsPerBlock;
	}

	/**
	 * @return the blocksPerRecord
	 */
	public int getBlocksPerRecord() {
		return blocksPerRecord;
	}

	/**
	 * @return the sequenceNumber
	 */
	public int getSequenceNumber() {
		return sequenceNumber;
	}
}
