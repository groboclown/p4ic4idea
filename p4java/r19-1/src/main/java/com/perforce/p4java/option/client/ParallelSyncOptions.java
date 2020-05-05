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
	 * @param batch - number of files in a batch
	 * @param batchSize - number of bytes in a batch
	 * @param minimum - minimum number of files in a parallel sync
	 * @param minumumSize - minimum number of bytes in a parallel sync
	 * @param numberOfThreads - number of independent network connections to be used during parallelisation
	 * @param callback - call back interface for parallel execution
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
	 * @param callback - call back interface for parallel execution
	 */
	public ParallelSyncOptions(IParallelCallback callback) {
		this.callback = callback;
	}

	/**
	 * @return number of files in a batch
	 */
	public int getBatch() {

		return batch;
	}

	/**
	 * @return number of bytes in a batch
	 */
	public int getBatchSize() {

		return batchSize;
	}

	/**
	 * @return minimum number of files in a parallel sync
	 */
	public int getMinimum() {

		return minimum;
	}

	/**
	 * @return minimum number of bytes in a parallel sync
	 */
	public int getMinumumSize() {

		return minumumSize;
	}

	/**
	 * @return number of independent network connections to be used during parallelisation
	 */
	public int getNumberOfThreads() {

		return numberOfThreads;
	}

	/**
	 * @return call back interface for parallel execution
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
	 * @param callback - call back interface for parallel execution
	 */
	public void setCallback(IParallelCallback callback) {
		this.callback = callback;
	}

	/**
	 * Sets the batch
	 *
	 * @param batch - number of bytes in a batch
	 */
	public void setBatch(int batch) {
		this.batch = batch;
	}

	/**
	 * Sets the batch size
	 *
	 * @param batchSize - number of bytes in a batch
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * Sets minimum
	 *
	 * @param minimum - minimum number of files in a parallel sync
	 */
	public void setMinimum(int minimum) {
		this.minimum = minimum;
	}

	/**
	 * Sets minimum
	 *
	 * @param minumumSize - minimum number of bytes in a parallel sync
	 */
	public void setMinumumSize(int minumumSize) {
		this.minumumSize = minumumSize;
	}

	/**
	 * Sets number of threads
	 *
	 * @param numberOfThreads - number of independent network connections to be used during parallelisation
	 */
	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}
}
