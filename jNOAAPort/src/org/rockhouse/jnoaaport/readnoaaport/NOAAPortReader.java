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

import java.net.DatagramPacket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.rockhouse.jnoaaport.product.ProductContainer;
import org.rockhouse.jnoaaport.product.ProductContainerException;

/**
 * The meat and potatoes of the NOAAPort ingest software.  This will take
 * the packets from the DVBS stream and reassemble them.  The class will
 * only group packets together to be processed by a writer further down
 * the chain.
 * 
 * This code is based on C code provided by Unidata and available online at 
 * the link below.
 *   
 * @see <a href="https://github.com/semmerson/NOAAPORT">Unidata NOAAPort Source</a>
 * 
 * @author Bryan Rockwood
 *
 */
public class NOAAPortReader implements Runnable {

	private Queue<DatagramPacket> sharedQueue;
	
	private ConcurrentLinkedQueue<ProductContainer> productQueue;

	private volatile boolean stopRequest = false;

	private long lastSbnSequenceNumber;

	private long numberMissedPackets;

	public NOAAPortReader() {
		lastSbnSequenceNumber = -1;
		numberMissedPackets = 0;
		productQueue = new ConcurrentLinkedQueue<ProductContainer>();
	}
	
	public Queue<ProductContainer> getProductContainerQueue(){
		return productQueue;
	}

	@Override
	public void run() {
		int cnt, dataoff, datalen, deflen;
		boolean NWSTG = false;
		boolean GOES = false;
		boolean prod_compressed = false;
		FrameLevelHeader flheader = new FrameLevelHeader();
		ProductDefinitionHeader pdheader = new ProductDefinitionHeader();
		ProductSpecificHeader psheader = new ProductSpecificHeader();
		ProductContainer product = new ProductContainer();
		while (!stopRequest) {
			int offset = 0;
			DatagramPacket packet = sharedQueue.poll();
			if (packet == null) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
				continue;
			}
			byte[] dataPacket = packet.getData();

			if ((dataPacket[0] & 0xFF) != 255) {
				// Log that the first packet out of the box is bad
				System.out.println("Packet does not have a valid start byte.");
				continue;
			}
			if (dataPacket.length < 80) {
				// log that we couldn't even have enough bytes for all three
				// headers
				System.out
						.println("Less than 80 bytes in packet.  Danger Will Robinson.  Continuing");
				continue;
			}
			if (!flheader.readHeader(dataPacket)) {
				// should log here that an invalid packet was received.
				System.out.println("Found a bad packet.  Continuing");
				continue;
			}
			if (lastSbnSequenceNumber != -1) {
				if (flheader.getSequenceNumber() != lastSbnSequenceNumber + 1) {
					System.out.println("Found a gap!");
					// Print out
					// "Gap in SBN sequence number %ld to %ld [skipped %ld]"
					// last_sbn_seqno, sbn->seqno, sbn->seqno - last_sbn_seqno -
					// 1);
					if (flheader.getSequenceNumber() > lastSbnSequenceNumber) {
						numberMissedPackets = numberMissedPackets
								+ (flheader.getSequenceNumber()
										- lastSbnSequenceNumber - 1);
					}
				}
			}
			lastSbnSequenceNumber = flheader.getSequenceNumber();
			if (((flheader.getCommand() != 3) && (flheader.getCommand() != 5))
					|| (flheader.getVersion() != 1)) {
				System.out.printf("Unknown sbn command/version %d PUNT\n",
						flheader.getCommand());
				continue;
			}

			switch (flheader.getDatastream()) {
			case 7: /* test */
			case 6: /* was reserved...now nwstg2 */
			case 5:
				NWSTG = true;
				GOES = false;
				break;
			case 1:
			case 2:
			case 4:
				NWSTG = false;
				GOES = true;
				break;
			default:
				System.out.printf("Unknown NOAAport channel %d PUNT\n",
						flheader.getDatastream());
				continue;
			}
			offset += flheader.getLength();
			pdheader.readHeader(dataPacket, offset);
			if (pdheader.getVersion() != 1) {
				// log Error: PDH transfer type %u, PUNT",
				// pdheader.getTransferType();
				continue;
			}
			offset += pdheader.getLength();
			
			if((pdheader.getTransferType() & 8) > 0){
				//  Say something about this being a product error!
			}
			
			if((pdheader.getTransferType() & 32) > 0){
				//  Say something about this being a product abort!
			}

			prod_compressed = ((pdheader.getTransferType() & 16) > 0);


			// Chech to see if this is a new product yet which should contain a PSH
			if ((pdheader.getSpecificHeaderLength() == 0)
					&& (pdheader.getTransferType() == 0)) {
				continue;
			}

			if (pdheader.getSpecificHeaderLength() != 0) {
				System.out.print("NEW PRODUCT!!!! " + pdheader.getTransferType());
				if (flheader.getCommand() == 5) /* timing block */
				{
					// if (ulogIsDebug ())
					// udebug ("Timing block recieved %ld %ld\0", psh->olen,
					// pdh->len);
					continue; /*
							 * don't step on our psh of a product struct of prod
							 * in progress
							 */
				}

				psheader.readHeader(dataPacket, offset,
						pdheader.getSpecificHeaderLength());
				
				System.out.println(" " + pdheader.getBlockNumber() + " " + psheader.getFragments());
				
				offset += pdheader.getSpecificHeaderLength();

				if (psheader.getOptionFieldLength() != pdheader
						.getSpecificHeaderLength()) {
					// uerror ("ERROR in calculation of psh len %ld %ld", psh->olen, pdh->len);
					continue;
				}
				// if (ulogIsDebug ())
				// udebug ("len %ld", psh->olen);

				/*
				 * if (ulogIsDebug ()) udebug
				 * ("product header flag %d, version %d", psh->hflag,
				 * psh->version); if (ulogIsDebug ()) udebug
				 * ("prodspecific data length %ld", psh->psdl); if (ulogIsDebug
				 * ()) udebug ("bytes per record %ld", psh->bytes_per_record);
				 * if (ulogIsDebug ()) udebug
				 * ("Fragments = %ld category %d ptype %d code %d", psh->frags,
				 * psh->pcat, psh->ptype, psh->pcode); if (psh->frags < 0)
				 * uerror ("check psh->frags %d", psh->frags); if
				 * (psh->origrunid != 0) uerror ("original runid %d",
				 * psh->origrunid); if (ulogIsDebug ()) udebug
				 * ("next header offset %ld", psh->nhoff); if (ulogIsDebug ())
				 * udebug ("original seq number %ld", psh->seqno); if
				 * (ulogIsDebug ()) udebug ("receive time %ld", psh->rectime);
				 * if (ulogIsDebug ()) udebug ("transmit time %ld",
				 * psh->transtime); if (ulogIsDebug ()) udebug ("run ID %ld",
				 * psh->runid); if (ulogIsDebug ()) udebug
				 * ("original run id %ld", psh->origrunid);
				 */
				if (product.isProductStarted()) {
					product = new ProductContainer();
					/*  
					 * Be sure to error info in here (very important).  Basically
					 * what this means is that a packet came in saying it was the 
					 * start of a new product but, in reality, we already have a 
					 * product going.  So, we throw out the old.
					 */
				}
				product.initProduct(pdheader.getSequenceNumber(), psheader.getFragments());

				if (dataPacket.length < (offset + pdheader.getBlockSize())) {
					System.out.println("ARGH!!! THIS ISN'T THE RIGHT LENGTH!!!");
					// uerror ("problem reading datablock");
					continue;
				}
				if(prod_compressed){
					System.out.println("Found a first packet with a compressed header.  " + pdheader.getBlockOffset());
				}
				psheader.readCCB(dataPacket, offset, pdheader.getBlockSize());
			} else {
				/* if a continuation record...don't let psh->pcat get missed */
				if ((flheader.getDatastream() == 4)
						&& (psheader.getProductSpecificCategory() != 3)) {
					GOES = false;
					NWSTG = true;
				}
				psheader.setCcbLength(0);
				// if (ulogIsDebug ())
				// udebug ("continuation record");
				if ((pdheader.getTransferType() & 4) > 0) {
					psheader.setFragments(0);
				}
				if (dataPacket.length < (offset + pdheader.getBlockSize())) {
					// uerror ("problem reading datablock (cont)");
					continue;
				}
				if (!product.isProductStarted()) {
					// if (ulogIsVerbose ())
					// uinfo
					// ("found data block before header, skipping sequence %d frag #%d",
					// pdh->seqno, pdh->dbno);
					continue;
				}
			}
			if(pdheader.getBlockNumber() == 0 && prod_compressed){
				dataoff = flheader.getLength() + pdheader.getLength() + pdheader.getSpecificHeaderLength() + pdheader.getBlockOffset();
				datalen = pdheader.getBlockSize() - -pdheader.getBlockOffset();
			} else {
				dataoff = flheader.getLength() + pdheader.getLength() + pdheader.getSpecificHeaderLength() + psheader.getCcbLength();
				datalen = pdheader.getBlockSize() - psheader.getCcbLength();
			}
			byte[] rawfrag = new byte[datalen];
			System.arraycopy(dataPacket, dataoff, rawfrag, 0, datalen);
			try {
				product.setProductFragment(pdheader.getSequenceNumber(), pdheader.getBlockNumber(), rawfrag, prod_compressed);
			} catch (ProductContainerException e) {
				// Print error here; something went wrong when adding the fragment to the container.
				e.printStackTrace();
				product = new ProductContainer();
				continue;
			}
			
			//woot!  we have a complete product!!!!!
			if(product.getNumberFragments() == 0 || product.getNumberFragments() == pdheader.getBlockNumber() + 1){
				System.out.println("-------- woot!  we have a complete product");
				productQueue.add(product);
				product = new ProductContainer();
			}
			
			//System.out.println(datalen);
			

			// System.out.println(sharedQueue.size());
		}
	}

	public void setSharedQueue(Queue<DatagramPacket> queue) {
		sharedQueue = queue;
	}

}
