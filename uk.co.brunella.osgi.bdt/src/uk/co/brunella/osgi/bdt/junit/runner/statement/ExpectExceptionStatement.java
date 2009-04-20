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
package uk.co.brunella.osgi.bdt.junit.runner.statement;

public class ExpectExceptionStatement extends Statement {

  private Statement fNext;
  private final Class<? extends Throwable> fExpected;

  public ExpectExceptionStatement(Statement next, Class<? extends Throwable> expected) {
    fNext = next;
    fExpected = expected;
  }

  @Override
  public void evaluate() throws Exception {
    boolean complete = false;
    try {
      fNext.evaluate();
      complete = true;
    } catch (Throwable e) {
      if (!fExpected.isAssignableFrom(e.getClass())) {
        String message = "Unexpected exception, expected<"
            + fExpected.getName() + "> but was<" + e.getClass().getName() + ">";
        throw new Exception(message, e);
      }
    }
    if (complete)
      throw new AssertionError("Expected exception: " + fExpected.getName());
  }
}