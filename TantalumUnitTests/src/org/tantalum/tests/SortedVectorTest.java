/*
 Copyright © 2012 Paul Houghton and Futurice on behalf of the Tantalum Project.
 All rights reserved.

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
package org.tantalum.tests;

import org.tantalum.util.SortedVector;

import jmunit.framework.cldc11.AssertionFailedException;
import jmunit.framework.cldc11.TestCase;

/**
 * Unit tests for SortedVector
 *
 * @author phou
 */
public class SortedVectorTest extends TestCase {

    /**
     * Create a new test harness
     */
    public SortedVectorTest() {
        //The first parameter of inherited constructor is the number of test cases
        super(4, "SortedVectorTest");
    }

    /**
     * Invoke unit tests by number
     *
     * @param testNumber
     * @throws Throwable
     */
    public void test(int testNumber) throws Throwable {
        switch (testNumber) {
            case 0:
                testInsertElementAt();
                break;
            case 1:
                testSetElementAt();
                break;
            case 2:
                testAddElement();
                break;
            case 3:
                testSequence();
                break;
            default:
                break;
        }
    }

    /**
     * Test of testInsertElementAt method, of class SortedVector.
     *
     * @throws AssertionFailedException
     */
    public void testInsertElementAt() throws AssertionFailedException {
        System.out.println("insertElementAt");
        SortedVector instance = new SortedVector(new SortedVector.Comparator() {
            public boolean before(Object o1, Object o2) {
                return ((Integer) o1).intValue() < ((Integer) o2).intValue();
            }
        });
        Object o_1 = null;
        int index_1 = 0;
        try {
            instance.insertElementAt(o_1, index_1);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("testInsertElementAt() should throw an exception.");
    }

    /**
     * Test of testSetElementAt method, of class SortedVector.
     *
     * @throws AssertionFailedException
     */
    public void testSetElementAt() throws AssertionFailedException {
        System.out.println("setElementAt");
        SortedVector instance = new SortedVector(new SortedVector.Comparator() {
            public boolean before(Object o1, Object o2) {
                return ((Integer) o1).intValue() < ((Integer) o2).intValue();
            }
        });
        Object o_1 = null;
        int index_1 = 0;
        try {
            instance.setElementAt(o_1, index_1);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("testSetElementAt() should throw an exception.");
    }

    /**
     * Test of testAddElement method, of class SortedVector.
     *
     * @throws AssertionFailedException
     */
    public void testAddElement() throws AssertionFailedException {
        System.out.println("addElement");
        SortedVector instance = new SortedVector(new SortedVector.Comparator() {
            public boolean before(Object o1, Object o2) {
                return ((Integer) o1).intValue() < ((Integer) o2).intValue();
            }
        });
        Object o_1 = new Integer(10);
        Object o_2 = new Integer(1);
        Object o_3 = new Integer(3);
        instance.addElement(o_1);
        instance.addElement(o_2);
        instance.addElement(o_3);
        assertEquals("First", o_2, instance.elementAt(0));
        assertEquals("Second", o_3, instance.elementAt(1));
        assertEquals("Third", o_1, instance.elementAt(2));
    }

    /**
     * Test an error discovered by air-dex to see it does not re-surface
     * 
     * http://projects.developer.nokia.com/Tantalum/ticket/10
     * 
     * @throws AssertionFailedException 
     */
    public void testSequence() throws AssertionFailedException {
        System.out.println("testSequence");
        SortedVector instance = new SortedVector(new SortedVector.Comparator() {
            public boolean before(Object o1, Object o2) {
                return ((Integer) o1).intValue() < ((Integer) o2).intValue();
            }
        });
        Object o_1 = new Integer(10);
        Object o_2 = new Integer(20);
        Object o_3 = new Integer(50);
        Object o_4 = new Integer(40);
        Object o_5 = new Integer(30);
        instance.addElement(o_1);
        instance.addElement(o_2);
        instance.addElement(o_3);
        instance.addElement(o_4);
        instance.addElement(o_5);

        Integer[] expected = {new Integer(10), new Integer(20), new Integer(30), new Integer(40), new Integer(50)};
        for (int i = 0; i < instance.size(); i++) {
            assertEquals("sequence test " + (i+1), expected[i], (Integer) instance.elementAt(i));
        }
    }
}