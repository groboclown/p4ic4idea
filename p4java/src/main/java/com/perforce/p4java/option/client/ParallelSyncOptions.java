/**
 *
 */
package com.perforce.p4java.option.client;

import com.perforce.p4java.impl.generic.core.DefaultParallelSync;
import com.perforce.p4java.server.callback.IParallelCallback;

/**
 * Simple default options object for IClient.syncParallel.
 */
public class ParallelSyncOptions {

	/**
	 * The call back interface for parallel execution.
	 */
	private IParallelCallback callback;
	/**
	 * Specifies the number of files in a batch
	 */
	private int batch = 0;
	/**
	 * Specifies the the number of bytes in a batch
	 */
	private int batchSize = 0;
	/**
	 * Specifies the minimum number of files in a parallel sync
	 */
	private int minimum = 0;
	/**
	 * Specifies the minimum number of bytes in a parallel sync
	 */
	private int minumumSize = 0;
	/**
	 * Specifies the number of independent network connections to be used during parallelisation
	 */
	private int numberOfThreads = 0;

	/**
	 * Default constructor
	 */
	public ParallelSyncOptions() {
	}

	/**
	 * Constructor with the given arguments
	 *
	 * @param batch
	 * @param batchSize
	 * @param minimum
	 * @param minumumSize
	 * @param numberOfThreads
	 * @param callback
	 */
	public ParallelSyncOptions(int batch, int batchSize, int minimum, int minumumSize, int numberOfThreads,
	                           IParallelCallback callback) {
		this.batch = batch;
		this.batchSize = batchSize;
		this.minimum = minimum;
		this.minumumSize = minumumSize;
		this.numberOfThreads = numberOfThreads;
		this.callback = callback;
	}

	/**
	 * Constructs a ParallelSyncOptions with an instance of an IParallelCallback
	 *
	 * @param callback
	 */
	public ParallelSyncOptions(IParallelCallback callback) {
		this.callback = callback;
	}

	/**
	 * Returns batch
	 *
	 * @return
	 */
	public int getBatch() {

		return batch;
	}

	/**
	 * Returns baatchSize
	 *
	 * @return
	 */
	public int getBatchSize() {

		return batchSize;
	}

	public int getMinimum() {

		return minimum;
	}

	/**
	 * Returns minumum
	 *
	 * @return
	 */
	public int getMinumumSize() {

		return minumumSize;
	}

	/**
	 * Returns the number of threads
	 *
	 * @return
	 */
	public int getNumberOfThreads() {

		return numberOfThreads;
	}

	/**
	 * Returns IParallelCallback
	 *
	 * @return
	 */
	public IParallelCallback getCallback() {
		if (callback == null) {
			callback = new DefaultParallelSync();
		}
		return callback;
	}

	/**
	 * Sets IParallelCallback
	 *
	 * @param callback
	 */
	public void setCallback(IParallelCallback callback) {
		this.callback = callback;
	}

	/**
	 * Sets the batch
	 *
	 * @param batch
	 */
	public void setBatch(int batch) {
		this.batch = batch;
	}

	/**
	 * Sets the batch size
	 *
	 * @param batchSize
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * Sets minimum
	 *
	 * @param minimum
	 */
	public void setMinimum(int minimum) {
		this.minimum = minimum;
	}

	/**
	 * Sets minimum
	 *
	 * @param minumumSize
	 */
	public void setMinumumSize(int minumumSize) {
		this.minumumSize = minumumSize;
	}

	/**
	 * Sets number of threads
	 *
	 * @param numberOfThreads
	 */
	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}
}
