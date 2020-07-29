package com.ms.silverking.cloud.dht;

import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.ms.silverking.cloud.dht.client.ChecksumType;
import com.ms.silverking.cloud.dht.client.Compression;
import com.ms.silverking.cloud.dht.client.OpSizeBasedTimeoutController;
import com.ms.silverking.cloud.dht.client.OpTimeoutController;
import com.ms.silverking.cloud.dht.trace.TraceIDProvider;
import com.ms.silverking.cloud.dht.common.DHTConstants;
import com.ms.silverking.cloud.dht.common.OptionsHelper;
import com.ms.silverking.text.FieldsRequirement;
import com.ms.silverking.text.ObjectDefParser2;

/**
 * Options for Invalidation operations
 */
public class InvalidationOptions extends PutOptions {
  private static final OpTimeoutController standardTimeoutController = new OpSizeBasedTimeoutController();
  private static final PutOptions template = OptionsHelper.newInvalidationOptions(standardTimeoutController,
      PutOptions.defaultVersion, PutOptions.noVersionRequired, PutOptions.noLock, DHTConstants.noSecondaryTargets);

  static {
    ObjectDefParser2.addParser(template, FieldsRequirement.ALLOW_INCOMPLETE);
  }

  // FIXME - temp until legacy instances have current defs
  public InvalidationOptions(OpTimeoutController opTimeoutController, Set<SecondaryTarget> secondaryTargets,
      Compression compression, ChecksumType checksumType, boolean checksumCompressedValues, long version,
      byte[] userData) {
    super(opTimeoutController, secondaryTargets, compression, checksumType, checksumCompressedValues, version,
        noVersionRequired, PutOptions.noLock, DHTConstants.defaultFragmentationThreshold, userData);
  }

  public InvalidationOptions(OpTimeoutController opTimeoutController, Set<SecondaryTarget> secondaryTargets,
      Compression compression, ChecksumType checksumType, boolean checksumCompressedValues, long version,
      long requiredPreviousVersion, short lockSeconds, int fragmentationThreshold, byte[] userData) {
    this(opTimeoutController, secondaryTargets, DHTConstants.defaultTraceIDProvider, compression, checksumType,
        checksumCompressedValues, version, requiredPreviousVersion, lockSeconds, fragmentationThreshold, userData);
  }

  /**
   * Complete constructor. We ignore any fragmentationThreshold passed in as it does not make sense for an
   * invalidation
   *
   * @param lockSeconds seconds to lock this key
   */
  public InvalidationOptions(OpTimeoutController opTimeoutController, Set<SecondaryTarget> secondaryTargets,
      TraceIDProvider traceIDProvider, Compression compression, ChecksumType checksumType,
      boolean checksumCompressedValues, long version, long requiredPreviousVersion, short lockSeconds,
      int fragmentationThreshold, byte[] userData) {
    super(opTimeoutController, secondaryTargets, traceIDProvider, compression, checksumType, checksumCompressedValues,
        version, requiredPreviousVersion, lockSeconds, DHTConstants.defaultFragmentationThreshold, userData);
  }

  /**
   * Construct InvalidationOptions from the given arguments. Usage is generally not recommended.
   * Instead of using this constructor, most applications should obtain an InvalidationOptions
   * object from a valid source such as the session, the namespace, or the namespace perspective.
   *
   * @param opTimeoutController opTimeoutController for the operation
   * @param secondaryTargets    constrains queried secondary replicas
   *                            to operation solely on the node that receives this operation
   * @param version             version of this object
   * @param lockSeconds         seconds to lock this key
   */
  public InvalidationOptions(OpTimeoutController opTimeoutController, Set<SecondaryTarget> secondaryTargets,
      long version, long requiredPreviousVersion, short lockSeconds) {
    super(opTimeoutController, secondaryTargets, Compression.NONE, ChecksumType.SYSTEM, false, version,
        requiredPreviousVersion, lockSeconds, DHTConstants.defaultFragmentationThreshold, null);
  }

  /**
   * Return an InvalidationOptions instance like this instance, but with a new OpTimeoutController.
   *
   * @param opTimeoutController the new field value
   * @return the modified InvalidationOptions
   */
  public InvalidationOptions opTimeoutController(OpTimeoutController opTimeoutController) {
    return new InvalidationOptions(opTimeoutController, getSecondaryTargets(), getTraceIDProvider(), getCompression(),
        getChecksumType(), getChecksumCompressedValues(), getVersion(), getRequiredPreviousVersion(), getLockSeconds(),
        getFragmentationThreshold(), getUserData());
  }

  /**
   * Return an InvalidationOptions instance like this instance, but with a new secondaryTargets.
   *
   * @param secondaryTargets the new field value
   * @return the modified InvalidationOptions
   */
  public InvalidationOptions secondaryTargets(Set<SecondaryTarget> secondaryTargets) {
    return new InvalidationOptions(getOpTimeoutController(), secondaryTargets, getTraceIDProvider(), getCompression(),
        getChecksumType(), getChecksumCompressedValues(), getVersion(), getRequiredPreviousVersion(), getLockSeconds(),
        getFragmentationThreshold(), getUserData());
  }

  /**
   * Return an InvalidationOptions instance like this instance, but with a new secondaryTargets.
   *
   * @param secondaryTarget the new field value
   * @return the modified InvalidationOptions
   */
  public InvalidationOptions secondaryTargets(SecondaryTarget secondaryTarget) {
    Preconditions.checkNotNull(secondaryTarget);
    return secondaryTargets(ImmutableSet.of(secondaryTarget));
  }

  /**
   * Return a InvalidationOptions instance like this instance, but with a new traceIDProvider.
   *
   * @param traceIDProvider the new field value
   * @return the modified InvalidationOptions
   */
  public InvalidationOptions traceIDProvider(TraceIDProvider traceIDProvider) {
    Preconditions.checkNotNull(traceIDProvider);
    return new InvalidationOptions(getOpTimeoutController(), getSecondaryTargets(), traceIDProvider, getCompression(),
        getChecksumType(), getChecksumCompressedValues(), getVersion(), getRequiredPreviousVersion(), getLockSeconds(),
        getFragmentationThreshold(), getUserData());
  }

  /**
   * Return an InvalidationOptions instance like this instance, but with a new version.
   *
   * @param version the new field value
   * @return the modified InvalidationOptions
   */
  public InvalidationOptions version(long version) {
    return new InvalidationOptions(getOpTimeoutController(), getSecondaryTargets(), getTraceIDProvider(),
        getCompression(), getChecksumType(), getChecksumCompressedValues(), version, getRequiredPreviousVersion(),
        getLockSeconds(), getFragmentationThreshold(), getUserData());
  }

  /**
   * Return an InvalidationOptions instance like this instance, but with a new requiredPreviousVersion.
   *
   * @param requiredPreviousVersion the new field value
   * @return the modified InvalidationOptions
   */
  public InvalidationOptions requiredPreviousVersion(long requiredPreviousVersion) {
    return new InvalidationOptions(getOpTimeoutController(), getSecondaryTargets(), getTraceIDProvider(),
        getCompression(), getChecksumType(), getChecksumCompressedValues(), getVersion(), requiredPreviousVersion,
        getLockSeconds(), getFragmentationThreshold(), getUserData());
  }

  /**
   * Return an InvalidationOptions instance like this instance, but with a new lockSeconds.
   *
   * @param lockSeconds the new field value
   * @return the modified InvalidationOptions
   */
  public InvalidationOptions lockSeconds(short lockSeconds) {
    return new InvalidationOptions(getOpTimeoutController(), getSecondaryTargets(), getTraceIDProvider(),
        getCompression(), getChecksumType(), getChecksumCompressedValues(), getVersion(), getRequiredPreviousVersion(),
        lockSeconds, getFragmentationThreshold(), getUserData());
  }

  @Override
  public String toString() {
    return ObjectDefParser2.objectToString(this);
  }
}
