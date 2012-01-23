 /** Copyright (c) 2012 Bryan Rockwood
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

package org.rockhouse.jnoaaport.writer;

import java.io.FileOutputStream;
import java.util.Queue;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.rockhouse.jnoaaport.product.ProductContainer;
import org.rockhouse.jnoaaport.product.ProductFragment;

/**
 * An example class which implements the ProductHandlerInterface.  It simply 
 * writes the data to a drive.  
 * 
 * @author Bryan Rockwood
 *
 */
public class FileWriter implements ProductHandlerInterface, Runnable{

	Queue<ProductContainer> productQueue;
	private volatile boolean stopRequest = false;
	
	@Override
	public void run() {
		ProductFragment pf = null;
		byte[] output = new byte[10000];
		
		int counter = 0;
		while (!stopRequest) {
			ProductContainer container = productQueue.poll();
			if (container == null) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
				continue;
			} else {
				String filename = "/Users/brock97/data/noaaport_" + counter + ".bin";
				try{
				FileOutputStream fos = new FileOutputStream(filename);
				while(container.hasFragments()){
					pf = container.getNextFragment();
					
					if(pf.fragmentNumber == 0 && pf.isCompressed){
						System.out.println("Filename for compressed product:  /Users/brock97/data/noaaport_" + counter + ".bin");
					}
					if(pf.isCompressed){
						Inflater inf = new Inflater();
						inf.setInput(pf.data);
						try {
							int outputsize = inf.inflate(output);
							if(outputsize == 0){
								System.out.println(pf.data.length + " " + inf.getTotalIn() + " " + inf.getTotalOut());
								if(inf.needsDictionary()) System.out.println("Needs dic");
								if(inf.needsInput()) System.out.println("Needs input");
							} else {
								System.out.printf("Input size:  %d, output size:  %d\n", pf.data.length, outputsize);
								fos.write(output, 0, outputsize);
							}
							inf.end();
						} catch (DataFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						inf.end();
					} else {
						fos.write(pf.data);
					}
				}
				fos.close();
				} catch (Exception e){
					System.out.println(e.getLocalizedMessage());
				}
				counter++;
			}
		}
	}

	@Override
	public void setProductHandlerQueue(Queue<ProductContainer> pcq) {
		productQueue = pcq;
	}

}
