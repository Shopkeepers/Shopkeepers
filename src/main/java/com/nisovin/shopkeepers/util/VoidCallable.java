package com.nisovin.shopkeepers.util;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface VoidCallable extends Callable<Void> {

	public static Callable<Void> toCallable(VoidCallable voidCallable) {
		return voidCallable;
	}

	@Override
	public default Void call() throws Exception {
		this.voidCall();
		return null;
	}

	public void voidCall() throws Exception;
}
