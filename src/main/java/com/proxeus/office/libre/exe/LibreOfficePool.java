package com.proxeus.office.libre.exe;

import com.proxeus.error.UnavailableException;
import com.proxeus.office.libre.LibreConfig;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * LibreOfficePool decreases the chances of failure for updating and converting the document to the requested type.
 * It makes trial and errors on various levels as LibreOffice can be very annoying on high load.
 *
 * It is configurable by min, max, highLoad and the path to the LibreOffice program.
 * With min, max and highLoad. You have the possibility to tell how much resources should be used even in edge cases.
 * min LibreOffice executables that should be ready in any case.
 * max LibreOffice executables that are allowed.
 * highLoad is the percentage of the actual usage to tell when to start to increase the executables.
 *
 * Please be aware of this minefield.
 * Objects of LibreOffice are sometimes just like that disposed, sometimes when requesting an object on a valid connection
 * it returns null and sometimes it fails where you wouldn't expect it although every call is handled by a single thread.
 * Any change can easily lead to unexpected behaviours.
 *
 * This class makes the horror more than just acceptable.
 */
public class LibreOfficePool {
    private Logger log = LogManager.getLogger(this.getClass());

    private LinkedBlockingDeque<LibreOffice> executables;
    private LinkedBlockingQueue<Long> toBeReleased;
    private LinkedBlockingQueue<LibreOffice> toReconnect;
    private final Object lock = new Object();
    private Map<Long, LibreOffice> occupied;
    private final int min;
    private final int max;
    private final String exeDir;
    private long lastReconnectionAttempt = 0;
    private volatile long reconnectionAttempt = 0;
    private int highLoad = 60;
    private int lowLoad = 0;
    private final int failoverStart = 1;
    private int failover = failoverStart;
    private long cleanupThreadSleep = 200;
    private Thread cleanupThread;
    private Thread reconnectionThread;

    public LibreOfficePool(LibreConfig libreConfig) throws Exception {
        exeDir = libreConfig.librepath;
        if (libreConfig.min <= 0 || libreConfig.max < libreConfig.min) {
            throw new InvalidParameterException("min must be higher than 0 and max must be at least as min or higher");
        }
        // UNSAFE and removed in fix-templ-22:
        // pathFixForLibrary();
        // Instead you can use:
        // export LD_LIBRARY_PATH=path/to/your/library/dir/
        executables = new LinkedBlockingDeque<>(libreConfig.max);
        occupied = new HashMap<>(libreConfig.max);
        toBeReleased = new LinkedBlockingQueue<>(libreConfig.max);
        toReconnect = new LinkedBlockingQueue<>(libreConfig.max);
        min = libreConfig.min;
        max = libreConfig.max;
        if (libreConfig.highLoad > 10 && libreConfig.highLoad <= 100) {
            highLoad = libreConfig.highLoad;
        }
        setupCleanupThread();
    }

    private void setupCleanupThread() {
        reconnectionThread = new Thread() {
            public void run() {
                try {
                    prepare();
                    while (!isInterrupted()) {
                        LibreOffice lo = toReconnect.poll(400, TimeUnit.MILLISECONDS);
                        if (lo != null) {
                            reconnect(lo);
                        } else if (lastReconnectionAttempt < reconnectionAttempt) {
                            //reconnect all prepared ones for font update
                            while (!isInterrupted()) {
                                lo = executables.pollLast();
                                if (lo != null) {
                                    if (lo.needToReconnect(reconnectionAttempt)) {
                                        reconnect(lo);
                                    } else {
                                        if(!executables.offer(lo)){
                                            lo.close();
                                        }
                                        //all reconnected
                                        break;
                                    }
                                }
                                lo = toReconnect.poll();
                                if (lo != null) {
                                    reconnect(lo);
                                }
                            }
                            lastReconnectionAttempt = reconnectionAttempt;
                        }
                    }
                } catch (InterruptedException e) {
                    //exit
                }
            }

            private void reconnect(LibreOffice lo) {
                try {
                    int currentOccupiedSize;
                    synchronized (lock) {
                        currentOccupiedSize = occupied.size();
                    }
                    if(shouldIncrease(currentOccupiedSize, 0)){
                        lo.reconnect(reconnectionAttempt);
                        if (executables.offerFirst(lo)) {
                            return;
                        }
                    }
                } catch (Exception e) {}
                //list seems to be full, an error was thrown or we don't need to increase anymore
                lo.close();
            }
        };
        reconnectionThread.start();
        cleanupThread = new Thread() {
            public void run() {
                try {
                    prepare();
                    //runtime maintenance
                    LibreOffice lo = null;
                    int currentOccupiedSize = 0;
                    while (!isInterrupted()) {
                        Long takerId = toBeReleased.poll(cleanupThreadSleep, TimeUnit.MILLISECONDS);
                        if (takerId != null) {
                            synchronized (lock) {
                                lo = occupied.remove(takerId);
                                currentOccupiedSize = occupied.size();
                            }
                            if (lo == null) {
                                //already released
                                continue;
                            }
                            if(offer(lo)){
                                isHighLoad(currentOccupiedSize);
                            }
                            checkLowLoad(currentOccupiedSize);
                        } else {
                            synchronized (lock) {
                                currentOccupiedSize = occupied.size();
                            }
                            if (!isHighLoad(currentOccupiedSize)) {
                                checkLowLoad(currentOccupiedSize);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.info("shutting down libre pool cleanup");
                }

                //shutdown
                synchronized (lock) {
                    Collection<LibreOffice> los = occupied.values();
                    for (LibreOffice lo : los) {
                        lo.close();
                    }
                }
                Iterator<LibreOffice> iterator = executables.iterator();
                while (iterator.hasNext()) {
                    iterator.next().close();
                    iterator.remove();
                }
            }
            private void checkLowLoad(int currentOccupiedSize){
                if (currentOccupiedSize == 0 && executables.size() > min) {
                    ++lowLoad;
                    lowLoad = lowLoad + (int) ((double) lowLoad * 0.4);
                    if (failover > failoverStart) {
                        failover = failover - (int) ((double) failover * 0.4);
                        if (failover < failoverStart) {
                            failover = failoverStart;
                        }
                    }
                    if (lowLoad > failover) {
                        LibreOffice lo = executables.pollLast();
                        if (lo != null) {
                            if (cleanupThreadSleep > 200) {
                                cleanupThreadSleep = 10;
                            }
                            lowLoad = 0;
                            lo.close();
                            if(executables.size() == min){
                                cleanupThreadSleep = 200;
                                System.gc();
                            }
                        }
                    }
                } else {
                    lowLoad = 0;
                }
            }
            private boolean isHighLoad(int currentOccupiedSize) {
                try {
                    if (shouldIncrease(currentOccupiedSize, toReconnect.size())) {
                        lowLoad = 0;
                        int fo = (int) ((double) failover * 6.8);
                        if (fo > failover) {
                            failover = fo;
                        }
                        if(cleanupThreadSleep<2000){
                            cleanupThreadSleep += 100;
                        }
                        return offerNew();
                    }
                } catch (Exception e) {
                    //not important
                }
                return false;
            }
        };
        cleanupThread.start();
    }

    private boolean offerNew(){
        if(toReconnect.size()<max){
            return toReconnect.offer(new LibreOffice(exeDir));
        }
        return false;
    }

    private boolean shouldIncrease(int currentOccupiedSize, int pending){
        //if over highLoad % occupied and list not full, offer a new one already to prevent requests from waiting
        //as creating a new instance takes around 1 second
        int available = executables.size();
        return currentOccupiedSize > 0 && ((double) currentOccupiedSize / available * 100.0) >= highLoad && available + pending + currentOccupiedSize < max;
    }

    private void prepare() {
        for (; executables.size() < min; ) {
            LibreOffice lo = tryNewLibreOffice();
            if (lo != null) {
                if (!executables.offer(lo)) {
                    lo.close();
                }
            }
        }
    }

    public LibreOffice take() throws Exception {
        return take(false);
    }

    public LibreOffice take(boolean reconnect) throws Exception {
        //return loff;
        long id = Thread.currentThread().getId();
        LibreOffice lo = null;
        int currentOccupiedSize;
        synchronized (lock) {
            currentOccupiedSize = occupied.size();
            if (currentOccupiedSize > 0) {
                lo = occupied.get(id);
            }
            if (reconnect) {
                reconnectionAttempt = System.currentTimeMillis();
            }
        }
        if (lo != null) {
            //already taken one, provide it again
            return lo;
        }
        if (currentOccupiedSize + toReconnect.size() >= max) {
            //full, lets wait until one of them is getting released again
            lo = executables.poll(8, TimeUnit.SECONDS);
            //looks like the service is under heavy load, lets throw and exceptions saying try again later
            //holding the request longer doesn't make sense as it takes more resources
            if (lo == null) {
                throw new UnavailableException("All LibreOffice instances busy.  Please try again later.");
            }
        } else {
            //try poll
            lo = executables.poll();
            if (lo == null) {
                offerNew();
                lo = executables.poll(8, TimeUnit.SECONDS);
                if (lo == null) {
                    throw new UnavailableException("Cannot get LibreOffice instance.  Please try again later.");
                }
            }
        }
        if (reconnect) {
            tryReconnect(lo);
        }
        synchronized (lock) {
            occupied.put(id, lo);
        }
        return lo;
    }

    public void release() {
        long tid = Thread.currentThread().getId();
        if (!toBeReleased.offer(tid)) {
            //couldn't queue it for release so lets just close it ourselves
            LibreOffice lo;
            synchronized (lock) {
                lo = occupied.get(tid);
            }
            if (lo != null) {
                lo.close();
            }
        }
    }

    private boolean offer(LibreOffice lo) {
        if (lo.needToReconnect(reconnectionAttempt)) {
            if (toReconnect.offer(lo)) {
                return true;
            }else{
                //list seems to be full
                lo.close();
                return false;
            }
        }
        if (executables.offer(lo)) {
            return true;
        }else{
            //list seems to be full
            lo.close();
            return false;
        }
    }

    private final int maxAttempts = 6;
    private LibreOffice tryNewLibreOffice() {
        int count = 0;
        do {
            try {
                return new LibreOffice(exeDir, true);
            } catch (Exception e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    break;
                }
            }
            ++count;
        } while (count < maxAttempts);
        return null;
    }

    private void tryReconnect(LibreOffice lo) throws Exception {
        int count = 0;
        do {
            try {
                lo.reconnect(reconnectionAttempt);
                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    break;
                }
            }
            ++count;
        } while (count < maxAttempts);
        lo.close();
        throw new UnavailableException("Please try again later.");
    }

    public void close() {
        cleanupThread.interrupt();
        reconnectionThread.interrupt();
    }
}
