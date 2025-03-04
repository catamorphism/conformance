package org.unicode.conformance;

import com.google.gson.reflect.TypeToken;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.number.NumberFormatter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.unicode.conformance.testtype.ITestType;
import org.unicode.conformance.testtype.ITestTypeOutputJson;
import org.unicode.conformance.testtype.collator.CollatorTester;
import org.unicode.conformance.testtype.langnames.LangNamesTester;
import org.unicode.conformance.testtype.likelysubtags.LikelySubtagsTester;
import org.unicode.conformance.testtype.messageformat2.MessageFormatTester;
import org.unicode.conformance.testtype.numberformatter.NumberFormatterTester;

/**
 * Hello world!
 *
 */
public class Icu4jExecutor {

    public static final String PLATFORM = "ICU4J";
    public static final String PLATFORM_VERSION = "74.2";
    public static final String ICU_VERSION = "74";

    public static final String CLDR_VERSION = "44";

    /**
     * Entry point for the executor.
     *
     * Run on an infinite loop until the input "#EXIT" is received.
     */
    public static void main(String[] args) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(System.in);
             BufferedReader br = new BufferedReader(isr)) {
            while (true) {
                computeAndHandleResponse(br);
            }
        } catch (IOException ioe) {
            String javaSetupErrorMsg = ioe.getMessage();
            String executorErrorMsg = "! " + javaSetupErrorMsg;
            ExecutorUtils.printResponseString(executorErrorMsg);

            // exit with non-zero return code
            throw ioe;
        }
    }

    public static void computeAndHandleResponse(BufferedReader br) {
        try {
            String line = br.readLine();
            String response = computeResponseString(line);
            handleResponseString(response);
        } catch (Exception e) {
            // At this level, we assume the IOException is coming from BufferedReader.
            // Any test case execution errors should be handled higher in the call stack (deeper in
            // the code)
            String javaErrorMsg = e.getMessage();
            String executorErrorMsg = "! " + javaErrorMsg;
            ExecutorUtils.printResponseString(executorErrorMsg);
        }
    }

    /**
     * Returns the string to be sent back to the testdriver caller, with the following cases:
     *
     * <ul>
     *   <li>For a test case input that was executed, return the JSON string of the result</li>
     *   <li>For empty input lines, return the empty string</li>
     *   <li>For end-of-input when <pre>#EXIT</pre> is sent in as input, return null</li>
     *   <li>For errors during test execution, return the error output string prefixed with
     *   <pre>#</pre></li>
     * </ul>
     */
    public static String computeResponseString(String inputLine) throws Exception {
        if (inputLine.equals("#EXIT")) {
            return null;
        } else if (inputLine.trim().equals("")) {
            return "";
        } else if (inputLine.equals("#VERSION")) {
            return getVersionResponse();
        } else {
            return getTestCaseResponse(inputLine);
        }
    }

    public static String getVersionResponse() {
        Map<String,String> versionMap = new HashMap<>();
        versionMap.put("platform", PLATFORM);
        versionMap.put("cldrVersion", CLDR_VERSION);
        versionMap.put("icuVersion", ICU_VERSION);
        versionMap.put("platformVersion", PLATFORM_VERSION);

        String result = ExecutorUtils.GSON.toJson(versionMap);
        return result;
    }

    public static String getTestCaseResponse(String inputLine) throws Exception {

        io.lacuna.bifurcan.Map<String,Object> parsedInputPersistentMap =
            ExecutorUtils.parseInputLine(inputLine);

        Optional<Object> testTypeOpt = parsedInputPersistentMap.get("test_type");

        if (!testTypeOpt.isPresent()) {
            io.lacuna.bifurcan.IMap<String,Object> response =
                parsedInputPersistentMap
                    .put("error", "Error in input")
                    .put("error_message", "Error in input found in executor before execution: test_type not present.");

            return ExecutorUtils.formatAsJson(response);
        } else {
            String testTypeStr = (String) testTypeOpt.get();
            ITestType testType;
            if (testTypeStr.equals("collation_short")) {
                testType = CollatorTester.INSTANCE;
            } else if (testTypeStr.equals("lang_names")) {
                testType = LangNamesTester.INSTANCE;
            } else if (testTypeStr.equals("likely_subtags")) {
                testType = LikelySubtagsTester.INSTANCE;
            } else if (testTypeStr.equals("number_fmt")) {
                testType = NumberFormatterTester.INSTANCE;
            } else if (testTypeStr.equals("message_fmt2")) {
                testType = MessageFormatTester.INSTANCE;
            } else {
                io.lacuna.bifurcan.IMap<String,Object> response =
                    parsedInputPersistentMap
                        .put("error", "Error in input")
                        .put("error_message", "Error in input found in executor before execution: test_type not recognized");

                return ExecutorUtils.formatAsJson(response);
            }

            try {
                return testType.getFinalOutputFromInput(parsedInputPersistentMap);
            } catch (Exception e) {
                ITestTypeOutputJson defaultOutput = testType.getDefaultOutputJson();
                return ExecutorUtils.formatAsJson(
                    testType.convertOutputToMap(defaultOutput)
                        .put("label", parsedInputPersistentMap.get("label", null))
                        .put("error", "Error in input" + e.getMessage())
                        .put("error_message", "Error in handling test case: " + e.getMessage())
                );
            }
        }
    }

    /**
     * Perform behavior according to the executor spec at <code>REPO/executors/README.md</code>
     * based on the output and associated semantics of <code>computeResponseString()</code>.
     * @param responseString
     */
    public static void handleResponseString(String responseString) {
        if (responseString == null) {
            System.exit(0);
        }
        if (responseString.equals("")) {
            return; // empty input line, do nothing
        }

        // otherwise, response string carries test result
        ExecutorUtils.printResponseString(responseString);
    }

}
