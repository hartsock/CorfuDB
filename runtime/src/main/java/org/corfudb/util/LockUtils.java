package org.corfudb.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by mwei on 4/5/16.
 */
public class LockUtils {

    public static class AutoCloseRWLock implements AutoCloseable {

        ReadWriteLock lock;
        Lock releasingLock;

        public AutoCloseRWLock(ReadWriteLock lock) {
            this.lock = lock;
        }

        public AutoCloseRWLock writeLock() {
            if (releasingLock != null) {
                throw new RuntimeException("Attempted to acquire a wlock when one was already acquired!");
            }
            releasingLock = lock.writeLock();
            releasingLock.lock();
            return this;
        }

        public AutoCloseRWLock readLock() {
            if (releasingLock != null) {
                throw new RuntimeException("Attempted to acquire a rlock when one was already acquired!");
            }
            releasingLock = lock.readLock();
            releasingLock.lock();
            return this;
        }

        /**
         * Closes this resource, relinquishing any underlying resources.
         * This method is invoked automatically on objects managed by the
         * {@code try}-with-resources statement.
         */
        @Override
        public void close() {
            releasingLock.unlock();
        }
    }
}
