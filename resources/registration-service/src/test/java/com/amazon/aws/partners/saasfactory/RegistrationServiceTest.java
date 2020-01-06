/**
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazon.aws.partners.saasfactory;

import org.junit.Test;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class RegistrationServiceTest {

    @Test
    public void generatePasswordTest() {

        Pattern regex = Pattern.compile("(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}");
        assertTrue(regex.matcher("ABCdef123").matches());
        assertFalse(regex.matcher("abcdef123").matches());
        assertFalse(regex.matcher("abcdefxzy").matches());
        assertFalse(regex.matcher("1234567").matches());

        for (int i = 0; i < 500; i++) {
            String password = RegistrationService.generatePassword();
            assertTrue("Invalid password " + password, regex.matcher(password).matches());
        }
    }

    @Test
    public void testRegistrationIsEmpty() {
        Registration r = new Registration();
        assertTrue(r.isEmpty());
        r.setCompany("Company");
        assertFalse(r.isEmpty());
    }
}