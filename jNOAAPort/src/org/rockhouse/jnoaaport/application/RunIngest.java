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

package org.rockhouse.jnoaaport.application;

import java.net.SocketException;
import java.net.UnknownHostException;

import org.rockhouse.jnoaaport.dvbs.MulticastReader;
import org.rockhouse.jnoaaport.readnoaaport.NOAAPortReader;
import org.rockhouse.jnoaaport.writer.ProductHandlerInterface;


/**
 * An example class which sets up the various threads to read in data from a 
 * NOAAPort data feed and send it to the specified writer class.
 * 
 * @author Bryan Rockwood
 *
 */
public class RunIngest {

	public static void main(String[] args) {
		
		ProductHandlerInterface fw = null;
		ClassLoader cl = RunIngest.class.getClassLoader();
		try{
			Class<?> aClass = cl.loadClass("org.rockhouse.jnoaaport.writer.FileWriter");
			fw = (ProductHandlerInterface)aClass.newInstance();
		} catch (ClassNotFoundException e){
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		MulticastReader mReader1 = null;
		try {
			mReader1 = new MulticastReader("224.0.1.2", "172.16.198.1");
		} catch (UnknownHostException e) {
			// Print error if the multicast addy is bad
			e.printStackTrace();
		} catch (SocketException e) {
			// Print an error if the specified NIC is bad
			e.printStackTrace();
		}
		
		NOAAPortReader reader = new NOAAPortReader();
		fw.setProductHandlerQueue(reader.getProductContainerQueue());
		
		reader.setSharedQueue(mReader1.getQueue());
		Thread thread1 = new Thread(mReader1, "dvbs1");
		Thread thread2 = new Thread(reader, "reader1");
		Thread thread3 = new Thread(fw, "fileWriter");
		
		thread3.start();
		thread2.start();
		thread1.start();
		

	}

}
