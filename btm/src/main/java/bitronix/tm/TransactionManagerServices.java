/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm;

import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.Journal;
import bitronix.tm.journal.NullJournal;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.resource.ResourceLoader;
import bitronix.tm.timer.TaskScheduler;
import bitronix.tm.twopc.executor.*;
import bitronix.tm.utils.InitializationException;
import bitronix.tm.utils.ClassLoaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for all BTM services.
 * <p>The different services available are: {@link BitronixTransactionManager}, {@link Configuration}, {@link Journal},
 * {@link TaskScheduler}, {@link ResourceLoader}, {@link Recoverer} and {@link Executor}. They are used in all places
 * of the TM so they must be globally reachable.</p>
 *
 * @author lorban
 */
public class TransactionManagerServices {

    private final static Logger log = LoggerFactory.getLogger(TransactionManagerServices.class);

    private static BitronixTransactionManager transactionManager;
    private static BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private static Configuration configuration;
    private static Journal journal;
    private static TaskScheduler taskScheduler;
    private static ResourceLoader resourceLoader;
    private static Recoverer recoverer;
    private static Executor executor;

    /**
     * Create an initialized transaction manager.
     * @return the transaction manager.
     */
    public synchronized static BitronixTransactionManager getTransactionManager() {
        if (transactionManager == null)
            transactionManager = new BitronixTransactionManager();
        return transactionManager;
    }

    /**
     * Create the JTA 1.1 TransactionSynchronizationRegistry.
     * @return the TransactionSynchronizationRegistry.
     */
    public synchronized static BitronixTransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        if (transactionSynchronizationRegistry == null)
            transactionSynchronizationRegistry = new BitronixTransactionSynchronizationRegistry();
        return transactionSynchronizationRegistry;
    }

    /**
     * Create the configuration of all the components of the transaction manager.
     * @return the global configuration.
     */
    public synchronized static Configuration getConfiguration() {
        if (configuration == null)
            configuration = new Configuration();
        return configuration;
    }

    /**
     * Create the transactions journal.
     * @return the transactions journal.
     */
    public synchronized static Journal getJournal() {
        if (journal == null) {
            String configuredJounal = getConfiguration().getJournal();
            if ("disk".equals(configuredJounal))
                journal = new DiskJournal();
            else if ("null".equals(configuredJounal))
                journal = new NullJournal();
            else {
                try {
                    Class clazz = ClassLoaderUtils.loadClass(configuredJounal);
                    journal = (Journal) clazz.newInstance();
                } catch (Exception ex) {
                    throw new InitializationException("invalid journal implementation '" + configuredJounal + "'", ex);
                }
            }
            if (log.isDebugEnabled()) { log.debug("using journal " + configuredJounal); }
        }
        return journal;
    }

    /**
     * Create the task scheduler.
     * @return the task scheduler.
     */
    public synchronized static TaskScheduler getTaskScheduler() {
        if (taskScheduler == null) {
            taskScheduler = new TaskScheduler();
            taskScheduler.start();
        }
        return taskScheduler;
    }

    /**
     * Create the resource loader.
     * @return the resource loader.
     */
    public synchronized static ResourceLoader getResourceLoader() {
        if (resourceLoader == null) {
            resourceLoader = new ResourceLoader();
        }
        return resourceLoader;
    }

    /**
     * Create the transaction recoverer.
     * @return the transaction recoverer.
     */
    public synchronized static Recoverer getRecoverer() {
        if (recoverer == null) {
            recoverer = new Recoverer();
        }
        return recoverer;
    }

    /**
     * Create the 2PC executor.
     * @return the 2PC executor.
     */
    public synchronized static Executor getExecutor() {
        if (executor == null) {
            boolean async = getConfiguration().isAsynchronous2Pc();
            if (async) {
                if (log.isDebugEnabled()) { log.debug("using AsyncExecutor"); }
                executor = new AsyncExecutor();
            }
            else {
                if (log.isDebugEnabled()) { log.debug("using SyncExecutor"); }
                executor = new SyncExecutor();
            }
        }
        return executor;
    }

    /**
     * Check if the transaction manager has started.
     * @return true if the transaction manager has started.
     */
    public synchronized static boolean isTransactionManagerRunning() {
        return transactionManager != null;
    }

    /**
     * Check if the task scheduler has started.
     * @return true if the task scheduler has started.
     */
    public synchronized static boolean isTaskSchedulerRunning() {
        return taskScheduler != null;
    }

    /**
     * Clear services references. Called at the end of the shutdown procedure.
     */
    protected synchronized static void clear() {
        transactionManager = null;
        transactionSynchronizationRegistry = null;
        configuration = null;
        journal = null;
        taskScheduler = null;
        resourceLoader = null;
        recoverer = null;
        executor = null;
    }

}