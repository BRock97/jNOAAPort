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

package org.rockhouse.jnoaaport.product;

import java.util.ArrayList;

/**
 * A ProductContainer manages the fragments as they are received from
 * the DVBS broadcast.  Fragments are added to the container and, when
 * all fragments are received, the container can be passed to a writer
 * that will be responsible for assembling the fragments.
 * 
 * @author Bryan Rockwood
 *
 */
public class ProductContainer {
	private int sequenceNumber;
	
	private int numberFragments;
	
	private boolean productStarted;
	
	private ProductFragment last;
	
	private ArrayList<ProductFragment> productFragments;
	
	public ProductContainer(){
		productFragments = new ArrayList<ProductFragment>();
		productStarted = false;
		last = null;
	}

	/**
	 * Retrieves the sequence number of the product these fragments
	 * belong to.
	 * 
	 * @return the sequence number
	 */
	public int getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * Sets the sequence number for the container.
	 * 
	 * @param seqnum The sequence number of the product
	 */
	public void setSequenceNumber(int seqnum) {
		sequenceNumber = seqnum;
	}

	/**
	 * Returns the number of fragments for this product.
	 * 
	 * @return fragment count
	 */
	public int getNumberFragments() {
		return numberFragments;
	}

	/**
	 * Sets the number of fragments for the container.
	 * 
	 * @param numfrags fragment count
	 */
	public void setNumberFragments(int numfrags) {
		numberFragments = numfrags;
	}

	/**
	 * Indicates if this container has fragments.
	 * 
	 * @return true if this already contains fragments, false otherwise
	 */
	public boolean isProductStarted() {
		return productStarted;
	}

	/**
	 * Sets the status of the container.
	 * 
	 * @param prodstart True if the container has fragments, false otherwise
	 */
	public void setProductStarted(boolean prodstart) {
		productStarted = prodstart;
	}
	
	/**
	 * Initialize the container to the specified sequence and fragment
	 * count.
	 * 
	 * @param seqnum sequence number of the product
	 * @param numfrag fragment count
	 */
	public void initProduct(int seqnum, int numfrag){
		sequenceNumber = seqnum;
		numberFragments = numfrag;
		productStarted = true;
	}
	
	/**
	 * Inserts a product fragment into the end of the container.
	 * 
	 * @param seqNum sequence number of fragment (which differs from the product sequence number... yeah)
	 * @param fragNum the fragment's number 
	 * @param data the packet's data
	 * @param isCompressed is the packet compressed
	 * @throws ProductContainerException if the fragment inserted is out of order, a ProductContainerException is thrown
	 */
	public void setProductFragment(int seqNum, short fragNum, byte[] data, boolean isCompressed) throws ProductContainerException{
		ProductFragment pf = new ProductFragment();
		pf.sequenceNumber = seqNum;
		pf.fragmentNumber = fragNum;
		pf.data = data;
		pf.isCompressed = isCompressed;
		productFragments.add(pf);
		if(numberFragments != 0 && last != null){
			if(fragNum != last.fragmentNumber + 1 || seqNum != sequenceNumber){
				throw new ProductContainerException("Missing fragment in sequence.");
			}
		}
		last = pf;
	}
	
	/**
	 * Determines if the container still has fragments.  To be used by an iterator to
	 * reassemble a product.
	 * 
	 * @return true if the container still has fragments
	 */
	public boolean hasFragments(){
		return !productFragments.isEmpty();
	}
	
	/**
	 * Returns the next fragment in order.  This will remove the fragment
	 * from the container.
	 * 
	 * @return a product fragment
	 */
	public ProductFragment getNextFragment(){
		return productFragments.remove(0);
	}

}
