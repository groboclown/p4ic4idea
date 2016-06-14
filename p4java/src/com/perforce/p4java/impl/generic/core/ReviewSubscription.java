/**
 * 
 */
package com.perforce.p4java.impl.generic.core;

import com.perforce.p4java.core.IReviewSubscription;

/**
 * Default ReviewSubscription implementation class.
 */

public class ReviewSubscription extends MapEntry implements IReviewSubscription {
	
	/**
	 * Default constructor -- sets all local and superclass fields to null or zero.
	 */
	public ReviewSubscription() {
	}
	
	/**
	 * Explicit value constructor -- calls super(order, subscription).
	 * Note that this probably won't do what you expect it to if
	 * there's more than one element in the subscription.
	 */
	public ReviewSubscription(int order, String subscription) {
		super(order, subscription);
	}

	/**
	 * @see com.perforce.p4java.core.IReviewSubscription#getSubscription()
	 */
	public String getSubscription() {
		return this.getLeft();
	}

	/**
	 * @see com.perforce.p4java.core.IReviewSubscription#setSubscription(java.lang.String)
	 */
	public void setSubscription(String subscription) {
		this.setLeft(subscription);
	}
}
