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

package org.rockhouse.jnoaaport.dvbs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * This simple class listens on a multicast address and puts any received
 * packets on a queue to be handled else where.  
 * 
 * This code is based on C code provided by Unidata and available online at 
 * the link below.
 *   
 * @see <a href="https://github.com/semmerson/NOAAPORT">Unidata NOAAPort Source</a>
 * 
 * @author Bryan Rockwood
 *
 */
public class MulticastReader implements Runnable{
	
	private Inet4Address multiAddress;
	
	private ConcurrentLinkedQueue<DatagramPacket> sharedQueue;
	
	private volatile boolean stopRequest = false;
	
	private static final int[] s_port = { 1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208 };
	
	private static final int MAX_MSG = 10000;
	
	private int socketTimeout = 0; // No timeout
	
	private NetworkInterface ni;
	
	int portNumber;

	
	/**
	 * Constructs a multicast reader that will listen to the specified address
	 * and only on the specified NIC.
	 * 
	 * @param address Multicast address to listen to
	 * @param nic The address of the NIC to connect to
	 * @throws UnknownHostException
	 * @throws SocketException
	 */
	public MulticastReader(String address, String nic) throws UnknownHostException, SocketException {
		multiAddress = (Inet4Address)Inet4Address.getByName(address);
		ni = NetworkInterface.getByInetAddress(InetAddress.getByName(nic));
		sharedQueue = new ConcurrentLinkedQueue<DatagramPacket>();
		if(!multiAddress.isMulticastAddress()) {
			System.out.println("This isn't a multicast address!");
		}
		int lastoctet = address.lastIndexOf(".") + 1;
		int s_portnumber = Integer.parseInt(address.substring(lastoctet));
		portNumber = s_port[s_portnumber - 1];
	}
	
	@Override
	public void run() {
		MulticastSocket msocket = null;
		int sbnnum, lastnum = 0, missedpackets = 0, totalpackets = 0;
		try{
			msocket = new MulticastSocket(portNumber);
			msocket.setNetworkInterface(ni);
			msocket.joinGroup(multiAddress);
			msocket.setReuseAddress(true);
			msocket.setSoTimeout(socketTimeout);
		} catch (IOException e){
			e.printStackTrace();
		}
		while (!stopRequest) {
			try {
				byte[] msg = new byte[MAX_MSG];
				DatagramPacket recv = new DatagramPacket(msg, msg.length);
				msocket.receive(recv);
				sbnnum = (((((((msg[8] & 0xFF) << 8) + (msg[9] & 0xFF)) << 8) +  (msg[10] & 0xFF)) << 8) + (msg[11] & 0xFF));
				int transfer = msg[17] & 0xFF;
				if ((lastnum != 0) && (lastnum + 1 != sbnnum)) {
					int n = recv.getLength();
					System.out.println("Woops, missed one.  This packet appears to be: " + sbnnum + " while I last worked on: " + lastnum + " and received " + n + " with a transfer type of " + transfer);
					missedpackets++;
				}
				totalpackets++;
				//System.out.println(sbnnum);
				lastnum = sbnnum;
				//System.out.println("Received a packet of size " + recv.getLength() + " " + sbnnum);
				sharedQueue.add(recv);
			} catch (IOException e) {
				System.out.println("Timed out while waiting.  Processed " + totalpackets + " packets and missed " + missedpackets);
				stopRequest = true;
			}
		}
	}
	
	/**
	 * Tells the thread to stop running.
	 * 
	 */
	public void requestStop() {
		stopRequest = true;
	}

	/**
	 * Allows external threads to get the queue this object will store incoming
	 * packets to.
	 * 
	 * @return the queue
	 */
	public Queue<DatagramPacket> getQueue(){
		return sharedQueue;
	}

	/**
	 * Overides the default time.
	 * 
	 * @param timeout time in milliseconds 
	 */
	public void setTimeout(int timeout) {
		socketTimeout = timeout;
	}
}

