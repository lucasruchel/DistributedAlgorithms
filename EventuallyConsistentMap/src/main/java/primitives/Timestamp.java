package primitives;

/**
 * Opaque version structure.
 * <p>
 * Classes implementing this interface must also implement
 * {@link #hashCode()} and {@link #equals(Object)}.
 */

import static com.google.common.base.Preconditions.checkNotNull;

public interface Timestamp extends Comparable<Timestamp> {

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);

    /**
     * Tests if this timestamp is newer than the specified timestamp.
     *
     * @param other timestamp to compare against
     * @return true if this instance is newer
     */
    default boolean isNewerThan(Timestamp other) {
        return this.compareTo(checkNotNull(other)) > 0;
    }

    /**
     * Tests if this timestamp is older than the specified timestamp.
     *
     * @param other timestamp to compare against
     * @return true if this instance is older
     */
    default boolean isOlderThan(Timestamp other) {
        return this.compareTo(checkNotNull(other)) < 0;
    }
}

