package com.jslib.tiny.container.net;

import java.security.Principal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Guest user for public event streams.
 * 
 * @author Iulian Rotaru
 */
public final class EventGuest implements Principal {
	/** Guest user should have unique name. This is the seed for incremental names. */
	private static AtomicInteger SEED = new AtomicInteger(0x1964);

	/** Unique guest user name. Uniqueness is guaranteed on JVM life span. */
	private final String name;

	/** Construct events guest user with unique name. */
	public EventGuest() {
		this.name = Integer.toHexString(SEED.getAndIncrement());
	}

	@Override
	public String getName() {
		return name;
	}
}