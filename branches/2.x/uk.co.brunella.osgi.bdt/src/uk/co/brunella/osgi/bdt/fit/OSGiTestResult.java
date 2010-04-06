/*
 * Copyright 2009 brunella ltd
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

import java.io.Serializable;

public class OSGiTestResult implements Serializable {

  private static final long serialVersionUID = -6574779086244575171L;

  public static final int PASS = 1; 
  public static final int FAIL = 2; 
  public static final int ERROR = 3; 
  
  public static OSGiTestResult pass(String className, String methodName, long time) {
    return new OSGiTestResult(PASS, className, methodName, null, "test passed", time);
  }
  
  public static OSGiTestResult fail(String className, String methodName, Throwable throwable, long time) {
    return new OSGiTestResult(FAIL, className, methodName, throwable, throwable.getMessage(), time);
  }
  
  public static OSGiTestResult error(String className, String methodName, Throwable throwable, long time) {
    return new OSGiTestResult(ERROR, className, methodName, throwable, throwable.getMessage(), time);
  }

  private int type;
  private String className;
  private String methodName;
  private Throwable throwable;
  private String message;
  private long time;
  
  private OSGiTestResult(int type, String className, String methodName, Throwable throwable, String message, long time) {
    this.type = type;
    this.className = className;
    this.methodName = methodName;
    this.throwable = throwable;
    this.message = message;
    this.time = time;
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public String getMessage() {
    return message;
  }
  
  public long getTime() {
    return time;
  }
  
  public void setTime(long time) {
    this.time = time;
  }
  
  public boolean hasPassed() {
    return type == PASS;
  }
  
  public boolean hasFailed() {
    return type == FAIL;
  }
  
  public boolean hasErrored() {
    return type == ERROR;
  }
  
  public String toString() {
    String asString;
    if (type == PASS) {
      asString = "PASS: " + methodName + " " + className;
    } else if (type == FAIL) {
      asString = "FAIL: " + methodName + " " + className + "\n\t" + message;
    } else {
      asString = "ERROR: " + methodName + " " + className + 
        "\n\t" + throwable.getClass().getName() + ": " + message;
    }
    asString += "\n\t" + ((double)time / 1000) + "s";
    return asString;
  }
}
