package org.talend.datascience.common.inference.type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class DataTypeInferExecutorTest {

	DataTypeInferExecutor inferExector = new DataTypeInferExecutor();
	private boolean isPrintAllowed = true;

	@Test
	public void testInferDataTypes() {
		// -------1. assert empty dataset ---------
		List<String[]> records = new ArrayList<String[]>();
		String start = TypeInferenceUtilsTest.getCurrentTimeStamp();
		printline("Empty data set infer start at " + start);
		List<ColumnTypeBean> typeResult = inferExector.handle(records);
		String end = TypeInferenceUtilsTest.getCurrentTimeStamp();
		printline("Empty data set infer end at " + end);
		Assert.assertEquals(true, typeResult.isEmpty());
		Assert.assertTrue(TypeInferenceUtilsTest.getTimeDifference(start, end) <= 0.005);

		// -------2. assert dataset with 100 records, 18 columns ---------
		InputStream in = this
				.getClass()
				.getClassLoader()
				.getResourceAsStream(
						"org/talend/datascience/common/inference/type/employee_100.csv");
		BufferedReader inBuffReader = new BufferedReader(new InputStreamReader(
				in));
		String line = null;
		records = new ArrayList<String[]>();
		try {
			while ((line = inBuffReader.readLine()) != null) {
				String[] record = StringUtils
						.splitByWholeSeparatorPreserveAllTokens(line, ";");
				records.add(record);
			}
			start = TypeInferenceUtilsTest.getCurrentTimeStamp();
			printline("100 data set infer start at " + start);
			typeResult = inferExector.handle(records);
			end = TypeInferenceUtilsTest.getCurrentTimeStamp();
			printline("100 data set infer end at " + end);
			printline("100 time difference "
					+ TypeInferenceUtilsTest.getTimeDifference(start, end));
			Assert.assertTrue(TypeInferenceUtilsTest.getTimeDifference(start,
					end) < 0.25);
			// Result
			for (int idx = 0; idx < typeResult.size(); idx++) {
				printline("column " + (idx + 1));
				ColumnTypeBean types = typeResult.get(idx);
				Iterator<String> typeKeys = types.getTypeToCountMap().keySet()
						.iterator();
				while (typeKeys.hasNext()) {
					String key = typeKeys.next();
					Long count = types.getDataTypeCount(key);
					printline("--- " + key + " : " + count);
					if (18 == (idx + 1)) {
						// Assert column 18, empty:10 string:90
						if (TypeInferenceUtils.TYPE_EMPTY.equals(key)) {
							Assert.assertEquals(10, count, 0.00d);
						} else if (TypeInferenceUtils.TYPE_STRING.equals(key)) {
							Assert.assertEquals(90, count, 0l);
						}
					} else if (16 ==(types.getColumnIdx()+1)) {
						// Assert column 16, char:90 integer:10
						if (TypeInferenceUtils.TYPE_CHAR.equals(key)) {
							Assert.assertEquals(90, count, 0.00d);
						} else if (TypeInferenceUtils.TYPE_INTEGER.equals(key)) {
							Assert.assertEquals(10, count, 0l);
						}
					} else if (13 == (idx + 1)) {
						// Assert column 13, double:100
						if (TypeInferenceUtils.TYPE_DOUBLE.equals(key)) {
							Assert.assertEquals(100, count, 0.00d);
						}
					} else if (11 == (idx + 1)) {
						// Assert column 11, date:100
						if (TypeInferenceUtils.TYPE_DATE.equals(key)) {
							Assert.assertEquals(100, count, 0.00d);
						}
					}
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// -------3. assert dataset with 1000 records, 18 columns ---------
		in = this
				.getClass()
				.getClassLoader()
				.getResourceAsStream(
						"org/talend/datascience/common/inference/type/employee_1000.csv");
		inBuffReader = new BufferedReader(new InputStreamReader(in));
		records = new ArrayList<String[]>();
		try {
			while ((line = inBuffReader.readLine()) != null) {
				String[] record = StringUtils
						.splitByWholeSeparatorPreserveAllTokens(line, ";");
				records.add(record);
			}
			start = TypeInferenceUtilsTest.getCurrentTimeStamp();
			printline("1000 data set infer start at " + start);
			typeResult = inferExector.handle(records);
			end = TypeInferenceUtilsTest.getCurrentTimeStamp();
			printline("1000 data set infer end at " + end);
			printline("1000 time difference "
					+ TypeInferenceUtilsTest.getTimeDifference(start, end));
			Assert.assertTrue(TypeInferenceUtilsTest.getTimeDifference(start,
					end) < 0.27);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// @Test don't run the perf test on testing server.
	@Ignore
	public void testInferTypesPerformance() {
		// -------4. assert dataset with 10 000 records, 18 columns ---------
		InputStream in = this
				.getClass()
				.getClassLoader()
				.getResourceAsStream(
						"org/talend/datascience/common/inference/type/employee_10000.csv");
		BufferedReader inBuffReader = new BufferedReader(new InputStreamReader(
				in));
		List<String[]> records = new ArrayList<String[]>();
		String line = null;
		String start, end;
		try {
			while ((line = inBuffReader.readLine()) != null) {
				String[] record = StringUtils
						.splitByWholeSeparatorPreserveAllTokens(line, ";");
				records.add(record);
			}
			start = TypeInferenceUtilsTest.getCurrentTimeStamp();
			printline("10 000 data set infer start at " + start);
			 inferExector.handle(records);
			end = TypeInferenceUtilsTest.getCurrentTimeStamp();
			printline("10 000 data set infer end at " + end);
			double timeDiff = TypeInferenceUtilsTest.getTimeDifference(start,
					end);
			printline("10 000 time difference: " + timeDiff);
			Assert.assertTrue(timeDiff < 0.9);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// -------5. assert dataset with 100 000 records, 18 columns ---------
		in = null;
		try {
			in = new FileInputStream(
					new File(
							"/home/zhao/Talend/product/6.0/TOS_DI-20150426_0751-V5.6.2/workspace/employee_100000.csv"));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		inBuffReader = new BufferedReader(new InputStreamReader(in));
		records = new ArrayList<String[]>();
		try {
			while ((line = inBuffReader.readLine()) != null) {
				String[] record = StringUtils
						.splitByWholeSeparatorPreserveAllTokens(line, ";");
				records.add(record);
			}
			start = TypeInferenceUtilsTest.getCurrentTimeStamp();
			printline("100 000 data set infer start at " + start);
			inferExector.handle(records);
			end = TypeInferenceUtilsTest.getCurrentTimeStamp();
			printline("100 000 data set infer end at " + end);
			double timeDiff = TypeInferenceUtilsTest.getTimeDifference(start,
					end);
			printline("100 000 time difference: " + timeDiff);
			Assert.assertTrue(timeDiff < 4.8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// -------6. assert dataset with 1 000 000 records, 18 columns ---------
		in = null;
		try {
			in = new FileInputStream(
					new File(
							"/home/zhao/Talend/product/6.0/TOS_DI-20150426_0751-V5.6.2/workspace/employee_1000000.csv"));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		inBuffReader = new BufferedReader(new InputStreamReader(in));
		try {
			start = TypeInferenceUtilsTest.getCurrentTimeStamp();
			printline("1 000 000 data set infer start at " + start);
			inferExector.init();
			while ((line = inBuffReader.readLine()) != null) {
				String[] record = StringUtils
						.splitByWholeSeparatorPreserveAllTokens(line, ";");
				inferExector.handle(record);
			}
			end = TypeInferenceUtilsTest.getCurrentTimeStamp();
			printline("1 000 000 data set infer end at " + end);
			double timeDiff = TypeInferenceUtilsTest.getTimeDifference(start,
					end);
			printline("1000 000 time difference: " + timeDiff);
			Assert.assertTrue(timeDiff < 45);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Test the order of column index to see whether it is same after the
	 * inferring type done comparing the before or not.
	 */
	@Test
	public void testInferTypesColumnIndexOrder() {
		// -------6. assert dataset with 1 000 000 records, 18 columns ---------
		InputStream in = this
				.getClass()
				.getClassLoader()
				.getResourceAsStream(
						"org/talend/datascience/common/inference/type/customers_100_bug_TDQ10380.csv");
		BufferedReader inBuffReader = new BufferedReader(new InputStreamReader(
				in));
		try {
			String line = null;
			inferExector.init();
			while ((line = inBuffReader.readLine()) != null) {
				String[] record = StringUtils
						.splitByWholeSeparatorPreserveAllTokens(line, ";");
				inferExector.handle(record);
			}

			List<ColumnTypeBean> typeResult = inferExector.getResults();
			// Result
			for (int idx = 0; idx < typeResult.size(); idx++) {
				printline("column " + (idx + 1));
				ColumnTypeBean types = typeResult.get(idx);
				Iterator<String> typeKeys = types.getTypeToCountMap().keySet()
						.iterator();
				while (typeKeys.hasNext()) {
					String key = typeKeys.next();
					Long count = types.getDataTypeCount(key);
					printline("--- " + key + " : " + count);
					if (8 == (idx + 1)) {
						// Assert column 18, empty:10 string:90
						if (TypeInferenceUtils.TYPE_EMPTY.equals(key)) {
							Assert.assertEquals(4, count, 0.00d);
						} else if (TypeInferenceUtils.TYPE_STRING.equals(key)) {
							Assert.assertEquals(5, count, 0l);
						}else if (TypeInferenceUtils.TYPE_DOUBLE.equals(key)) {
							Assert.assertEquals(1, count, 0l);
						}else if (TypeInferenceUtils.TYPE_INTEGER.equals(key)) {
							Assert.assertEquals(90, count, 0l);
						}
					} 
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printline(String valueToPrint) {
		if (isPrintAllowed) {
			System.out.println(valueToPrint);
		}
	}
}
