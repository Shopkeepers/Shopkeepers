package com.nisovin.shopkeepers.util.timer;

import java.util.concurrent.TimeUnit;

import com.nisovin.shopkeepers.util.java.TimeUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class Timer implements Timings {

	// Note: When a timer operation is called during an unexpected state, we only log a (verbose)
	// error instead of throwing an actual exception, because any issues with the timer potentially
	// yielding wrong values are usually less important compared to issues that could be caused by
	// an exception unexpectedly aborting some external operation. In order to avoid spamming the
	// log with errors when an unexpected timer state is retained and also affects all future timer
	// operations, we only log the error once and then disable all future timer state checks.

	private long counter = 0L;
	private long totalTimeNanos = 0L;
	private long maxTimeNanos = 0L;

	// Current timing:
	private volatile boolean started = false;
	private volatile boolean paused = false;
	private volatile long startTimeNanos;
	private volatile long elapsedTimeNanos;

	private volatile boolean stateErrorEncountered = false;

	public Timer() {
	}

	private void validateState(boolean expectedStated) {
		// We only log the first encountered state error and then skip all future timer state
		// checks:
		if (stateErrorEncountered) return;
		if (!expectedStated) {
			stateErrorEncountered = true;
			Log.severe("Unexpected timer state! Timings might be wrong. started="
					+ started + ", paused=" + paused, new IllegalStateException());
		}
	}

	public void start() {
		this.validateState(!started && !paused);
		// Start a new timing:
		started = true;
		elapsedTimeNanos = 0L;
		startTimeNanos = System.nanoTime();
	}

	public void startPaused() {
		this.start();
		this.pause();
	}

	public void pause() {
		this.validateState(started && !paused);
		// Pause and update the current timing:
		paused = true;
		elapsedTimeNanos += (System.nanoTime() - startTimeNanos);
	}

	public void resume() {
		this.validateState(started && paused);
		// Continue timing:
		paused = false;
		startTimeNanos = System.nanoTime();
	}

	public void stop() {
		this.validateState(started);
		if (!paused) {
			// Update the current timing by pausing:
			this.pause();
		}
		assert paused;

		// Stop the timer:
		started = false;
		paused = false;

		// Update the timings:
		counter++;
		totalTimeNanos += elapsedTimeNanos;

		// Update the max timing:
		if (elapsedTimeNanos > maxTimeNanos) {
			maxTimeNanos = elapsedTimeNanos;
		}
	}

	// TIMINGS

	@Override
	public void reset() {
		counter = 0L;
		totalTimeNanos = 0L;
		maxTimeNanos = 0L;
	}

	@Override
	public long getCounter() {
		return counter;
	}

	@Override
	public double getAverageTimeMillis() {
		double avgTimeNanos = (double) totalTimeNanos / (counter == 0L ? 1L : counter);
		return TimeUtils.convert(avgTimeNanos, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
	}

	@Override
	public double getMaxTimeMillis() {
		return TimeUtils.convert(maxTimeNanos, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
	}
}
