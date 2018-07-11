package com.github.sereneant.segystream

import java.nio.ByteOrder

import akka.util.{ByteIterator, ByteString}

sealed trait SegyPart {
  def info: String //textual description
}

case class TextHeader(
  s: String
) extends SegyPart {
  override def info: String = "TextHeader:\n" + s.grouped(80).mkString("\n")
}

case class BinHeader(
  jobId: Int,
  lineNumber: Int,
  reelNumber: Int,
  dataTracesPerEnsemble: Short,
  auxTracesPerEnsemble: Short,
  sampleIntervalMicroSec: Short,
  sampleIntervalMicroSecOrig: Short,
  samplesPerDataTrace: Short,
  samplesPerDataTraceOrig: Short,
  dataSampleFormatCode: Short,
  ensembleFold: Short,
  traceSortingCode: Short,
  verticalSumCode: Short,
  sweepFrequencyAtStartHz: Short,
  sweepFrequencyAtEndHz: Short,
  sweepLengthMs: Short,
  sweepTypeCode: Short,
  traceNumberOfSweepChannel: Short,
  sweepTraceTaperLengthAtStartMs: Short,
  sweepTraceTaperLengthAtEndMs: Short,
  taperType: Short,
  correlatedDataTraces: Short,
  binaryGainRecovered: Short,
  amplitudeRecoveryMethod: Short,
  measurementSystem: Short,
  impulseSignalPolarity: Short,
  vibratoryPolarityCode: Short,
  // 3261 - 3500 Unassigned
  segyRevision: Short, //Rev1 -> 0x100
  fixedLengthTraceFlag: Short,
  extendedTextHeaders: Short
  // 3507 - 3600 Unassigned
) extends SegyPart {
  def getDataFormat: DataFormat[_] = DataFormat.of(dataSampleFormatCode)
  def info: String = s"BinHeader: rev=$segyRevision, dataTraces: $dataTracesPerEnsemble, " +
    s"samplesPerTrace=$samplesPerDataTrace at ${sampleIntervalMicroSec}ms, format=$getDataFormat"
}

object DataFormat {
  def of(dataSampleFormatCode: Short): DataFormat[_] = dataSampleFormatCode match {
    case 1 => DataFormatIbmFloat // 4-byte IBM floating point
    case 2 => DataFormatInt // 4-byte, two's complement integer
    case 3 => DataFormatShort // 2-byte, two's complement integer
    case 4 => // 4-byte fixed point with gain (obsolete)
      throw new UnsupportedOperationException("Format code 5 is not supported")
    case 5 => DataFormatFloat // 4-byte IEEE floating point
    case 6 | 7 => throw new UnsupportedOperationException("Format codes 6, 7 are not supported")
    case 8 => DataFormatByte // 1-byte, two's complement integer
    case f => throw new SegyException(s"Invalid format code: $f")
  }

  case object DataFormatIbmFloat extends DataFormat[Float] {
    override val length = 4
    override def next(byteIt: ByteIterator): Float = ConvertUtils.ibm370toFloat(byteIt)
    override def nextFloat(byteIt: ByteIterator): Float = next(byteIt)
    override def toString: String = "4-byte IBM floating point"
  }
  case object DataFormatInt extends DataFormat[Int] {
    override val length = 4
    override def next(byteIt: ByteIterator): Int = byteIt.getInt
    override def nextFloat(byteIt: ByteIterator): Float = next(byteIt)
    override def toString: String = "4-byte, two's complement integer"
  }
  case object DataFormatShort extends DataFormat[Short] {
    override val length = 2
    override def next(byteIt: ByteIterator): Short = byteIt.getShort
    override def nextFloat(byteIt: ByteIterator): Float = next(byteIt)
    override def toString: String = "2-byte, two's complement integer"
  }
  case object DataFormatFloat extends DataFormat[Float] {
    override val length = 4
    override def next(byteIt: ByteIterator): Float = byteIt.getFloat
    override def nextFloat(byteIt: ByteIterator): Float = next(byteIt)
    override def toString: String = "4-byte IEEE floating point"
  }
  case object DataFormatByte extends DataFormat[Byte] {
    override val length = 1
    override def next(byteIt: ByteIterator): Byte = byteIt.getByte
    override def nextFloat(byteIt: ByteIterator): Float = next(byteIt)
    override def toString: String = "1-byte, two's complement integer"
  }
}
sealed trait DataFormat[T] {
  protected implicit val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
  def length: Int //bytes
  def next(byteIt: ByteIterator): T
  def nextFloat(byteIt: ByteIterator): Float
}

case class ExtTextHeader(
  s: String
) extends SegyPart {
  override def info: String = "ExtTextHeader:\n" + s.grouped(80).mkString("\n")
}

case class TraceHeader(
  traceSequenceNumberWithinLine: Int,
  traceSequenceNumberWithinSegyFile: Int,
  origFieldRecordNumber: Int,
  traceNumberWithinOrigFieldRecord: Int,
  energySourcePointNumber: Int,
  ensembleNumber: Int,
  traceNumberWithinEnsemble: Int,
  traceIdCode: Short,
  vertSummedTracesYieldingThisTrace: Short,
  horizStackedTracesYieldingThisTrace: Short,
  dataUse: Short,
  distFromSourcePointToReceiverGroup: Int,
  receiverGroupElevation: Int,
  surfaceElevationAtSource: Int,
  sourceDepthBelowSurface: Int,
  datumElevationAtReceiverGroup: Int,
  datumElevationAtSource: Int,
  waterDepthAtSource: Int,
  waterDepthAtGroup: Int,
  elevationScalar: Short,
  coordScalar: Short,
  sourceCoordX: Int,
  sourceCoordY: Int,
  groupCoordX: Int,
  groupCoordY: Int,
  coordUnits: Short,
  weatheringVelocity: Short,
  subweatheringVelocity: Short,
  upholeTimeAtSource: Short,
  upholeTimeAtGroup: Short,
  sourceStaticCorrection: Short,
  groupStaticCorrectionMs: Short,
  totalStaticAppliedMs: Short,
  lagTimeAMs: Short,
  lagTimeBMs: Short,
  delayRecordingTimeMs: Short,
  muteTimeStartMs: Short,
  muteTimeEndMs: Short,
  samplesNumber: Short,
  sampleIntervalMs: Short,
  gainType:Short,
  instrumentGainConstant: Short,
  instrumentEarlyOrInitialGain: Short,
  correlated: Short,
  sweepFrequencyAtStart: Short,
  sweepFrequencyAtEnd: Short,
  sweepLengthMs: Short,
  sweepType: Short,
  sweepTraceTaperLengthAtStartMs: Short,
  sweepTraceTaperLengthAtEndMs: Short,
  taperType: Short,
  aliasFilterFreq: Short,
  aliasFilterSlope: Short,
  notchFilterFreq: Short,
  notchFilterSlope: Short,
  lowCutFreq: Short,
  highCutFreq: Short,
  lowCutSlope: Short,
  highCutSlope: Short,
  year: Short,
  dayOfYear: Short,
  hourOfDay: Short,
  minuteOfHour: Short,
  secondOfMinute: Short,
  timeBasisCode: Short,
  traceWeightFactor: Short,
  geophoneGroupNumOfRollSwitchPosOne: Short,
  geophoneGroupNumOfTraceOneWithinOrigRecord: Short,
  geophoneGroupNumOfLastWithinOrigRecord: Short,
  gapSize: Short,
  overTravel: Short,
  x: Int,
  y: Int,
  iLine: Int,
  xLine: Int,
  shotPointNum: Int,
  shotPointNumCoef: Int,
  traceValueMeasUnit: Short,
  transductionConstantMantissa: Int,
  transductionConstantExp: Int,
  transductionUnit: Short,
  deviceOrTraceId: Short,
  timeToMsScalar: Short,
  sourceTypeOrOrientation: Short,
  sourceEnergyDirection0: Int,
  sourceEnergyDirection1: Short,
  sourceMeasMantissa: Int,
  sourceMeasExp: Short,
  sourceMeasUnit: Short
  // 233 - 240 Unassigned in v1
) extends SegyPart {
  override def info: String = s"TraceHeader: #$traceSequenceNumberWithinSegyFile, iLine=$iLine, xLine=$xLine, samples=$samplesNumber at ${sampleIntervalMs}ms"
}

/**
  * Part of the trace data array.
  */
case class TraceDataChunk(
  bs: ByteString,
  pos: Int, // Position of the chunk in the trace
  iLine: Int, // Trace's inline
  xLine: Int // Trace's inline
) (dataFormat: DataFormat[_]) extends SegyPart {
  def length: Int = bs.length

//TODO: Implement with typeclasses
//  def iterator: Iterator[T] = {
//    val byteIt = bs.iterator
//    new Iterator[T] {
//      def hasNext: Boolean = byteIt.hasNext
//      def next(): T = dataFormat.next(byteIt)
//    }
//  }

  /**
    * Casts original data cast to float if required.
    */
  def floatIterator: Iterator[Float] = {
    val byteIt = bs.iterator
    new Iterator[Float] {
      def hasNext: Boolean = byteIt.hasNext
      def next(): Float = dataFormat.nextFloat(byteIt)
    }
  }

  override def info: String = s"TraceDataChunk: iLine=$iLine, xLine=$xLine, pos=$pos, len=${bs.length}"
}
