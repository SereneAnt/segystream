package com.github.sereneant.segystream

// SEG-Y r2.0 format specification
// https://seg.org/Portals/0/SEG/News%20and%20Resources/Technical%20Standards/seg_y_rev2_0-mar2017.pdf
// Extra redundancy added by intention to look exactly as SEG-Y format spec

sealed trait SegyField
final case class TextField(length: Int) extends SegyField
final case class IntField(from: Int, to: Int, name: Symbol, notes: String) extends SegyField
final case class ReservedField(from: Int, to: Int) extends SegyField

sealed trait Part {
  def fields: List[SegyField]
}

final case class TextHeader2() extends Part {
  override def fields: List[SegyField] = TextField(3200) :: Nil
}

final case class BinHeader2() extends Part {
  override def fields: List[SegyField] =
    IntField(3201, 3204, 'jobId, "Job identification number.") ::
    IntField(3205, 3208, 'lineNumber, "Line number. For 3-D poststack data, this will typically contain the in-line number.") ::
    IntField(3209, 3212, 'reelNumber, "Reel number.") ::
    IntField(3213, 3214, 'dataTracesPerEnsemble, "Number of data traces per ensemble. Mandatory for prestack data.") ::
    IntField(3215, 3216, 'auxTracesPerEnsemble, "Number of auxiliary traces per ensemble. Mandatory for prestack data.") ::
    IntField(3217, 3218, 'sampleInterval, """
      |Sample interval. Microseconds (µs) for time data,
      |Hertz (Hz) for frequency data, meters (m) or feet (ft) for depth data.
      |""".stripMargin) ::
    IntField(3219, 3220, 'sampleIntervalOrig, """
      |Sample interval of original field recording. Microseconds (µs) for time data,
      |Hertz (Hz) for frequency data, meters (m) or feet (ft) for depth data.
      |""".stripMargin) ::
    IntField(3221, 3222, 'samplesPerDataTrace, """
      |Number of samples per data trace. Note: The sample interval
      |and number of samples in the Binary File Header should be
      |for the primary set of seismic data traces in the file.
      |""".stripMargin) ::
    IntField(3223, 3224, 'samplesPerDataTraceOrig, "Number of samples per data trace for original field recording.") ::
    IntField(3225, 3226, 'formatCode, """
      |Data sample format code. Mandatory for all data. These formats are described in Appendix E.
      |1 = 4-byte IBM floating-point
      |2 = 4-byte, two's complement integer
      |3 = 2-byte, two's complement integer
      |4 = 4-byte fixed-point with gain (obsolete)
      |5 = 4-byte IEEE floating-point
      |6 = 8-byte IEEE floating-point
      |7 = 3-byte two’s complement integer
      |8 = 1-byte, two's complement integer
      |9 = 8-byte, two's complement integer
      |10 = 4-byte, unsigned integer
      |11 = 2-byte, unsigned integer
      |12 = 8-byte, unsigned integer
      |15 = 3-byte, unsigned integer
      |16 = 1-byte, unsigned integer
      |""".stripMargin) ::
    IntField(3227, 3228, 'ensembleFold,
      "Ensemble fold — The expected number of data traces per trace ensemble (e.g. the CMP fold).") ::
    IntField(3229, 3230, 'traceSortingCode, """
      |Trace sorting code (i.e. type of ensemble) :
      |–1 = Other (should be explained in a user Extended Textual File Header stanza)
      | 0 = Unknown
      | 1 = As recorded (no sorting)
      | 2 = CDP ensemble
      | 3 = Single fold continuous profile
      | 4 = Horizontally stacked
      | 5 = Common source point
      | 6 = Common receiver point
      | 7 = Common offset point
      | 8 = Common mid-point
      | 9 = Common conversion point
      | """.stripMargin) ::
    IntField(3231, 3232, 'verticalSumCode, """
      |Vertical sum code:
      |1 = no sum,
      |2 = two sum,
      |…,
      |N = M–1 sum (M = 2 to 32,767)
      |""".stripMargin) ::
    IntField(3233, 3234, 'sweepFrequencyAtStart, "Sweep frequency at start (Hz).") ::
    IntField(3235, 3236, 'sweepFrequencyAtEnd, "Sweep frequency at end (Hz).") ::
    IntField(3237, 3238, 'sweepLength, "Sweep length (ms).") ::
    IntField(3239, 3240, 'sweepTypeCode, """
      |Sweep type code:
      |1 = linear
      |2 = parabolic
      |3 = exponential
      |4 = other
      |""".stripMargin) ::
    IntField(3241, 3242, 'traceNumberOfSweepChannel, "Trace number of sweep channel.") ::
    IntField(3243, 3244, 'sweepTraceTaperLengthAtStart,
      "Sweep trace taper length in milliseconds at start if tapered (the taper starts at zero time and is effective for this length.") ::
    IntField(3245, 3246, 'sweepTraceTaperLengthAtEnd,
      "Sweep trace taper length in milliseconds at end (the ending taper starts at sweep length minus the taper length at end).") ::
    IntField(3247, 3248, 'taperType, """
      |Taper type:
      |1 = linear
      |2 = cosine squared
      |3 = other
      |""".stripMargin) ::
    IntField(3249, 3250, 'correlatedDataTraces, """
      |Correlated data traces:
      |1 = no
      |2 = yes
      |""".stripMargin) ::
    IntField(3251, 3252, 'binaryGainRecovered, """
      |Binary gain recovered:
      |1 = yes
      |2 = no
      |""".stripMargin) ::
    IntField(3253, 3254, 'amplitudeRecoveryMethod, """
      |Amplitude recovery method:
      |1 = none
      |2 = spherical divergence
      |3 = AGC
      |4 = other
      |""".stripMargin) ::
    IntField(3255, 3256, 'measurementSystem, """
      |Measurement system: If Location Data stanzas are included in the file, this
      |entry would normally agree with the Location Data stanza. If there is a
      |disagreement, the last Location Data stanza is the controlling authority. If units
      |are mixed, e.g. meters on surface, feet in depth, then a Location Data stanza is
      |mandatory.
      |1 = Meters
      |2 = Feet
      |""".stripMargin) ::
    IntField(3257, 3258, 'impulseSignalPolarity, """
      |Impulse signal polarity
      |1 = Increase in pressure or upward geophone case movement gives negative number on trace.
      |2 = Increase in pressure or upward geophone case movement gives positive number on trace.
      |""".stripMargin) ::
    IntField(3259, 3260, 'vibratoryPolarityCode, """
      |Vibratory polarity code:
      |Seismic signal lags pilot signal by:
      |1 = 337.5° to 22.5°
      |2 = 22.5° to 67.5°
      |3 = 67.5° to 112.5°
      |4 = 112.5° to 157.5°
      |5 = 157.5° to 202.5°
      |6 = 202.5° to 247.5°
      |7 = 247.5° to 292.5°
      |8 = 292.5° to 337.5°
      |""".stripMargin) ::
    IntField(3261, 3264, 'extDataTracesPerEnsemble, """
      |Extended number of data traces per ensemble.
      |If nonzero, this overrides the number of data traces per ensemble in bytes 3213–3214.
      |""".stripMargin) ::
    IntField(3265, 3268, 'extAuxTracesPerEnsemble, """
      |Extended number of auxiliary traces per ensemble.
      |If nonzero, this overrides the number of auxiliary traces per ensemble in bytes 3215–3216.
      |""".stripMargin) ::
    IntField(3269, 3272, 'extSamplesPerDataTrace, """
      |Extended number of samples per data trace.
      |If nonzero, this overrides the number of samples per data trace in bytes 3221–3222.
      |""".stripMargin) ::
    IntField(3273, 3280, 'extSampleInterval, """
      |Extended sample interval, IEEE double precision (64-bit).
      |If nonzero, this overrides the sample interval in bytes 3217–3218 with the same units.
      |""".stripMargin) ::
    IntField(3281, 3288, 'extSampleIntervalOrig, """
      |Extended sample interval of original field recording, IEEE double precision (64-bit).
      |If nonzero, this overrides the sample interval of original field recording in bytes 3219–3220 with the same units.
      |""".stripMargin) ::
    IntField(3289, 3292, 'extSamplesPerDataTraceOrig, """
        |Extended number of samples per data trace in original recording.
        |If nonzero, this overrides the number of samples per data trace in original recording in bytes 3223–3224.
        |""".stripMargin) ::
    IntField(3293, 3296, 'extEnsembleFold,
      "Extended ensemble fold. If nonzero, this overrides ensemble fold in bytes 3227–3228.") ::
    IntField(3297, 3300, 'intConstant, """
      |The integer constant 16909060<10> (01020304<16>). This is used to allow
      |unambiguous detection of the byte ordering to expect for this SEG-Y file. For
      |example, if this field reads as 67305985<10> (04030201<16>) then the bytes in every
      |Binary File Header, Trace Header and Trace Data field must be reversed as
      |they are read, i.e. converting the endian-ness of the fields. If it reads
      |33620995<10> (02010403<16>) then consecutive pairs of bytes need to be swapped
      |in every Binary File Header, Trace Header and Trace Data field.
      |The byte ordering of all other portions (the Extended Textual Header and Data
      |Trailer) of the SEG-Y file is not affected by this field.
      |""".stripMargin) ::
    ReservedField(3301, 3500) ::
    IntField(3501, 3501, 'majorSegyRevision, """
      |Major SEG-Y Format Revision Number. This is an 8-bit unsigned value. Thus
      |for SEG-Y Revision 2.0, as defined in this document, this will be recorded as
      |02<16>. This field is mandatory for all versions of SEG-Y, although a value of
      |zero indicates “traditional” SEG-Y conforming to the 1975 standard.
      |""".stripMargin) ::
    IntField(3502, 3502, 'minorSegyRevision, """
      |Minor SEG-Y Format Revision Number. This is an 8-bit unsigned value with a
      |radix point between the first and second bytes. Thus for SEG-Y Revision 2.0,
      |as defined in this document, this will be recorded as 00<16>. This field is
      |mandatory for all versions of SEG-Y.
      |""".stripMargin) ::
    IntField(3503, 3504, 'fixedLengthTraceFlag, """
      |Fixed length trace flag. A value of one indicates that all traces in this SEG-Y
      |file are guaranteed to have the same sample interval, number of trace header
      |blocks and trace samples, as specified in Binary File Header bytes 3217–3218
      |or 3281–3288, 3517–3518, and 3221–3222 or 3289–3292. A value of zero
      |indicates that the length of the traces in the file may vary and the number of
      |samples in bytes 115–116 of the Standard SEG-Y Trace Header and, if
      |present, bytes 137–140 of SEG-Y Trace Header Extension 1 must be
      |examined to determine the actual length of each trace. This field is mandatory
      |for all versions of SEG-Y, although a value of zero indicates “traditional” SEGY
      |conforming to the 1975 standard. Irrespective of this flag, it is strongly
      |recommended that correct values for the number of samples per trace and
      |sample interval appear in the appropriate trace Trace Header locations.
      |""".stripMargin) ::
    IntField(3505, 3506, 'extTextHeaders, """
      |Number of 3200-byte, Extended Textual File Header records following the
      |Binary Header. If bytes 3521–3528 are nonzero, that field overrides this one. A
      |value of zero indicates there are no Extended Textual File Header records (i.e.
      |this file has no Extended Textual File Header(s)). A value of -1 indicates that
      |there are a variable number of Extended Textual File Header records and the
      |end of the Extended Textual File Header is denoted by an ((SEG: EndText))
      |stanza in the final record (Section 6.2). A positive value indicates that there
      |are exactly that many Extended Textual File Header records.
      |Note that, although the exact number of Extended Textual File Header records
      |may be a useful piece of information, it will not always be known at the time the
      |Binary Header is written and it is not mandatory that a positive value be
      |recorded here or in bytes 3521–3528. It is however recommended to record
      |the number of records if possible as this makes reading more effective and
      |supports direct access to traces on disk files. In the event that this number
      |exceeds 32767, set this field to –1 and bytes 3521–3528 to
      |3600+3200*(number of Extended Textual File Header records). Add a further
      |128 if a SEG-Y Tape Label is present.
      |""".stripMargin) ::
    IntField(3507, 3510, 'maxAdditionalTraceHeaders, """
      |Maximum number of additional 240 byte trace headers. A value of zero
      |indicates there are no additional 240 byte trace headers. The actual number
      |for a given trace may be supplied in bytes 157–158 of SEG-Y Trace Header Extension 1.
      |""".stripMargin) ::
    IntField(3511, 3512, 'timeBasisCode, """
      |Time basis code:
      |1 = Local
      |2 = GMT (Greenwich Mean Time)
      |3 = Other, should be explained in a user defined stanza in the Extended Textual File Header
      |4 = UTC (Coordinated Universal Time)
      |5 = GPS (Global Positioning System Time)
      |""".stripMargin) ::
    IntField(3513, 3520, 'tracesPerFileOrStream, """
      |Number of traces in this file or stream. (64-bit unsigned integer value) If zero,
      |all bytes in the file or stream are part of this SEG-Y dataset.
      |""".stripMargin) ::
    IntField(3521, 3528, 'byteOffsetOfFirstTrace, """
      |Byte offset of first trace relative to start of file or stream if known, otherwise
      |zero. (64-bit unsigned integer value) This byte count will include the initial
      |3600 bytes of the Textual and this Binary File Header plus the Extended
      |Textual Header if present. When nonzero, this field overrides the byte offset
      |implied by any nonnegative number of Extended Textual Header records present in bytes 3505–3506.
      |""".stripMargin) ::
    IntField(3529, 3532, 'trailerRecords, """
      |Number of 3200-byte data trailer stanza records following the last trace (4 byte
      |signed integer). A value of 0 indicates there are no trailer records. A value of -1
      |indicates an undefined number of trailer records (0 or more) following the data.
      |It is, however, recommended to record the number of trailer records if possible
      |as this makes reading more efficient.
      |""".stripMargin) ::
    ReservedField(3533, 3600) ::
    Nil
}
