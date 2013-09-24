/*
 * Copyright (C) 2006-2013 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package org.talend.dataquality.matchmerge.mfb;

import junit.framework.TestCase;
import org.talend.dataquality.matchmerge.*;
import org.talend.dataquality.record.linkage.attribute.IAttributeMatcher;
import org.talend.dataquality.record.linkage.constant.AttributeMatcherType;

import java.util.*;

public class MFBTest extends TestCase {

    private final static String[] CONSTANTS = {"constant", "value", "tac", "different", "big", "database", "heat", "quality"};

    private final static String[] SIMILARS = {"constant", "constan", "ocnstant", "constnat", "constnta", "oncstant", "constatn", "consttan"};

    private final static AttributeMatcherType[] TESTS_MATCH = {
            AttributeMatcherType.levenshtein,
            AttributeMatcherType.soundex,
            AttributeMatcherType.jaroWinkler,
            AttributeMatcherType.doubleMetaphone
    };

    public void testArguments() throws Exception {
        MatchMergeAlgorithm algorithm = new MFB(new AttributeMatcherType[0],
                new float[0],
                new MergeAlgorithm[0],
                new double[0],
                new IAttributeMatcher.NullOption[0],
                new SubString[0]);
        List<Record> list = algorithm.execute(Collections.<Record>emptyList().iterator());
        assertEquals(0, list.size());
    }

    public void testEmptyRecords() throws Exception {
        Map<String, ValueGenerator> generators = new HashMap<String, ValueGenerator>();
        Iterator<Record> iterator = new ValuesIterator(100000, generators);
        MatchMergeAlgorithm algorithm = new MFB(new AttributeMatcherType[0],
                new float[0],
                new MergeAlgorithm[0],
                new double[0],
                new IAttributeMatcher.NullOption[0],
                new SubString[0]);
        List<Record> list = algorithm.execute(iterator);
        assertEquals(100000, list.size());
    }

    public void testConstantValueRecords() throws Exception {
        for (AttributeMatcherType matchAlgorithm : TESTS_MATCH) {
            testConstant(1, 100000, matchAlgorithm);
            testConstant(2, 100000, matchAlgorithm);
            testConstant(4, 100000, matchAlgorithm);
            testConstant(8, 100000, matchAlgorithm);
        }
    }

    private static void testConstant(final int constantNumber, int totalCount, AttributeMatcherType matchAlgorithm) {
        Map<String, ValueGenerator> generators = new HashMap<String, ValueGenerator>();
        generators.put("name", new ValueGenerator() {
            int index = 0;

            @Override
            public String newValue() {
                return CONSTANTS[index++ % constantNumber];
            }
        });

        Iterator<Record> iterator = new ValuesIterator(totalCount, generators);
        MatchMergeAlgorithm algorithm = new MFB(new AttributeMatcherType[]{matchAlgorithm},
                new float[]{1},
                new MergeAlgorithm[]{MergeAlgorithm.UNIFY},
                new double[]{1},
                new IAttributeMatcher.NullOption[]{IAttributeMatcher.NullOption.nullMatchAll},
                new SubString[]{SubString.NO_SUBSTRING});
        List<Record> mergedRecords = algorithm.execute(iterator);
        assertEquals(constantNumber, mergedRecords.size());
        for (Record mergedRecord : mergedRecords) {
            assertNotNull(mergedRecord.getGroupId());
            assertEquals(totalCount / constantNumber, mergedRecord.getRelatedIds().size());
        }
    }

    public void testMatchWeight() throws Exception {
        for (AttributeMatcherType matchAlgorithm : TESTS_MATCH) {
            testWeight(1, 100000, matchAlgorithm);
            testWeight(2, 100000, matchAlgorithm);
            testWeight(4, 100000, matchAlgorithm);
            testWeight(8, 100000, matchAlgorithm);
        }
    }

    private static void testWeight(final int constantNumber, int totalCount, AttributeMatcherType matchAlgorithm) {
        Map<String, ValueGenerator> generators = new HashMap<String, ValueGenerator>();
        generators.put("name", new ValueGenerator() {
            int index = 0;

            @Override
            public String newValue() {
                return CONSTANTS[index++ % constantNumber];
            }
        });

        Iterator<Record> iterator = new ValuesIterator(totalCount, generators);
        MatchMergeAlgorithm algorithm = new MFB(new AttributeMatcherType[]{matchAlgorithm},
                new float[]{1},
                new MergeAlgorithm[]{MergeAlgorithm.UNIFY},
                new double[]{0}, // Mark rule with no weight (-> match record should have a 0 confidence).
                new IAttributeMatcher.NullOption[] {IAttributeMatcher.NullOption.nullMatchAll},
                new SubString[]{SubString.NO_SUBSTRING});
        List<Record> mergedRecords = algorithm.execute(iterator);
        assertEquals(constantNumber, mergedRecords.size());
        for (Record mergedRecord : mergedRecords) {
            assertNotNull(mergedRecord.getGroupId());
            assertEquals(totalCount / constantNumber, mergedRecord.getRelatedIds().size());
            assertEquals(0.0, mergedRecord.getConfidence());
        }
    }


    public void testSimilarValueRecords() throws Exception {
        testSimilar(1, 100000, AttributeMatcherType.levenshtein);
        testSimilar(2, 100000, AttributeMatcherType.levenshtein);
        testSimilar(4, 100000, AttributeMatcherType.levenshtein);
        testSimilar(8, 100000, AttributeMatcherType.levenshtein);
    }

    private static void testSimilar(final int similarNumber, int totalCount, AttributeMatcherType matchAlgorithm) {
        Map<String, ValueGenerator> generators = new HashMap<String, ValueGenerator>();
        generators.put("name", new ValueGenerator() {
            int index = 0;

            @Override
            public String newValue() {
                return SIMILARS[index++ % similarNumber];
            }
        });

        Iterator<Record> iterator = new ValuesIterator(totalCount, generators);
        MatchMergeAlgorithm algorithm = new MFB(new AttributeMatcherType[]{matchAlgorithm},
                new float[]{0.5f},
                new MergeAlgorithm[]{MergeAlgorithm.UNIFY},
                new double[]{1},
                new IAttributeMatcher.NullOption[]{IAttributeMatcher.NullOption.nullMatchAll},
                new SubString[]{SubString.NO_SUBSTRING});
        List<Record> mergedRecords = algorithm.execute(iterator);
        assertEquals(1, mergedRecords.size());
        for (Record mergedRecord : mergedRecords) {
            assertNotNull(mergedRecord.getGroupId());
            assertEquals(totalCount, mergedRecord.getRelatedIds().size());
        }
    }

    interface ValueGenerator {
        String newValue();
    }

    private static class ValuesIterator implements Iterator<Record> {

        private int currentIndex = 0;

        private final int size;

        private final Map<String, ValueGenerator> generators;

        private ValuesIterator(int size, Map<String, ValueGenerator> generators) {
            this.size = size;
            this.generators = generators;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < size;
        }

        @Override
        public Record next() {
            Vector<Attribute> record = new Vector<Attribute>();
            for (Map.Entry<String, ValueGenerator> generator : generators.entrySet()) {
                record.add(new Attribute(generator.getKey(), generator.getValue().newValue()));
            }
            currentIndex++;
            return new Record(record, String.valueOf(currentIndex - 1));
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not supported");
        }
    }
}
