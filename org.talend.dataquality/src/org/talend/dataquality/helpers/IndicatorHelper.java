// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataquality.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.talend.dataquality.analysis.AnalysisResult;
import org.talend.dataquality.domain.Domain;
import org.talend.dataquality.domain.RangeRestriction;
import org.talend.dataquality.domain.pattern.Pattern;
import org.talend.dataquality.domain.pattern.PatternComponent;
import org.talend.dataquality.domain.pattern.RegularExpression;
import org.talend.dataquality.indicators.BoxIndicator;
import org.talend.dataquality.indicators.CompositeIndicator;
import org.talend.dataquality.indicators.IQRIndicator;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.IndicatorParameters;
import org.talend.dataquality.indicators.IndicatorsFactory;
import org.talend.dataquality.indicators.IndicatorsPackage;
import org.talend.dataquality.indicators.MaxValueIndicator;
import org.talend.dataquality.indicators.MinValueIndicator;
import org.talend.dataquality.indicators.PatternMatchingIndicator;
import org.talend.dataquality.indicators.RangeIndicator;
import org.talend.dataquality.indicators.TextParameters;
import orgomg.cwm.objectmodel.core.Expression;

/**
 * @author scorreia
 * 
 * Helper class for handling indicator attributes.
 */
public final class IndicatorHelper {

    private static Logger log = Logger.getLogger(IndicatorHelper.class);

    private IndicatorHelper() {
    }

    public static void setDataThreshold(Indicator indicator, String min, String max) {
        IndicatorParameters parameters = indicator.getParameters();
        if (parameters == null) {
            parameters = IndicatorsFactory.eINSTANCE.createIndicatorParameters();
            indicator.setParameters(parameters);
        }
        setDataThreshold(parameters, min, max);
    }

    public static void setDataThreshold(IndicatorParameters parameters, String min, String max) {
        assert parameters != null;
        Domain validDomain = parameters.getDataValidDomain();
        if (validDomain == null) {
            validDomain = DomainHelper.createDomain("Data threshold");
            parameters.setDataValidDomain(validDomain);
        }
        // remove previous ranges
        assert validDomain.getRanges().size() < 2;
        validDomain.getRanges().clear();
        RangeRestriction rangeRestriction = DomainHelper.createStringRangeRestriction(min, max);
        validDomain.getRanges().add(rangeRestriction);
    }

    /**
     * Method "getDataThreshold".
     * 
     * @param indicator
     * @return an array with 2 strings representing the data thresholds or null. If the array is not null, its content
     * can be null but its size is always 2.
     */
    public static String[] getDataThreshold(Indicator indicator) {
        IndicatorParameters parameters = indicator.getParameters();
        if (parameters == null) {
            return null;
        }
        return getDataThreshold(parameters);
    }

    public static void setIndicatorThreshold(IndicatorParameters parameters, String min, String max) {
        assert parameters != null;
        Domain validDomain = parameters.getIndicatorValidDomain();
        if (validDomain == null) {
            validDomain = DomainHelper.createDomain("Indicator threshold");
            parameters.setIndicatorValidDomain(validDomain);
        }
        // remove previous ranges
        assert validDomain.getRanges().size() < 2;
        validDomain.getRanges().clear();
        RangeRestriction rangeRestriction = DomainHelper.createStringRangeRestriction(min, max);
        validDomain.getRanges().add(rangeRestriction);
    }

    /**
     * Method "getIndicatorThreshold".
     * 
     * @param indicator
     * @return an array of thresholds if any or null. When the array is not null, its size is 2 but its elements can be
     * null. The first element is the lower threshold and the second element is the higher threshold.
     */
    public static String[] getIndicatorThreshold(Indicator indicator) {
        IndicatorParameters parameters = indicator.getParameters();
        if (parameters == null) {
            return null;
        }
        return getIndicatorThreshold(parameters);
    }

    public static String[] getIndicatorThreshold(IndicatorParameters parameters) {
        Domain validDomain = parameters.getIndicatorValidDomain();
        if (validDomain == null) {
            return null;
        }
        EList<RangeRestriction> ranges = validDomain.getRanges();
        if (ranges.size() != 1) {
            log.warn("Indicator threshold contain too many ranges (or no range): " + ranges.size() + " range(s).");
            return null;
        }
        RangeRestriction rangeRestriction = ranges.get(0);
        if (rangeRestriction == null) {
            return new String[] { null, null };
        }
        return new String[] { DomainHelper.getMinValue(rangeRestriction), DomainHelper.getMaxValue(rangeRestriction) };
    }

    /**
     * Method "getDataThreshold".
     * 
     * @param parameters
     * @return an array with two elements. returns null when no threshold has been found. One element of the array can
     * be null but not both. In this case, it means that there is only one defined threshold, either the upper or the
     * lower threshold.
     */
    public static String[] getDataThreshold(IndicatorParameters parameters) {
        Domain validDomain = parameters.getDataValidDomain();
        if (validDomain == null) {
            return null;
        }
        EList<RangeRestriction> ranges = validDomain.getRanges();
        if (ranges.size() != 1) {
            // log.warn("Data threshold contain too many ranges (or no range): " + ranges.size() + " range(s).");
            return null;
        }
        RangeRestriction rangeRestriction = ranges.get(0);
        if (rangeRestriction == null) {
            return new String[] { null, null };
        }
        return new String[] { DomainHelper.getMinValue(rangeRestriction), DomainHelper.getMaxValue(rangeRestriction) };
    }

    /**
     * Method "getIndicatorLeaves" returns the leaf indicators when the given indicator is a composite indicator or the
     * given indicator.
     * 
     * @param indicator the indicator
     * @return the leaf indicators
     */
    public static List<Indicator> getIndicatorLeaves(Indicator indicator) {
        List<Indicator> leafIndicators = new ArrayList<Indicator>();
        if (indicator instanceof CompositeIndicator) {
            CompositeIndicator compositeIndicator = (CompositeIndicator) indicator;
            for (Indicator ind : compositeIndicator.getChildIndicators()) {
                leafIndicators.addAll(getIndicatorLeaves(ind));
            }
        } else {
            leafIndicators.add(indicator);
        }
        return leafIndicators;
    }

    /**
     * Method "getIndicatorLeaves".
     * 
     * @param result
     * @return all the leaf indicators
     */
    public static List<Indicator> getIndicatorLeaves(AnalysisResult result) {
        List<Indicator> leafIndicators = new ArrayList<Indicator>();
        EList<Indicator> indicators = result.getIndicators();
        for (Indicator indicator : indicators) {
            leafIndicators.addAll(getIndicatorLeaves(indicator));
        }
        return leafIndicators;
    }

    /**
     * Method "getExpectedValue".
     * 
     * @param indicator usually a mode indicator
     * @return the expected value of the indicator
     */
    public static String getExpectedValue(Indicator indicator) {
        return getExpectedValue(indicator.getParameters());
    }

    /**
     * DOC zqin Comment method "getExpectedValue".
     * 
     * @param parameters
     * @return
     */
    public static String getExpectedValue(IndicatorParameters parameters) {
        if (parameters == null) {
            return null;
        }
        Domain indValidDomain = parameters.getIndicatorValidDomain();
        if (indValidDomain == null) {
            return null;
        }
        return DomainHelper.getIndicatorExpectedValue(Collections.singleton(indValidDomain));
    }

    public static void setIndicatorExpectedValue(IndicatorParameters parameters, String value) {
        assert parameters != null;
        Domain validDomain = parameters.getIndicatorValidDomain();
        if (validDomain == null) {
            validDomain = DomainHelper.createIndicatorExpectedValueDomain();
            parameters.setIndicatorValidDomain(validDomain);
        }
        DomainHelper.setIndicatorExpectedValuePattern(Collections.singleton(validDomain), value);
    }

    /**
     * Method "propagateDataThresholdsInChildren" will propage the data threshold to the indicator if the given
     * indicator is a BoxIndicator (Otherwise, nothing is done).
     * 
     * @param indicator an instance of BoxIndicator
     * 
     * 
     */
    public static void propagateDataThresholdsInChildren(Indicator indicator) {
        if (IndicatorsPackage.eINSTANCE.getBoxIndicator().equals(indicator.eClass())) {
            BoxIndicator boxIndicator = (BoxIndicator) indicator;
            String[] dataThreshold = IndicatorHelper.getDataThreshold(boxIndicator);
            if (dataThreshold == null) {
                dataThreshold = new String[2];
            }

            // --- add thresholds in min and max indicators
            RangeIndicator rangeIndicator = boxIndicator.getRangeIndicator();
            setDataThresholds(rangeIndicator, dataThreshold);

            // --- add thresholds in lower and upper quartile indicators
            IQRIndicator iqr = boxIndicator.getIQR();
            setDataThresholds(iqr, dataThreshold);

            // --- add threholds to the mean and median indicator
            setDataThreshold(boxIndicator.getMeanIndicator(), dataThreshold[0], dataThreshold[1]);
            setDataThreshold(boxIndicator.getMedianIndicator(), dataThreshold[0], dataThreshold[1]);
        }

    }

    /**
     * DOC scorreia Comment method "setDataThresholds".
     * 
     * @param rangeIndicator
     * @param dataThreshold
     */
    private static void setDataThresholds(RangeIndicator rangeIndicator, String[] dataThreshold) {
        if (rangeIndicator != null) {
            MinValueIndicator lowerValue = rangeIndicator.getLowerValue();
            if (lowerValue != null) {
                IndicatorHelper.setDataThreshold(lowerValue, dataThreshold[0], dataThreshold[1]);
            }
            MaxValueIndicator upperValue = rangeIndicator.getUpperValue();
            if (upperValue != null) {
                IndicatorHelper.setDataThreshold(upperValue, dataThreshold[0], dataThreshold[1]);
            }
        }
    }

    /**
     * Method "ignoreCaseOption".
     * 
     * @param indicator
     * @return the ignoreCase option or null if it is not set.
     */
    public static Boolean ignoreCaseOption(Indicator indicator) {
        IndicatorParameters parameters = indicator.getParameters();
        return (parameters != null) ? ignoreCaseOption(parameters) : null;
    }

    /**
     * Method "ignoreCaseOption".
     * 
     * @param parameters
     * @return the ignoreCase option or null if it is not set.
     */
    public static Boolean ignoreCaseOption(IndicatorParameters parameters) {
        TextParameters textParameter = parameters.getTextParameter();
        return (textParameter != null) ? textParameter.isIgnoreCase() : null;
    }

    /**
     * Method "getRegexPatternString".
     * 
     * @param indicator
     * @return the regular expression or null if none was found
     */
    public static String getRegexPatternString(PatternMatchingIndicator indicator) {
        IndicatorParameters parameters = indicator.getParameters();
        if (parameters == null) {
            return null;
        }
        Domain dataValidDomain = parameters.getDataValidDomain();
        if (dataValidDomain == null) {
            return null;
        }
        EList<Pattern> patterns = dataValidDomain.getPatterns();
        for (Pattern pattern : patterns) {
            PatternComponent next = pattern.getComponents().iterator().next();
            if (next == null) {
                continue;
            } else {
                RegularExpression regexp = (RegularExpression) next;
                Expression expression = regexp.getExpression();
                if (expression != null) {
                    return expression.getBody();
                }
            }
        }
        return null;
    }
}
