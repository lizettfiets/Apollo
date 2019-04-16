package com.apollocurrency.aplwallet.apl.core.db.cdi.transaction;

import static org.slf4j.LoggerFactory.getLogger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Holds opened connection {@link Handle} for current thread.
 */
@Singleton
public class JdbiHandleFactory {
    private static final Logger log = getLogger(JdbiHandleFactory.class);

    private final static ThreadLocal<Handle> currentHandleThreadLocal = new ThreadLocal<>();
    private final static ThreadLocal<Boolean> isReadOnly = new ThreadLocal<>();

    private Jdbi jdbi;

    @Inject
    public void setJdbi(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public boolean isInTransaction() {
        Handle handle = currentHandleThreadLocal.get();
        return handle != null && handle.isInTransaction();
    }

    public Handle getCurrentHandle() {
        Handle handle = currentHandleThreadLocal.get();
        if (handle == null) {
            handle = open(isReadOnly.get() != null ? isReadOnly.get() : false);
        }
        return handle;
    }

    public Handle open(boolean isAnnotatedReadOnly) {
        Handle handle = currentHandleThreadLocal.get();
        if (handle != null) {
            return handle;
        }
        handle = jdbi.open();
        handle.setReadOnly(isAnnotatedReadOnly);
        currentHandleThreadLocal.set(handle);
        isReadOnly.set(isAnnotatedReadOnly ? Boolean.TRUE : Boolean.FALSE);
        return handle;
    }

    protected void begin() {
        Handle handle = getCurrentHandle();
        handle.begin();
        isReadOnly.set(Boolean.FALSE);
    }

    protected void commit() {
        Handle handle = getCurrentHandle();
        handle.commit();
    }

    protected void rollback() {
        Handle handle = getCurrentHandle();
        handle.rollback();
    }

    public void close() {
        Handle handle = getCurrentHandle();
        if (!handle.isClosed()) {
            handle.close();
        }
        currentHandleThreadLocal.remove();
        isReadOnly.remove();
    }

}
