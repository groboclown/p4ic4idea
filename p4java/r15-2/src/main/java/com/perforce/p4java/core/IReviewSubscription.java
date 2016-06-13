/**
 * 
 */
package com.perforce.p4java.core;

/**
 * Defines the view mapping for Perforce user review subscriptions.<p>
 * 
 * Perforce user subscriptions are a simple view that uses the left
 * side only, but this is not enforced if the superclass methods
 * are also used.
 */

public interface IReviewSubscription extends IMapEntry {
	
	/**
	 * Get the review subscription.
	 */
	
	String getSubscription();
	
	/**
	 * Set the review subscription.
	 */
	void setSubscription(String subscription);
}
