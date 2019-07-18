package com.ms.silverking.cloud.dht.daemon.storage.protocol;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import com.ms.silverking.cloud.dht.client.OperationState;
import com.ms.silverking.cloud.dht.common.DHTKey;
import com.ms.silverking.cloud.dht.common.KeyUtil;
import com.ms.silverking.cloud.dht.common.OpResult;
import com.ms.silverking.cloud.dht.daemon.storage.protocol.SingleWriterConsistentStorageEntryState.StateTransitionResult;
import com.ms.silverking.cloud.dht.net.ForwardingMode;
import com.ms.silverking.cloud.dht.net.MessageGroupKeyOrdinalEntry;
import com.ms.silverking.log.Log;
import com.ms.silverking.net.IPAndPort;

public class SingleWriterConsistentWrite extends BaseStorageOperation<SingleWriterConsistentStorageEntryState> {
    private static final boolean    debug = false;
    
    public SingleWriterConsistentWrite(long deadline, PutOperationContainer putOperationContainer, ForwardingMode forwardingMode) {
        super(deadline, putOperationContainer, forwardingMode);
    }
    
    /**
     * Called after a replica state updates
     */
    @Override
    public void update(DHTKey key, IPAndPort replica, byte storageState, OpResult update, 
                       PutVirtualCommunicator pvComm) {
        assert replica != null;
        if (update == OpResult.SUCCEEDED || update == OpResult.INVALID_VERSION 
                || update == OpResult.MUTATION || update == OpResult.NO_SUCH_NAMESPACE
                || update == OpResult.LOCKED) {
            SingleWriterConsistentStorageEntryState entryState;

            if (debug) {
                System.out.printf("key %s replica %s\tupdate %s\n", KeyUtil.keyToString(key), replica, update);
            }
            // FUTURE - could refactor code to only perform this lookup once if needed
            entryState = getEntryState(key);
            synchronized (entryState) {
                if (entryState.getCurOpResult() == OpResult.INCOMPLETE) {
                    TwoPhaseStorageState    prevState;            
                    StateTransitionResult   transitionResult;
                    TwoPhaseStorageState    nextState;
                    OpResult                opResult;
                    
                    prevState = TwoPhaseStorageState.values[storageState];
                    if (update.toOperationState() != OperationState.FAILED) {
                        nextState = TwoPhaseStorageState.nextState(prevState);
                        transitionResult = entryState.transitionFromState(replica, prevState);
                        opResult = nextState.toOpResult();
                    } else {
                        entryState.fail(replica, prevState);
                        transitionResult = StateTransitionResult.COMPLETION;
                        nextState = TwoPhaseStorageState.FAILED;
                        opResult = update;
                    }
                    if (debug) {
                        System.out.printf("key %s prevState %s nextState %s transitionResult %s\n",
                                KeyUtil.keyToString(key), prevState, nextState, transitionResult);
                    }
                    switch (transitionResult) {
                    case REPLICA_TRANSITION: 
                        break;
                    case QUORUM_TRANSITION:
                        quorumTransition(key, entryState.primaryReplicas(), nextState, update, pvComm);
                        quorumTransition(key, entryState.secondaryReplicas(), nextState, update, pvComm);
                        if (!nextState.isComplete()) {
                            break;
                        } else {
                            // fall through if complete
                        }
                    case COMPLETION:
                        int _completeEntries;
                        
                        pvComm.sendResult(key, opResult);
                        _completeEntries = completeEntries.incrementAndGet();
                        if (debug) {
                            System.out.printf("COMPLETION %s %d %d\n", 
                                    KeyUtil.keyToString(key), _completeEntries, numEntries);
                        }
                        if (_completeEntries >= numEntries) {
                            setOpResult(OpResult.SUCCEEDED);
                        }                    
                        if (debug) {
                            System.out.printf("opResult %s\n", getOpResult());
                        }
                        break;
                    case NO_TRANSITION: 
                        break; // must be old update
                    default: throw new RuntimeException("panic");
                    }
                } else {
                    if (Log.levelMet(Level.FINE)) {
                        Log.fine("Update for non-incomplete: ", key + " " + replica + " " + update);
                    }
                }
            }
        } else {
            // FUTURE - Consider additional error handling
            Log.warning("Unexpected update: ", key + " " + replica + " " + update);
        }
    }
    
    private void quorumTransition(DHTKey key, Collection<IPAndPort> replicas, TwoPhaseStorageState nextState, 
                                  OpResult update, PutVirtualCommunicator pvComm) {
        if (debug) {
            System.out.printf("quorumTransition %s %s %s\n", KeyUtil.keyToString(key), nextState, update);
        }
        for (IPAndPort replica : replicas) {
            MessageGroupKeyOrdinalEntry  entry;
            
            if (debug) {
                System.out.printf("quorumTransition replica %s %s\n", KeyUtil.keyToString(key), replica);
            }
            entry = new MessageGroupKeyOrdinalEntry(key, (byte)nextState.ordinal());
            // FUTURE - Consider faster implementation for local case. For now, we handle
            //          local with remote.
            //if (pvComm.isLocalReplica(replica)) {
            //    update(key, replica, (byte)nextState.ordinal(), update, pvComm);
            //} else {
                pvComm.forwardUpdateEntry(replica, entry);
            //}
        }
        if (debug) {
            System.out.printf("out quorumTransition %s %s %s\n", KeyUtil.keyToString(key), nextState, update);
        }
    }

    @Override
    public void initializeEntryState(DHTKey entryKey, List<IPAndPort> primaryReplicas, 
                                                      List<IPAndPort> secondaryReplicas) {
        setEntryState(entryKey, new SingleWriterConsistentStorageEntryState(primaryReplicas, secondaryReplicas));
    }
    
    public byte nextStorageState(byte prevStorageState) {
        return (byte)TwoPhaseStorageState.nextState(TwoPhaseStorageState.values[prevStorageState]).ordinal();
    }
}
