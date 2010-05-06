/*
 * Copyright (c) 2010.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.utils.tools;

import org.broadinstitute.sting.commandline.Argument;
import org.broadinstitute.sting.commandline.CommandLineProgram;
import org.broadinstitute.sting.utils.genotype.vcf.VCFWriter;
import org.broadinstitute.sting.utils.genotype.vcf.VCFReader;
import org.broadinstitute.sting.utils.fasta.IndexedFastaSequenceFile;
import org.broadinstitute.sting.utils.GenomeLocParser;
import org.broadinstitute.sting.utils.GenomeLoc;
import org.broad.tribble.vcf.VCFRecord;
import org.broad.tribble.vcf.VCFHeader;

import java.util.TreeSet;
import java.util.HashMap;
import java.io.File;
import java.io.FileNotFoundException;

import net.sf.picard.liftover.LiftOver;
import net.sf.picard.util.Interval;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;

/**
 * Lifts a VCF file over from one build to another.
 */
public class LiftoverVCF extends CommandLineProgram {

    public static void main(String args[]) {
        LiftoverVCF LO = new LiftoverVCF();
        CommandLineProgram.start( LO, args );
        System.exit(0);
    }

    @Argument(fullName="vcf", shortName="vcf", doc="VCF file to lift over", required=true)
    protected File VCF = null;

    @Argument(fullName="out", shortName="out", doc="Output VCF file", required=true)
    protected File OUT = null;

    @Argument(fullName="chain", shortName="chain", doc="Chain file", required=true)
    protected File CHAIN = null;

    @Argument(fullName="newSequenceDictionary", shortName="dict", doc="Sequence .dict file for the new build", required=true)
    protected File NEW_SEQ_DICT = null;

    @Argument(fullName="oldReferenceFasta", shortName="fasta", doc="Sequence .fasta file for the old build", required=true)
    protected File OLD_REF_FASTA = null;

    @Override
    protected int execute() {

        try {
            IndexedFastaSequenceFile seq = new IndexedFastaSequenceFile(OLD_REF_FASTA);
            GenomeLocParser.setupRefContigOrdering(seq);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to load the old sequence dictionary");
        }

        VCFReader reader = new VCFReader(VCF);
        VCFHeader header = reader.getHeader();

        LiftOver liftOver = new LiftOver(CHAIN);
        liftOver.setLiftOverMinMatch(LiftOver.DEFAULT_LIFTOVER_MINMATCH);

        final SAMFileHeader toHeader = new SAMFileReader(NEW_SEQ_DICT).getFileHeader();
        liftOver.validateToSequences(toHeader.getSequenceDictionary());

        TreeSet<GenomeLoc> sortedLocs = new TreeSet<GenomeLoc>();
        HashMap<GenomeLoc, VCFRecord> locsToRecords = new HashMap<GenomeLoc, VCFRecord>();
        long successfulIntervals = 0, failedIntervals = 0;

        while ( reader.hasNext() ) {
            VCFRecord record = reader.next();

            final Interval fromInterval = new Interval(record.getChr(), record.getStart(), record.getEnd());
            final Interval toInterval = liftOver.liftOver(fromInterval);

            if ( toInterval != null ) {
                record.setLocation(toInterval.getSequence(), toInterval.getStart());
                GenomeLoc loc = GenomeLocParser.createGenomeLoc(toInterval.getSequence(), toInterval.getStart());
                sortedLocs.add(loc);
                locsToRecords.put(loc, record);
                successfulIntervals++;
            } else {
                failedIntervals++;
            }
        }

        reader.close();

        System.out.println("Converted " + successfulIntervals + " intervals; failed to convert " + failedIntervals + " intervals.");
        System.out.println("Writing new VCF...");

        VCFWriter writer = new VCFWriter(OUT);
        writer.writeHeader(header);

        for ( GenomeLoc loc : sortedLocs )
            writer.addRecord(locsToRecords.get(loc));

        writer.close();

        return 0;
    }
}