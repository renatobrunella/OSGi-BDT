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
package uk.co.brunella.osgi.bdt.runner.result;

import java.io.Serializable;
import java.lang.reflect.Method;

public class OSGiTestResult implements Serializable {

  private static final long serialVersionUID = -6574779086244575171L;

  public static final int INFO = 0; 
  public static final int PASS = 1; 
  public static final int FAIL = 2; 
  public static final int ERROR = 3; 
  
  public static OSGiTestResult info(String info, String description) {
    return new OSGiTestResult(INFO, null, null, info, description, 0);
  }
  
  public static OSGiTestResult pass(Method method, String description, long time) {
    return new OSGiTestResult(PASS, method, null, "test passed", description, time);
  }
  
  public static OSGiTestResult fail(Method method, Throwable throwable, String description, long time) {
    return new OSGiTestResult(FAIL, method, throwable, throwable.getMessage(), description, time);
  }
  
  public static OSGiTestResult error(Method method, Throwable throwable, String description, long time) {
    return new OSGiTestResult(ERROR, method, throwable, throwable.getMessage(), description, time);
  }

  private int type;
  private String className;
  private String methodName;
  private Throwable throwable;
  private String message;
  private String description;
  private long time;
  
  private OSGiTestResult(int type, Method method, Throwable throwable, String message, String description, long time) {
    this.type = type;
    if (method != null) {
      this.className = method.getDeclaringClass().getName();
      this.methodName = method.getName();
    }
    this.throwable = throwable;
    this.message = message;
    this.description = description;
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
  
  public String getDescription() {
    return description;
  }
  
  public long getTime() {
    return time;
  }
  
  public void setTime(long time) {
    this.time = time;
  }
  
  public boolean isInfo() {
    return type == INFO;
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
    if (type == INFO) {
      asString = "INFO: " + message;
    } else if (type == PASS) {
      asString = "PASS: " + methodName + " " + className;
    } else if (type == FAIL) {
      asString = "FAIL: " + methodName + " " + className + "\n\t" + message;
    } else {
      asString = "ERROR: " + methodName + " " + className + 
        "\n\t" + throwable.getClass().getName() + ": " + message;
    }
    if (description != null) {
      asString += "\n\t" + description;
    }
    asString += "\n\t" + ((double)time / 1000) + "s";
    return asString;
  }
}
