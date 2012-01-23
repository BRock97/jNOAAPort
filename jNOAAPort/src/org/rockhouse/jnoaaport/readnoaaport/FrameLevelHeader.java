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
 * Decodes the frame level header which is the first header in any packet.
 * 
 * This code is based on C code provided by Unidata and available online at 
 * the link below.
 *   
 * @see <a href="https://github.com/semmerson/NOAAPORT">Unidata NOAAPort Source</a>
 * 
 * @author Bryan Rockwood
 *
 */
public class FrameLevelHeader {
	
	private int version;
	private int length;
	private int datastream;
	private long sequenceNumber;
	private int runNumber;
	private int command;
	private long checksum;
	private boolean validPacket = true;

	public FrameLevelHeader() {
	}
	
	public boolean readHeader(byte[] header) {
		int b1, b2;
		long lval;
		long csum = 0;
		int i;
		lval = ((header[14] & 0xFF) << 8) + (header[15] & 0xFF);
		for (i = 0; i < 14; i++) {
			csum = csum + (header[i] & 0xFF);
		}
		if (csum != lval) {
			System.out.println("SBN checksum invalid. Expected: " + lval
					+ " Calculated: " + csum);
			return false;
		} else {
			checksum = csum;
		}
		b1 = header[0] & 0xFF;
		if (b1 != 255) {
			// Code from readsbn.c. Appears to print out the first 32 bytes
			// in the packet in search of the start. Why this is done after
			// the checksum is beyond me.
			/*
			 * for(i = 0; i < 32; i++) { uinfo("look val %d %u",i,buf[i]); }
			 */
			return false;
		}
		b1 = (header[2] >> 4) & 0xFF;
		b2 = ((header[2] & 0xFF) & 15);
		version = b1;
		length = b2 * 4;

		command = header[4] & 0xFF;

		switch (command) {
		case 3: /* product format data transfer */
		case 5: /* Synchonize timing */
		case 10:/* Test message */
			break;
		default:
			// uerror ( "Invalid SBN command %d", sbn->command );
			return false;
		}

		datastream = header[5] & 0xFF;

		sequenceNumber = (((((((header[8] & 0xFF) << 8) + (header[9] & 0xFF)) << 8) + (header[10] & 0xFF)) << 8) + (header[11] & 0xFF));
		
		runNumber = ((header[12] & 0xFF) << 8) + (header[13] & 0xFF);
		
		return true;

	}

	public int getVersion() {
		return version;
	}

	public int getLength() {
		return length;
	}

	public int getDatastream() {
		return datastream;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public int getRunNumber() {
		return runNumber;
	}

	public int getCommand() {
		return command;
	}

	public long getChecksum() {
		return checksum;
	}

	public boolean isValidPacket() {
		return validPacket;
	}

}
