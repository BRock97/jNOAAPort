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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decodes the product specific header which is the third header found only on
 * the first packet of a product
 * 
 * This code is based on C code provided by Unidata and available online at 
 * the link below.
 *   
 * @see <a href="https://github.com/semmerson/NOAAPORT">Unidata NOAAPort Source</a>
 * 
 * @author Bryan Rockwood
 *
 */
public class ProductSpecificHeader {
	private int pshVersion;
	private int optionFieldNumber, optionFieldType, optionFieldLength;
	private int pshFlag;
	private int psDataLength;
	private int bytesPerRecord;
	private int productSpecificType;
	private int productSpecificCategory;
	private int productCode;
	private int fragments;
	private int nextHeaderOffset;
	private int source;
	private long sequenceNumber, receiveTime, transmitTime;
	private int runID, originalRunID;
	private String productName;
	private boolean hasCCB;
	private int ccbMode, ccbSubmode;
	private char[] ccbDataType;
	private char[] metadata;
	private int metadataOffset;
	private static Pattern WMO_HEADER = Pattern.compile("^\\D\\D\\D\\D\\d\\d \\D\\D\\D\\D \\d\\d\\d\\d\\d\\d");

	// CCB specific stuff
	private int ccbB1;
	private int ccbLength;
	private int ccbUser1, ccbUser2;

	public ProductSpecificHeader() {
		productName = new String();
		ccbDataType = new char[20];
		metadata = new char[512];
		ccbLength = 0;
	}

	public void readHeader(byte[] packet, int offset, int length) {
		byte[] header = new byte[length];
		System.arraycopy(packet, offset, header, 0, length);
		hasCCB = false;
		ccbMode = 0;
		ccbSubmode = 0;
		metadata = new char[512];
		metadataOffset = -1;

		optionFieldNumber = header[0] & 0xFF;
		optionFieldType = header[1] & 0xFF;
		optionFieldLength = ((header[2] & 0xFF) << 8) + (header[3] & 0xFF);

		pshVersion = header[4] & 0xFF;
		pshFlag = header[5] & 0xFF;
		psDataLength = ((header[6] & 0xFF) << 8) + (header[7] & 0xFF);

		bytesPerRecord = ((header[8] & 0xFF) << 8) + (header[9] & 0xFF);

		productSpecificType = header[10] & 0xFF;
		productSpecificCategory = header[11] & 0xFF;
		productCode = ((header[12] & 0xFF) << 8) + (header[13] & 0xFF);
		fragments = ((header[14] & 0xFF) << 8) + (header[15] & 0xFF);
		nextHeaderOffset = ((header[16] & 0xFF) << 8) + (header[17] & 0xFF);
		source = header[19] & 0xFF;

		sequenceNumber = (((((((header[20] & 0xFF) << 8) + (header[21] & 0xFF)) << 8) + (header[22] & 0xFF)) << 8) + (header[23] & 0xFF));

		receiveTime = (((((((header[24] & 0xFF) << 8) + (header[25] & 0xFF)) << 8) + (header[26] & 0xFF)) << 8) + (header[27] & 0xFF));
		transmitTime = (((((((header[28] & 0xFF) << 8) + (header[29] & 0xFF)) << 8) + (header[30] & 0xFF)) << 8) + (header[31] & 0xFF));

		runID = ((header[32] & 0xFF) << 8) + (header[33] & 0xFF);
		originalRunID = ((header[34] & 0xFF) << 8) + (header[35] & 0xFF);
	}

	public boolean readCCB(byte[] packet, int offset, int length) {
		byte[] ccbHeader = new byte[length];
		System.arraycopy(packet, offset, ccbHeader, 0, length);
		ccbLength = 2 * (((ccbHeader[0] & 63) << 8) + (ccbHeader[1] & 0xFF));
		int wmoLength, wmoOffset = -1;
		if (ccbLength > length) {
			// uerror
			// ("invalid ccb length = %d %d %d, blen %d\n",ccb->len,b1,b2,blen);

			/* try a failsafe header, otherwise use our own! */
			wmoLength = 0;
			while (((int) ccbHeader[wmoLength] >= 32) && (wmoLength < 256)) {
				wmoLength++;
			}
			if (wmoLength > 0) {
				productName = new String(ccbHeader, 0, wmoLength);
			} else {
				productName = "Unidentifiable product";
			}
			ccbLength = 0;
			return false;
		}
		ccbMode = ccbHeader[10] & 0xFF;
		ccbSubmode = ccbHeader[11] & 0xFF;
		hasCCB = true;
		ccbUser1 = ccbHeader[12] & 0xFF;
		ccbUser2 = ccbUser1; // ?!  Not sure here... that's what it is in the Unidata code....
		String headerSearch = new String(ccbHeader, ccbLength, 18);
		Matcher wmoSearch = WMO_HEADER.matcher(headerSearch);
		if(wmoSearch.find()) {
			productName = headerSearch;
		} else {
			wmoLength = 0;
			while (((int) ccbHeader[wmoLength + ccbLength] >= 32) && (wmoLength < 256)) {
				wmoLength++;
			}
			if (wmoLength > 0) {
				productName = new String(ccbHeader, 0 + ccbLength, wmoLength);
			} else {
				productName = "Unidentifiable product";
			}
		}

		return true;
	}

	public int getPshVersion() {
		return pshVersion;
	}

	public int getOptionFieldNumber() {
		return optionFieldNumber;
	}

	public int getOptionFieldType() {
		return optionFieldType;
	}

	public int getOptionFieldLength() {
		return optionFieldLength;
	}

	public int getPshFlag() {
		return pshFlag;
	}

	public int getPsDataLength() {
		return psDataLength;
	}

	public int getBytesPerRecord() {
		return bytesPerRecord;
	}

	public int getProductSpecificType() {
		return productSpecificType;
	}

	public int getProductSpecificCategory() {
		return productSpecificCategory;
	}

	public int getProductCode() {
		return productCode;
	}

	public int getFragments() {
		return fragments;
	}

	public void setFragments(int frags) {
		fragments = frags;
	}

	public int getNextHeaderOffset() {
		return nextHeaderOffset;
	}

	public int getSource() {
		return source;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public long getReceiveTime() {
		return receiveTime;
	}

	public long getTransmitTime() {
		return transmitTime;
	}

	public int getRunID() {
		return runID;
	}

	public int getOriginalRunID() {
		return originalRunID;
	}

	public String getProductName() {
		return productName;
	}

	public boolean hasCCB() {
		return hasCCB;
	}

	public int getCCBMode() {
		return ccbMode;
	}

	public int getCCBSubmode() {
		return ccbSubmode;
	}

	public char[] getCCBDataType() {
		return ccbDataType;
	}

	public char[] getMetadata() {
		return metadata;
	}

	public int getMetadataOff() {
		return metadataOffset;
	}

	public int getCcbB1() {
		return ccbB1;
	}

	public void setCcbB1(int ccbB1) {
		this.ccbB1 = ccbB1;
	}

	public int getCcbLength() {
		return ccbLength;
	}

	public void setCcbLength(int ccbLength) {
		this.ccbLength = ccbLength;
	}

	public int getCcbUser1() {
		return ccbUser1;
	}

	public void setCcbUser1(int ccbUser1) {
		this.ccbUser1 = ccbUser1;
	}

	public int getCcbUser2() {
		return ccbUser2;
	}

	public void setCcbUser2(int ccbUser2) {
		this.ccbUser2 = ccbUser2;
	}

	public String toString() {
		return "***********************************************"
				+ "\nVersion:\t\t\t"
				+ pshVersion
				+ "\noption field number:\t\t"
				+ optionFieldNumber
				+ "\noption field type:\t\t"
				+ optionFieldType
				+ "\noption field length:\t\t"
				+ optionFieldLength
				+ "\nproduct specific header flag:\t"
				+ pshFlag
				+ "\nproduct specific length:\t"
				+ psDataLength
				+ "\nbytes per record:\t\t"
				+ bytesPerRecord
				+ "\nproduct specific type:\t\t"
				+ productSpecificType
				+ "\nproduct specific category:\t"
				+ productSpecificCategory
				+ "\nproduct code:\t\t\t"
				+ productCode
				+ "\nfragements:\t\t\t"
				+ fragments
				+ "\nnext header offset:\t\t"
				+ nextHeaderOffset
				+ "\nsource:\t\t\t\t"
				+ source
				+ "\nsequence number:\t\t"
				+ sequenceNumber
				+ "\n***********************************************";
	}

}
