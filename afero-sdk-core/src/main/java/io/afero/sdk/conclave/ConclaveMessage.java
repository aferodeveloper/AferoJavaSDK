/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class ConclaveMessage {
    private static final int PROTOCOL_VERSION = 2;

    // ----- hello

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hello {
        public HelloFields hello;

        public Hello(String version) {
            hello = new HelloFields();
            hello.version = version;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HelloFields {
        public String version;
        public int heartbeat;
        public int bufferSize;
    }

    // ----- welcome

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Welcome {
        public WelcomeFields welcome;

        public Welcome(int sessionId, long generation, int seq, String accountId) {
            welcome = new WelcomeFields();
            welcome.sessionId = sessionId;
            welcome.generation = generation;
            welcome.seq = seq;
            welcome.accountId = accountId;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WelcomeFields {
        public int sessionId;
        public long generation;
        public int seq;
        public String accountId;
    }

    // ----- login

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Login {
        public LoginFields login;

        public Login(String accountId, String userId, String mobileDeviceId, String token, String type, String clientVersion, boolean trace) {
            login = new LoginFields();
            login.channelId = accountId;
            login.userId = userId;
            login.mobileDeviceId = mobileDeviceId;
            login.accessToken = token;
            login.type = type;
            login.version = clientVersion;
            login.trace = trace;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginFields {
        public String channelId;
        public String userId;
        public String mobileDeviceId;
        public String accessToken;
        public String type;
        public String version;
        public int protocol = PROTOCOL_VERSION;
        public boolean trace;
    }

    // ----- bye

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bye {
        public ByeFields bye;

        public Bye() {
            bye = new ByeFields();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ByeFields {
    }

    // ----- join

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Join {
        public JoinFields join;

        public Join(int sessionId, int timestamp, String type, String userId, String deviceId, String version) {
            join = new JoinFields();
            join.sessionId = sessionId;
            join.timestamp = timestamp;
            join.type = type;
            join.userId = userId;
            join.deviceId = deviceId;
            join.version = version;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JoinFields {
        public int sessionId;
        public int timestamp;
        public String type;
        public String userId;
        public String deviceId;
        public String version;
    }

    // ----- leave

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Leave {
        public LeaveFields leave;

        public Leave(int sessionId, int timestamp, String type, String deviceId) {
            leave = new LeaveFields();
            leave.sessionId = sessionId;
            leave.timestamp = timestamp;
            leave.type = type;
            leave.deviceId = deviceId;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeaveFields {
        public int sessionId;
        public int timestamp;
        public String type;
        public String deviceId;
    }

    // ----- say

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Say {
        public SayFields say;

        public Say(String event, Object data) {
            say = new SayFields();
            say.event = event;
            say.data = data;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SayFields {
        public String event;
        public Object data;
    }

    // ----- whisper

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Whisper {
        public WhisperFields whisper;

        public Whisper(int sessionId, String event, Object data) {
            whisper = new WhisperFields();
            whisper.sessionId = sessionId;
            whisper.event = event;
            whisper.data = data;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhisperFields {
        public int sessionId;
        public String event;
        public Object data;
    }

    // ----- public / private

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Public {
        @JsonProperty("public")
        public MessageFields message;

        public Public(int sessionId, int seq, String event, Object data) {
            message = new MessageFields();
            message.sessionId = sessionId;
            message.seq = seq;
            message.event = event;
            message.data = data;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Private {
        @JsonProperty("private")
        public MessageFields message;

        public Private(int sessionId, int seq, String event, Object data) {
            message = new MessageFields();
            message.sessionId = sessionId;
            message.seq = seq;
            message.event = event;
            message.data = data;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageFields {
        public int sessionId;
        public int seq;
        public String event;
        public Object data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ViewDeviceFields {
        public ViewDeviceFields(String id, boolean v) {
            deviceId = id;
            viewing = v;
        }

        public String deviceId;
        public boolean viewing;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metric{
        public Metric() {
        }

        public enum FailureReason {
            APP_TIMEOUT,
            SERVICE_API_TIMEOUT,
            SERVICE_API_ERROR,
            HUB_ERROR,
        }

        public static class MetricsFields  {

            public MetricsFields () {
            }

            public MetricsFields (String pId, long et, boolean s, String reason) {
                peripheralId = pId;
                elapsed = et;
                success = s;
                failure_reason = reason;
            }

            public String name = "AttributeChangeRTT";
            public String peripheralId;
            public long elapsed;
            public boolean success;
            public String platform = "android";
            public String failure_reason;
        }

        public ArrayList<MetricsFields> application;
        public ArrayList<MetricsFields> peripherals;

        public void addApplicationMetric(MetricsFields  m) {
            if (application == null) {
                application = new ArrayList<>(1);
            }
            application.add(m);
        }

        public void addPeripheralMetric(MetricsFields  m) {
            if (peripherals == null) {
                peripherals = new ArrayList<>(1);
            }
            peripherals.add(m);
        }

        public boolean isEmpty() {
            return (application == null || application.isEmpty()) &&
                (peripherals == null || peripherals.isEmpty());
        }
    }
}
