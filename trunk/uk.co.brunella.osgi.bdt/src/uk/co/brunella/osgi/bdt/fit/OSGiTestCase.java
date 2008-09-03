/*
 * Copyright 2008 brunella ltd
 *
 * Licensed under the GPL Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.co.brunella.osgi.bdt.fit;

import java.util.List;

import uk.co.brunella.osgi.bdt.runner.OSGiTestRunner;
import uk.co.brunella.osgi.bdt.runner.result.OSGiTestResult;

import fit.Fixture;
import fit.Parse;

public class OSGiTestCase extends Fixture {

  private OSGiTestRunner testRunner;

  public OSGiTestCase() {
    testRunner = new OSGiTestRunner();
  }

  public void doCells(Parse cells) {
    String action = cells.text().toLowerCase();
    if ("required".equals(action)) {
      addRequired(cells);
    } else if ("system".equals(action)) {
      setSystemBundle(cells);
    } else if ("runner".equals(action)) {
      setRunnerBundle(cells);
    } else if ("parameter".equals(action)) {
      setParameter(cells);
    } else if ("test".equals(action)) {
      addTest(cells);
    } else if ("run".equals(action) || "run tests".equals(action)) {
      runTests(cells);
    } else {
      exception(cells, new RuntimeException("Invalid"));
    }
  }

  private void runTests(Parse cells) {
    if (args.length < 1) {
      exception(cells, new RuntimeException("Repository not set as argument"));
    }
    testRunner.setRepositoryDirectory(args[0]);
    try {
      List<OSGiTestResult> testResults = testRunner.runTests();
      
      addTestResultRows(cells, testResults);

      
    } catch (Exception e) {
      exception(cells, e);
    }
  }

  private void addRequired(Parse cells) {
    if (cells.size() < 2 || cells.size() > 3) {
      exception(cells, new RuntimeException("Invalid number of columns"));
    }
    String bundleSymbolicName = cells.at(1).text();
    String versionRange;
    if (cells.size() == 3) {
      versionRange = cells.at(2).text();
    } else {
      versionRange = "";
    }

    testRunner.addRequiredBundle(bundleSymbolicName, versionRange);
  }

  private void setSystemBundle(Parse cells) {
    if (cells.size() < 2 || cells.size() > 3) {
      exception(cells, new RuntimeException("Invalid number of columns"));
    }
    String bundleSymbolicName = cells.at(1).text();
    String versionRange;
    if (cells.size() == 3) {
      versionRange = cells.at(2).text();
    } else {
      versionRange = "";
    }

    testRunner.setSystemBundle(bundleSymbolicName, versionRange);
  }

  private void setRunnerBundle(Parse cells) {
    if (cells.size() < 2 || cells.size() > 3) {
      exception(cells, new RuntimeException("Invalid number of columns"));
    }
    String bundleSymbolicName = cells.at(1).text();
    String versionRange;
    if (cells.size() == 3) {
      versionRange = cells.at(2).text();
    } else {
      versionRange = "";
    }

    testRunner.setTestRunnerBundle(bundleSymbolicName, versionRange);
  }

  private void setParameter(Parse cells) {
    if (cells.size() != 3) {
      exception(cells, new RuntimeException("Invalid number of columns"));
    }
    String name = cells.at(1).text();
    String value = cells.at(2).text();

    testRunner.addTestParameter(name, value);
  }

  private void addTest(Parse cells) {
    if (cells.size() < 2 || cells.size() > 3) {
      exception(cells, new RuntimeException("Invalid number of columns"));
    }
    String bundleSymbolicName = cells.at(1).text();
    String versionRange;
    if (cells.size() == 3) {
      versionRange = cells.at(2).text();
    } else {
      versionRange = "";
    }

    testRunner.addTestBundle(bundleSymbolicName, versionRange);
  }


  private void addTestResultRows(Parse cells, List<OSGiTestResult> testResults) {
    Parse last = cells.last();
    last.more = buildHeaderRow();
    last = cells.last();
    last.more = buildRows(testResults);
  }

  private Parse buildHeaderRow() {
    Parse root = new Parse(null, null, null, null);
    Parse first = new Parse("td", "Status", null, null);
    Parse next = first.more = new Parse("td", "Test Name", null, null);
    next = next.more = new Parse("td", "Description", null, null);
    next = next.more = new Parse("td", "Message", null, null);
    root.more = new Parse("tr", null, first, null);
    return root.more;
  }
  
  private Parse buildRows(List<OSGiTestResult> testResults) {
    Parse root = new Parse(null, null, null, null);
    Parse next = root;
    for (int i = 0; i < testResults.size(); i++) {
      next = next.more = new Parse("tr", null, buildCells(testResults.get(i)), null);
    }
    return root.more;
  }

  private Parse buildCells(OSGiTestResult testResult) {
    Parse root = new Parse(null, null, null, null);
    Parse next = root;
    if (testResult.isInfo()) {
      next = next.more = new Parse("td", "Test Case", null, null);
      next = next.more = new Parse("td", escape(testResult.getMessage()), null, null);
      next = next.more = new Parse("td", escape(testResult.getDescription()), null, null);
      next = next.more = new Parse("td", "&nbsp;", null, null);
    } else {
      if (testResult.hasPassed()) {
        next = next.more = new Parse("td", "Pass", null, null);
        right(next);
      } else if (testResult.hasFailed()) {
        next = next.more = new Parse("td", "Fail", null, null);
        wrong(next);
      } else if (testResult.hasErrored()) {
        next = next.more = new Parse("td", "Error", null, null);
        exception(next, testResult.getThrowable());
      }
      String testName = testResult.getMethodName(); 
      next = next.more = new Parse("td", testName, null, null);
      if (testResult.getDescription().equals("")) {
        next = next.more = new Parse("td", "&nbsp;", null, null);
      } else {
        next = next.more = new Parse("td", escape(testResult.getDescription()), null, null);
      }
      if (!testResult.hasPassed()) {
        next = next.more = new Parse("td", escape(testResult.getMessage()), null, null);
      } else {
        next = next.more = new Parse("td", "&nbsp;", null, null);
      }
    }
    return root.more;
  }

}
