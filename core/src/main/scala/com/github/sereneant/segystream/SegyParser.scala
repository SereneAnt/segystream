package com.github.sereneant.segystream

import java.nio.ByteOrder

import akka.util.ByteString

sealed trait PromiseStrategy
case object KEEP extends PromiseStrategy
case object COMPLETE extends PromiseStrategy
case object NOOP extends PromiseStrategy

sealed trait SegyPhase {
  def length: Int
  def matPromise: PromiseStrategy
  def extract(bs: ByteString): (SegyPart, SegyPhase) // (segy, nextPhase)
}

case class TextHeaderPhase(cfg: SegyConfig) extends SegyPhase {
  val length = 3200
  override def matPromise: PromiseStrategy = KEEP
  override def extract(bs: ByteString): (TextHeader, SegyPhase) = {
    val s = bs.decodeString(cfg.charset)
    TextHeader(s) -> BinHeaderPhase(cfg)
  }
}

case class BinHeaderPhase(cfg: SegyConfig) extends SegyPhase {
  val length = 400
  override def matPromise: PromiseStrategy = KEEP
  def nextPhase(binHeader: BinHeader): SegyPhase = binHeader.extendedTextHeaders match {
    case extHeaders if extHeaders < 0 =>
      //TODO: Add support for variable number of ext text headers
      throw new UnsupportedOperationException("Variable number of Extended Text Headers not supported yet!")
    case extHeaders if extHeaders == 0 => TraceHeaderPhase(cfg, binHeader) //skip extended text header
    case extHeadersLeft => ExtTextHeaderPhase(cfg, extHeadersLeft, binHeader)
  }
  override def extract(bs: ByteString): (BinHeader, SegyPhase) = {
    var it = bs.iterator // Var!!!
    implicit val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
    val segy = BinHeader(
      jobId = it.getInt,
      lineNumber = it.getInt,
      reelNumber = it.getInt,
      dataTracesPerEnsemble = it.getShort,
      auxTracesPerEnsemble = it.getShort,
      sampleIntervalMicroSec = it.getShort,
      sampleIntervalMicroSecOrig = it.getShort,
      samplesPerDataTrace = it.getShort,
      samplesPerDataTraceOrig = it.getShort,
      dataSampleFormatCode = it.getShort,
      ensembleFold = it.getShort,
      traceSortingCode = it.getShort,
      verticalSumCode = it.getShort,
      sweepFrequencyAtStartHz = it.getShort,
      sweepFrequencyAtEndHz = it.getShort,
      sweepLengthMs = it.getShort,
      sweepTypeCode = it.getShort,
      traceNumberOfSweepChannel = it.getShort,
      sweepTraceTaperLengthAtStartMs = it.getShort,
      sweepTraceTaperLengthAtEndMs = it.getShort,
      taperType = it.getShort,
      correlatedDataTraces = it.getShort,
      binaryGainRecovered = it.getShort,
      amplitudeRecoveryMethod = it.getShort,
      measurementSystem = it.getShort,
      impulseSignalPolarity = it.getShort,
      vibratoryPolarityCode = it.getShort,
      segyRevision = {
        it = it.drop(3500 - 3261 + 1) // 3261 - 3500 Unassigned
        it.getShort
      },
      fixedLengthTraceFlag = it.getShort,
      extendedTextHeaders = it.getShort
    )
    segy -> nextPhase(segy)
  }
}

case class ExtTextHeaderPhase(cfg: SegyConfig, extHeadersLeft: Int, binHeader: BinHeader) extends SegyPhase {
  val length = 3200
  override def matPromise: PromiseStrategy = KEEP
  def nextPhase: SegyPhase = extHeadersLeft match {
    case headers if headers > 0 => ExtTextHeaderPhase(cfg, headers - 1, binHeader)
    case _ => TraceHeaderPhase(cfg, binHeader)
  }
  override def extract(bs: ByteString): (ExtTextHeader, SegyPhase) = {
    val s = bs.decodeString(cfg.charset)
    ExtTextHeader(s) -> nextPhase
  }
}

case class TraceHeaderPhase(cfg: SegyConfig, binHeader: BinHeader) extends SegyPhase {
  val length = 240
  override def matPromise: PromiseStrategy = COMPLETE
  def nextPhase(th: TraceHeader): SegyPhase = {
    val nSamples = binHeader.fixedLengthTraceFlag match {
      case fixed if fixed == 1 => binHeader.samplesPerDataTrace // fixed trace number
      case _ => th.samplesNumber match {
        case sNum if sNum == 0 => binHeader.samplesPerDataTrace
        case sNum if sNum > 0 => sNum
      }
    }
    val bytesLeft = nSamples * binHeader.getDataFormat.length
    TraceDataPhase(cfg, binHeader, th, 0, bytesLeft)
  }
  override def extract(bs: ByteString): (TraceHeader, SegyPhase) = {
    val it = bs.iterator
    implicit val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
    val segy = TraceHeader(
      traceSequenceNumberWithinLine = it.getInt,
      traceSequenceNumberWithinSegyFile = it.getInt,
      origFieldRecordNumber = it.getInt,
      traceNumberWithinOrigFieldRecord = it.getInt,
      energySourcePointNumber = it.getInt,
      ensembleNumber = it.getInt,
      traceNumberWithinEnsemble = it.getInt,
      traceIdCode = it.getShort,
      vertSummedTracesYieldingThisTrace = it.getShort,
      horizStackedTracesYieldingThisTrace = it.getShort,
      dataUse = it.getShort,
      distFromSourcePointToReceiverGroup = it.getInt,
      receiverGroupElevation = it.getInt,
      surfaceElevationAtSource = it.getInt,
      sourceDepthBelowSurface = it.getInt,
      datumElevationAtReceiverGroup = it.getInt,
      datumElevationAtSource = it.getInt,
      waterDepthAtSource = it.getInt,
      waterDepthAtGroup = it.getInt,
      elevationScalar = it.getShort,
      coordScalar = it.getShort,
      sourceCoordX = it.getInt,
      sourceCoordY = it.getInt,
      groupCoordX = it.getInt,
      groupCoordY = it.getInt,
      coordUnits = it.getShort,
      weatheringVelocity = it.getShort,
      subweatheringVelocity = it.getShort,
      upholeTimeAtSource = it.getShort,
      upholeTimeAtGroup = it.getShort,
      sourceStaticCorrection = it.getShort,
      groupStaticCorrectionMs = it.getShort,
      totalStaticAppliedMs = it.getShort,
      lagTimeAMs = it.getShort,
      lagTimeBMs = it.getShort,
      delayRecordingTimeMs = it.getShort,
      muteTimeStartMs = it.getShort,
      muteTimeEndMs = it.getShort,
      samplesNumber = it.getShort,
      sampleIntervalMs = it.getShort,
      gainType = it.getShort,
      instrumentGainConstant = it.getShort,
      instrumentEarlyOrInitialGain = it.getShort,
      correlated = it.getShort,
      sweepFrequencyAtStart = it.getShort,
      sweepFrequencyAtEnd = it.getShort,
      sweepLengthMs = it.getShort,
      sweepType = it.getShort,
      sweepTraceTaperLengthAtStartMs = it.getShort,
      sweepTraceTaperLengthAtEndMs = it.getShort,
      taperType = it.getShort,
      aliasFilterFreq = it.getShort,
      aliasFilterSlope = it.getShort,
      notchFilterFreq = it.getShort,
      notchFilterSlope = it.getShort,
      lowCutFreq = it.getShort,
      highCutFreq = it.getShort,
      lowCutSlope = it.getShort,
      highCutSlope = it.getShort,
      year = it.getShort,
      dayOfYear = it.getShort,
      hourOfDay = it.getShort,
      minuteOfHour = it.getShort,
      secondOfMinute = it.getShort,
      timeBasisCode = it.getShort,
      traceWeightFactor = it.getShort,
      geophoneGroupNumOfRollSwitchPosOne = it.getShort,
      geophoneGroupNumOfTraceOneWithinOrigRecord = it.getShort,
      geophoneGroupNumOfLastWithinOrigRecord = it.getShort,
      gapSize = it.getShort,
      overTravel = it.getShort,
      x = it.getInt,
      y = it.getInt,
      iLine = it.getInt,
      xLine = it.getInt,
      shotPointNum = it.getInt,
      shotPointNumCoef = it.getInt,
      traceValueMeasUnit = it.getShort,
      transductionConstantMantissa = it.getInt,
      transductionConstantExp = it.getInt,
      transductionUnit = it.getShort,
      deviceOrTraceId = it.getShort,
      timeToMsScalar = it.getShort,
      sourceTypeOrOrientation = it.getShort,
      sourceEnergyDirection0 = it.getInt,
      sourceEnergyDirection1 = it.getShort,
      sourceMeasMantissa = it.getInt,
      sourceMeasExp = it.getShort,
      sourceMeasUnit = it.getShort
      // 233 - 240 Unassigned in v1
    )
    segy -> nextPhase(segy)
  }
}

/**
  * @param pos index of starting sample of the DataChunk in the Trace
  * @param bytesLeft bytes left to fetch for the current Trace
  */
case class TraceDataPhase(cfg: SegyConfig, binHeader: BinHeader, th: TraceHeader, pos: Int, bytesLeft: Int)
  extends SegyPhase
{
  override def length: Int = Math.min(cfg.dataChunkSize, bytesLeft)
  override def matPromise: PromiseStrategy = NOOP
  def nextPhase(td: TraceDataChunk): SegyPhase = {
    val bytesUsed = td.bs.length
    val curPos = pos + bytesUsed / binHeader.getDataFormat.length
    bytesLeft - bytesUsed match {
      case left if left > 0 => TraceDataPhase(cfg, binHeader, th, curPos, left)
      case left if left == 0 => TraceHeaderPhase(cfg, binHeader)
      case left if left < 0 => throw new SegyException(s"Something went wrong, negative offset reading SegY Data: $left")
    }
  }
  override def extract(bs: ByteString): (TraceDataChunk, SegyPhase) = {
    val segy = TraceDataChunk(bs, pos, th.iLine, th.xLine)(binHeader.getDataFormat)
    segy -> nextPhase(segy)
  }
}

object SegyHeaders {
  def of(segyParts: Seq[SegyPart]): SegyHeaders = segyParts match {
    case Seq(th: TextHeader, bh: BinHeader) =>
      SegyHeaders(th, bh, Vector.empty)
    case Seq(th: TextHeader, bh: BinHeader, ext @ _*) =>
      SegyHeaders(th, bh, ext.map(_.asInstanceOf[ExtTextHeader]).toVector)
    case _ => throw new SegyException(s"Wrong SegY headers: $segyParts")
  }
}
case class SegyHeaders(
  textHeader: TextHeader,
  binHeader: BinHeader,
  extTextHeaders: Vector[ExtTextHeader]
)
