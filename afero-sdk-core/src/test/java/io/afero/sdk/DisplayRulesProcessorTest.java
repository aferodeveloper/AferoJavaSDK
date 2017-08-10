/*
 * Copyright (c) 2014-2015 Afero, Inc. All rights reserved.
 */

package io.afero.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.device.ApplyParams;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.device.DisplayRulesProcessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DisplayRulesProcessorTest {

    static class IntValue implements DisplayRulesProcessor.Value<DeviceModel> {

        AttributeValue mValue;

        public void set(Integer i) {
            mValue = new AttributeValue(AttributeValue.DataType.SINT32);
            mValue.setValue(BigDecimal.valueOf(i));
        }

        @Override
        public AttributeValue get(DeviceModel model) {
            return mValue;
        }
    }

    private static DisplayRulesProcessor.Rule[] createRules(DeviceProfile.DisplayRule[] displayRules, DisplayRulesProcessor.Value<DeviceModel> value) {
        DisplayRulesProcessor.Rule[] rules = new DisplayRulesProcessor.Rule[displayRules.length];
        int i = 0;

        for (DeviceProfile.DisplayRule rule : displayRules) {

            ApplyParams apply = ApplyParams.create(rule.apply);

            DisplayRulesProcessor.Rule<DeviceModel> newRule = new DisplayRulesProcessor.Rule<>(value, rule.match, apply);
            rules[i] = newRule;
            ++i;
        }

        return rules;
    }

    @Test
    public void testSimpleInteger() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        InputStream is = getClass().getClassLoader().getResourceAsStream("resources/displayRules.json");
        assertNotNull(is);

        IntValue value = new IntValue();

        ApplyParams result = new ApplyParams();
        result.put("zero", 0);

        DeviceProfile.DisplayRule[] rules = objectMapper.readValue(is, DeviceProfile.DisplayRule[].class);
        DisplayRulesProcessor<DeviceModel> processor = new DisplayRulesProcessor<>(createRules(rules, value));

        value.set(666);
        processor.process(result, null);

        assertEquals("result[*] == true", true, result.get("*"));
        assertEquals("result[one] == null", null, result.get("one"));
        assertEquals("result[two] == null", null, result.get("two"));
        assertEquals("result[three] == null", null, result.get("three"));
        assertEquals("result[hundred] == null", null, result.get("hundred"));
        assertEquals("result[!hundred] == null", true, result.get("!hundred"));
        assertEquals("result[0x100] == null", null, result.get("0x100"));
        assertEquals("result[or] == null", null, result.get("or"));
        assertEquals("result[&0x10000] == true", null, result.get("&0x10000"));
        assertEquals("result[(&0x02)&&(!&0x01)] == true", true, result.get("(&0x02)&&(!&0x01)"));

        result.clear();
        value.set(100);
        processor.process(result, null);

        assertEquals("result[*] == true", true, result.get("*"));
        assertEquals("result[one] == null", null, result.get("one"));
        assertEquals("result[two] == null", null, result.get("two"));
        assertEquals("result[three] == null", null, result.get("three"));
        assertEquals("result[hundred] == 100", 100, result.get("hundred"));
        assertEquals("result[!hundred] == null", null, result.get("!hundred"));
        assertEquals("result[0x100] == null", null, result.get("0x100"));
        assertEquals("result[or] == null", null, result.get("or"));
        assertEquals("result[&0x10000] == true", null, result.get("&0x10000"));
        assertEquals("result[(&0x02)&&(!&0x01)] == true", null, result.get("(&0x02)&&(!&0x01)"));

        result.clear();
        value.set(0x100);
        processor.process(result, null);

        assertEquals("result[*] == true", true, result.get("*"));
        assertEquals("result[one] == null", null, result.get("one"));
        assertEquals("result[two] == null", null, result.get("two"));
        assertEquals("result[three] == null", null, result.get("three"));
        assertEquals("result[hundred] == null", null, result.get("hundred"));
        assertEquals("result[!hundred] == null", true, result.get("!hundred"));
        assertEquals("result[0x100] == 0x100", 0x100, result.get("0x100"));
        assertEquals("result[or] == null", null, result.get("or"));
        assertEquals("result[&0x10000] == true", null, result.get("&0x10000"));
        assertEquals("result[(&0x02)&&(!&0x01)] == true", null, result.get("(&0x02)&&(!&0x01)"));

        result.clear();
        value.set(2);
        processor.process(result, null);

        assertEquals("result[*] == true", true, result.get("*"));
        assertEquals("result[one] == 1", 1, result.get("one"));
        assertEquals("result[two] == 2", 2, result.get("two"));
        assertEquals("result[three] == 3", 3, result.get("three"));
        assertEquals("result[hundred] == 100", null, result.get("hundred"));
        assertEquals("result[!hundred] == null", true, result.get("!hundred"));
        assertEquals("result[0x100] == null", null, result.get("0x100"));
        assertEquals("result[or] == null", null, result.get("or"));
        assertEquals("result[&0x10000] == true", null, result.get("&0x10000"));
        assertEquals("result[(&0x02)&&(!&0x01)] == true", true, result.get("(&0x02)&&(!&0x01)"));

        result.clear();
        value.set(3);
        processor.process(result, null);

        assertEquals("result[*] == true", true, result.get("*"));
        assertEquals("result[one] == 1", 1, result.get("one"));
        assertEquals("result[two] == 2", 2, result.get("two"));
        assertEquals("result[three] == 3", 3, result.get("three"));
        assertEquals("result[hundred] == 100", null, result.get("hundred"));
        assertEquals("result[!hundred] == null", true, result.get("!hundred"));
        assertEquals("result[0x100] == null", null, result.get("0x100"));
        assertEquals("result[or] == null", true, result.get("or"));
        assertEquals("result[&0x10000] == true", null, result.get("&0x10000"));
        assertEquals("result[(&0x02)&&(!&0x01)] == true", null, result.get("(&0x02)&&(!&0x01)"));

        result.clear();
        value.set(8);
        processor.process(result, null);

        assertEquals("result[*] == true", true, result.get("*"));
        assertEquals("result[one] == 1", null, result.get("one"));
        assertEquals("result[two] == 2", null, result.get("two"));
        assertEquals("result[three] == 3", null, result.get("three"));
        assertEquals("result[hundred] == 100", null, result.get("hundred"));
        assertEquals("result[!hundred] == null", true, result.get("!hundred"));
        assertEquals("result[0x100] == null", null, result.get("0x100"));
        assertEquals("result[or] == null", true, result.get("or"));
        assertEquals("result[&0x10000] == true", null, result.get("&0x10000"));
        assertEquals("result[(&0x02)&&(!&0x01)] == true", null, result.get("(&0x02)&&(!&0x01)"));

        result.clear();
        value.set(0x10000);
        processor.process(result, null);

        assertEquals("result[*] == true", true, result.get("*"));
        assertEquals("result[one] == 1", null, result.get("one"));
        assertEquals("result[two] == 2", null, result.get("two"));
        assertEquals("result[three] == 3", null, result.get("three"));
        assertEquals("result[hundred] == 100", null, result.get("hundred"));
        assertEquals("result[!hundred] == null", true, result.get("!hundred"));
        assertEquals("result[0x100] == null", null, result.get("0x100"));
        assertEquals("result[or] == null", null, result.get("or"));
        assertEquals("result[&0x10000] == true", true, result.get("&0x10000"));
        assertEquals("result[(&0x02)&&(!&0x01)] == true", null, result.get("(&0x02)&&(!&0x01)"));
    }

    // This tests a fix where we were merging map values improperly. We weren't creating a new instance of the
    // the map that was copied into the result, so subsequent merges would actually modify the original display
    // rule map instance.
    @Test
    public void testMapValues() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        InputStream is = getClass().getClassLoader().getResourceAsStream("resources/displayRulesWithMaps.json");
        assertNotNull(is);

        IntValue value = new IntValue();
        ApplyParams result = new ApplyParams();
        DeviceProfile.DisplayRule[] rules = objectMapper.readValue(is, DeviceProfile.DisplayRule[].class);
        DisplayRulesProcessor<DeviceModel> processor = new DisplayRulesProcessor<>(createRules(rules, value));

        value.set(0);
        processor.process(result, null);

        assertEquals("result.state.value == one", "one", ((Map)result.get("state")).get("value"));

        result.clear();
        value.set(1);
        processor.process(result, null);

        assertEquals("result.state.value == two", "two", ((Map)result.get("state")).get("value"));

        result.clear();
        value.set(0);
        processor.process(result, null);

        assertEquals("result.state.value == one", "one", ((Map)result.get("state")).get("value"));
    }
}
