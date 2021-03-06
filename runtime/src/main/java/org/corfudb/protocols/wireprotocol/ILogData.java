package org.corfudb.protocols.wireprotocol;

import org.corfudb.protocols.logprotocol.LogEntry;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.runtime.view.Address;

import java.util.UUID;

/** An interface to log data entries.
 *
 * Log data entries represent data stored in the actual log,
 * with convenience methods for software to retrieve the
 * stored information.
 *
 * Created by mwei on 8/17/16.
 */
public interface ILogData extends IMetadata, Comparable<ILogData> {

    Object getPayload(CorfuRuntime t);
    DataType getType();

    @Override
    default int compareTo(ILogData o) {
        return getGlobalAddress().compareTo(o.getGlobalAddress());
    }

    /** This class provides a serialization handle, which
     * manages the lifetime of the serialized copy of this
     * entry.
     */
    class SerializationHandle implements AutoCloseable {

        /** A reference to the log data. */
        final ILogData data;

        /** Explicitly request the serialized form of this log data
         * which only exists for the lifetime of this handle.
         * @return  The serialized form of this handle.
         */
        public ILogData getSerialized() {
            return data;
        }

        /** Create a new serialized handle with a reference
         * to the log data.
         * @param data  The log data to manage.
         */
        public SerializationHandle(ILogData data)
        {
            this.data = data;
            data.acquireBuffer();
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
            data.releaseBuffer();
        }
    }

    void releaseBuffer();
    void acquireBuffer();
    default SerializationHandle getSerializedForm() {
        return new SerializationHandle(this);
    }
    /**
     * Return whether or not this entry is a log entry.
     */
    default boolean isLogEntry(CorfuRuntime runtime) {
        return getPayload(runtime) instanceof LogEntry;
    }

    /**
     * Return the payload as a log entry.
     */
    default LogEntry getLogEntry(CorfuRuntime runtime) {
        return (LogEntry) getPayload(runtime);
    }

    /**
     * Return if there is backpointer for a particular stream.
     */
    default boolean hasBackpointer(UUID streamID) {
        return getBackpointerMap() != null
                && getBackpointerMap().containsKey(streamID) &&
                Address.isAddress(getBackpointerMap().get(streamID));
    }

    /**
     * Return the backpointer for a particular stream.
     */
    default Long getBackpointer(UUID streamID) {
        if (!hasBackpointer(streamID)) return null;
        return getBackpointerMap().get(streamID);
    }

    /**
     * Return if this is the first entry in a particular stream.
     */
    default boolean isFirstEntry(UUID streamID) {
        return getBackpointer(streamID) == -1L;
    }

    /**
     * Get an estimate of how large this entry is in memory.
     *
     * @return An estimate on the size of this object, in bytes.
     */
    default int getSizeEstimate() {
        return 1; // The default is that we don't know, so we return 1.
    }

    /** Return whether the entry represents a hole or not. */
    default boolean isHole() {
        return getType() == DataType.HOLE;
    }

    /** Return whether the entry represents an empty entry or not. */
    default boolean isEmpty() { return getType() == DataType.EMPTY;}

    /** Assign a given token to this log data.
     *
     * @param token     The token to use.
     */
    default void useToken(IToken token) {
        setGlobalAddress(token.getTokenValue());
        if (token.getBackpointerMap().size() > 0) {
            setBackpointerMap(token.getBackpointerMap());
        }
    }
}
