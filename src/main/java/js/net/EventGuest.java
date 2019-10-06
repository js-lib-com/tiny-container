package js.net;

import java.security.Principal;

/**
 * Guest user for public event streams.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class EventGuest implements Principal {
	/** Guest user should have unique name. This is the seed for incremental names. */
	private static int SEED = 0x1964;

	/** Unique guest user name. Uniqueness is guaranteed on JVM life span. */
	private final String name;

	/** Construct events guest user with unique name. */
	public EventGuest() {
		this.name = Integer.toHexString(SEED++);
	}

	@Override
	public String getName() {
		return name;
	}
}