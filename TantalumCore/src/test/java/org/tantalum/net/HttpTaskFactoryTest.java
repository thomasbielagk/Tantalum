/*
 Copyright (c) 2012-2013 Nokia Corporation. All rights reserved.
 Nokia and Nokia Connecting People are registered trademarks of Nokia Corporation.
 Oracle and Java are trademarks or registered trademarks of Oracle and/or its
 affiliates. Other product and company names mentioned herein may be trademarks
 or trade names of their respective owners.

 Tantalum software shall be used to make the world a better place for everyone.

 This software is licensed for use under the Apache 2 open source software license,
 http://www.apache.org/licenses/LICENSE-2.0.html

 You are kindly requested to return your improvements to this library to the
 open source community at http://projects.developer.nokia.com/Tantalum

 The above copyright and license notice notice shall be included in all copies
 or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package org.tantalum.net;

import org.junit.Before;
import org.junit.Test;
import org.tantalum.MockedStaticInitializers;

import static org.junit.Assert.assertFalse;

/**
 * Unit tests for <code>StaticWebCache.HttpTaskFactory</code>
 * 
 * @author Kai Inkinen <kai.inkinen@futurice.com>; github.com/kaiinkinen
 */
public class HttpTaskFactoryTest extends MockedStaticInitializers {

//    StaticWebCache.HttpTaskFactory taskFactory;
//
//    @Before
//    public void httpTaskFactoryTest() {
//        taskFactory = new StaticWebCache.HttpTaskFactory();
//    }
//
//    @Test
//    public void badRequestResponseCodeIsConsideredInvalid() {
//        final int responseCode = HttpGetter.HTTP_400_BAD_REQUEST;
//        final boolean valid = taskFactory.validateHttpResponse(null);
//
//        assertFalse("Bad Request response (" + responseCode + ") is erroneously marked as valid", valid);
//    }
//
//    @Test
//    public void badRequest417ResponseCodeIsConsideredInvalid() {
//        final int responseCode = HttpGetter.HTTP_417_EXPECTATION_FAILED;
//        final boolean valid = taskFactory.validateHttpResponse(responseCode, null);
//
//        assertFalse("Bad Request response (" + responseCode + ") is erroneously marked as valid", valid);
//    }
//
//    @Test
//    public void serverErrorResponseCodeMarkedAsValid() {
//        final int responseCode = HttpGetter.HTTP_500_INTERNAL_SERVER_ERROR;
//        final boolean valid = taskFactory.validateHttpResponse(responseCode, null, null);
//
//        assertFalse("Bad Request response (" + responseCode + ") is erroneously marked as valid", valid);
//    }
}
