package io.afero.sdk.log;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AfLogTest {

    @After
    public void afterTest() {
        AfLog.init(null);
    }

    @Test
    public void init() throws Exception {
        makeTester()
                .afLogInit()
                .verifyInit()
        ;
    }

    @Test
    public void testTag() throws Exception {
        makeTester()
                .afLogInit()
                .afLogSetTag("TestTag")
                .verifyTag("TestTag")
        ;
    }

    @Test
    public void testFilterLevelCount() throws Exception {
        assertEquals("AfLog.FilterLevel enum count changed; probably need to update tests!", 6, AfLog.FilterLevel.values().length);
    }

    @Test
    public void testFilterLevelVerbose() throws Exception {
        makeTester()
                .afLogInit()
                .afLogSetFilterLevel(AfLog.FilterLevel.VERBOSE)
                .verifyFilterLevel(AfLog.FilterLevel.VERBOSE)

                .afLogVerbose("verbose")
                .verifyLogVerbose("verbose")

                .afLogInfo("info")
                .verifyLogInfo("info")

                .afLogWarning("warning")
                .verifyLogWarning("warning")

                .afLogError("error")
                .verifyLogError("error")
        ;
    }

    @Test
    public void testFilterLevelDebug() throws Exception {
        makeTester()
                .afLogInit()
                .afLogSetFilterLevel(AfLog.FilterLevel.DEBUG)
                .verifyFilterLevel(AfLog.FilterLevel.DEBUG)

                .afLogVerbose("verbose")
                .verifyLogVerboseNull()

                .afLogDebug("debug")
                .verifyLogDebug("debug")

                .afLogInfo("info")
                .verifyLogInfo("info")

                .afLogWarning("warning")
                .verifyLogWarning("warning")

                .afLogError("error")
                .verifyLogError("error")
        ;
    }

    @Test
    public void testFilterLevelInfo() throws Exception {
        makeTester()
                .afLogInit()
                .afLogSetFilterLevel(AfLog.FilterLevel.INFO)
                .verifyFilterLevel(AfLog.FilterLevel.INFO)

                .afLogVerbose("verbose")
                .verifyLogVerboseNull()

                .afLogDebug("debug")
                .verifyLogDebugNull()

                .afLogInfo("info")
                .verifyLogInfo("info")

                .afLogWarning("warning")
                .verifyLogWarning("warning")

                .afLogError("error")
                .verifyLogError("error")
        ;
    }

    @Test
    public void testFilterLevelWarning() throws Exception {
        makeTester()
                .afLogInit()
                .afLogSetFilterLevel(AfLog.FilterLevel.WARNING)
                .verifyFilterLevel(AfLog.FilterLevel.WARNING)

                .afLogVerbose("verbose")
                .verifyLogVerboseNull()

                .afLogDebug("debug")
                .verifyLogDebugNull()

                .afLogInfo("info")
                .verifyLogInfoNull()

                .afLogWarning("warning")
                .verifyLogWarning("warning")

                .afLogError("error")
                .verifyLogError("error")
        ;
    }

    @Test
    public void testFilterLevelError() throws Exception {
        makeTester()
                .afLogInit()
                .afLogSetFilterLevel(AfLog.FilterLevel.ERROR)
                .verifyFilterLevel(AfLog.FilterLevel.ERROR)

                .afLogVerbose("verbose")
                .verifyLogVerboseNull()

                .afLogDebug("debug")
                .verifyLogDebugNull()

                .afLogInfo("info")
                .verifyLogInfoNull()

                .afLogWarning("warning")
                .verifyLogWarningNull()

                .afLogError("error")
                .verifyLogError("error")
        ;
    }

    @Test
    public void testFilterLevelSilent() throws Exception {
        makeTester()
                .afLogInit()
                .afLogSetFilterLevel(AfLog.FilterLevel.SILENT)
                .verifyFilterLevel(AfLog.FilterLevel.SILENT)

                .afLogVerbose("verbose")
                .verifyLogVerboseNull()

                .afLogDebug("debug")
                .verifyLogDebugNull()

                .afLogInfo("info")
                .verifyLogInfoNull()

                .afLogWarning("warning")
                .verifyLogWarningNull()

                .afLogError("error")
                .verifyLogErrorNull()
        ;
    }

    @Test
    public void setUserEmail() throws Exception {
        makeTester()
                .afLogInit()
                .afLogSetUserEmail("foo@example.com")
                .verifyUserEmail("foo@example.com")
        ;
    }

    @Test
    public void setUserId() throws Exception {
        makeTester()
                .afLogInit()
                .afLogSetUserId("user_id")
                .verifyUserId("user_id")
        ;
    }

    @Test
    public void setString() throws Exception {
        makeTester()
                .afLogInit()
                .afLogSetString("key", "value")
                .verifyString("key", "value")
        ;
    }

    @Test
    public void v() throws Exception {
        makeTester()
                .afLogInit()
                .afLogDebug("verbose")
                .verifyLogDebug("verbose")
        ;
    }

    @Test
    public void d() throws Exception {
        makeTester()
                .afLogInit()
                .afLogDebug("debug")
                .verifyLogDebug("debug")
        ;
    }

    @Test
    public void i() throws Exception {
        makeTester()
                .afLogInit()
                .afLogInfo("info")
                .verifyLogInfo("info")
        ;
    }

    @Test
    public void w() throws Exception {
        makeTester()
                .afLogInit()
                .afLogWarning("warning")
                .verifyLogWarning("warning")
        ;
    }

    @Test
    public void e() throws Exception {
        makeTester()
                .afLogInit()
                .afLogError("error")
                .verifyLogError("error")
        ;
    }

    @Test
    public void e_exception() throws Exception {
        Exception e = new Exception("test");
        makeTester()
                .afLogInit()
                .afLogException(e)
                .verifyLogException(e)
        ;
    }

    @Test
    public void content_type() throws Exception {
        makeTester()
                .afLogInit()
                .afLogContent("type")
                .verifyContent("type")
        ;
    }

    @Test
    public void content_type_id() throws Exception {
        makeTester()
                .afLogInit()
                .afLogContent("type", "id")
                .verifyContent("type", "id")
        ;
    }

    @Test
    public void content_type_id_name() throws Exception {
        makeTester()
                .afLogInit()
                .afLogContent("type", "id", "name")
                .verifyContent("type", "id", "name")
        ;
    }

    private Tester makeTester() {
        return new Tester();
    }

    private class Tester {

        final TestLogger testLogger = new TestLogger();

        Tester() {
        }

        Tester afLogInit() {
            AfLog.init(testLogger);
            return this;
        }

        Tester verifyInit() {
            assertEquals(testLogger, AfLog.getImpl());
            return this;
        }

        Tester afLogSetTag(String tag) {
            AfLog.setTag(tag);
            return this;
        }

        Tester verifyTag(String tag) {
            assertEquals(AfLog.getTag(), tag);
            return this;
        }

        Tester afLogSetFilterLevel(AfLog.FilterLevel level) {
            AfLog.setFilterLevel(level);
            return this;
        }

        Tester verifyFilterLevel(AfLog.FilterLevel info) {
            assertEquals(AfLog.getFilterLevel(), info);
            return this;
        }

        Tester afLogSetUserEmail(String s) {
            AfLog.setUserEmail(s);
            return this;
        }

        Tester verifyUserEmail(String s) {
            assertEquals(testLogger.userEmail, s);
            return this;
        }

        Tester afLogSetUserId(String id) {
            AfLog.setUserId(id);
            return this;
        }

        Tester verifyUserId(String id) {
            assertEquals(testLogger.userId, id);
            return this;
        }

        Tester afLogSetString(String key, String value) {
            AfLog.setString(key, value);
            return this;
        }

        Tester verifyString(String key, String value) {
            assertEquals(testLogger.stringKey, key);
            assertEquals(testLogger.stringValue, value);
            return this;
        }

        Tester afLogVerbose(String s) {
            AfLog.v(s);
            return this;
        }

        Tester afLogDebug(String s) {
            AfLog.d(s);
            return this;
        }

        Tester afLogInfo(String s) {
            AfLog.i(s);
            return this;
        }

        Tester afLogWarning(String s) {
            AfLog.w(s);
            return this;
        }

        Tester afLogError(String s) {
            AfLog.e(s);
            return this;
        }

        Tester afLogException(Throwable t) {
            AfLog.e(t);
            return this;
        }

        Tester verifyLogVerbose(String s) {
            assertEquals(testLogger.logVerbose, s);
            return this;
        }

        Tester verifyLogDebug(String s) {
            assertEquals(testLogger.logDebug, s);
            return this;
        }

        Tester verifyLogInfo(String s) {
            assertEquals(testLogger.logInfo, s);
            return this;
        }

        Tester verifyLogWarning(String s) {
            assertEquals(testLogger.logWarning, s);
            return this;
        }

        Tester verifyLogError(String s) {
            assertEquals(testLogger.logError, s);
            return this;
        }

        Tester verifyLogException(Throwable t) {
            assertEquals(testLogger.logException, t);
            return this;
        }

        Tester afLogContent(String type) {
            AfLog.content(type);
            return this;
        }

        Tester afLogContent(String type, String id) {
            AfLog.content(type, id);
            return this;
        }

        Tester afLogContent(String type, String id, String name) {
            AfLog.content(type, id, name);
            return this;
        }

        Tester verifyContent(String type) {
            assertEquals(testLogger.contentType, type);
            return this;
        }

        Tester verifyContent(String type, String id) {
            assertEquals(testLogger.contentType, type);
            assertEquals(testLogger.contentId, id);
            return this;
        }

        Tester verifyContent(String type, String id, String name) {
            assertEquals(testLogger.contentType, type);
            assertEquals(testLogger.contentId, id);
            assertEquals(testLogger.contentName, name);
            return this;
        }

        Tester verifyLogVerboseNull() {
            assertNull(testLogger.logVerbose);
            return this;
        }

        Tester verifyLogDebugNull() {
            assertNull(testLogger.logDebug);
            return this;
        }

        Tester verifyLogInfoNull() {
            assertNull(testLogger.logInfo);
            return this;
        }

        Tester verifyLogWarningNull() {
            assertNull(testLogger.logWarning);
            return this;
        }

        Tester verifyLogErrorNull() {
            assertNull(testLogger.logError);
            return this;
        }
    }

    private class TestLogger implements AfLog.Impl {
        private String userEmail;
        private String userId;
        private String stringKey;
        private String stringValue;
        private String logVerbose;
        private String logDebug;
        private String logInfo;
        private String logWarning;
        private String logError;
        private Throwable logException;
        private String contentType;
        private String contentId;
        private String contentName;

        @Override
        public void setUserEmail(String email) {
            userEmail = email;
        }

        @Override
        public void setUserId(String id) {
            userId = id;
        }

        @Override
        public void setString(String key, String value) {
            stringKey = key;
            stringValue = value;
        }

        @Override
        public void v(String s) {
            logVerbose = s;
        }

        @Override
        public void d(String s) {
            logDebug = s;
        }

        @Override
        public void i(String s) {
            logInfo = s;
        }

        @Override
        public void w(String s) {
            logWarning = s;
        }

        @Override
        public void e(String s) {
            logError = s;
        }

        @Override
        public void e(Throwable t) {
            logException = t;
        }

        @Override
        public void content(String type) {
            contentType = type;
        }

        @Override
        public void content(String type, String id) {
            contentType = type;
            contentId = id;
        }

        @Override
        public void content(String type, String id, String name) {
            contentType = type;
            contentId = id;
            contentName = name;
        }
    }
}